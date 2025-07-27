import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import model.Message;
import model.Tracks;

public class ChatScreen extends Canvas implements CommandListener {

  private static final int MAX_VISIBLE_MESSAGES = 5;
  private static final int MAX_STORED_MESSAGES = 100;
  private static final int CLEANUP_THRESHOLD = 80;
  private static final int MESSAGE_BUFFER = 3;

  private static final int RECEIVED_BUBBLE_COLOR = 0xFFFFFF;
  private static final int SENT_TEXT_COLOR = 0xFFFFFF;
  private static final int RECEIVED_TEXT_COLOR = 0x000000;
  private static final int BUBBLE_BORDER_COLOR = 0xE0E0E0;

  private static final int BUBBLE_PADDING = 8;
  private static final int BUBBLE_MARGIN = 6;
  private static final int MESSAGE_SPACING = 5;

  private static final int CHAR_PER_LOOP = 4;
  private static final int MESSAGE_DELAY = 50;

  private final SettingsManager settingsManager = SettingsManager.getInstance();

  private Vector messages = new Vector();
  private int currentMessageIndex = 0;
  private int currentCharIndex = 0;
  private Timer timer;
  private boolean isWaitingForResponse = false;
  private final Vector pendingMessages = new Vector();
  private int firstVisibleMessage = 0;
  private int lastVisibleMessage = 0;

  private int scrollOffset = 0;
  private int focusedClickableIndex = -1;
  private int totalContentHeight = 0;

  private int lastPointerY = -1;
  private boolean isDragging = false;
  private int dragStartY = -1;
  private int dragStartScrollOffset = -1;

  private TextBox inputBox;
  private Font font;
  private String sessionId;

  private final Navigator navigator;

  public ChatScreen(String title, Navigator navigator) {
    this.navigator = navigator;
    this.setTitle(title);
    this.addCommands();
    this.setCommandListener(this);
    this.sessionId = generateRandomId(12);
  }

  private void addCommands() {
    this.addCommand(Commands.ok());
    this.addCommand(Commands.back());
    this.addCommand(Commands.input());
  }

  public void showNotify() {
    super.showNotify();

    if (this.messages.isEmpty()) {
      addAIMessage(Lang.tr("chat.welcome_message"));
      startTypingEffect();
    }
  }

  private void addAIMessage(String text) {
    Message msg = new Message(text, false);
    messages.addElement(msg);
    currentMessageIndex = messages.size() - 1;
    currentCharIndex = 0;
    msg.displayText = "";

    if (messages.size() > MAX_STORED_MESSAGES) {
      forceCleanupMessages();
    }

    startTypingEffect();
  }

  private void addUserMessage(String text) {
    Message msg = new Message(text, true);
    messages.addElement(msg);
    currentMessageIndex = messages.size() - 1;
    currentCharIndex = 0;
    msg.displayText = "";

    if (messages.size() > MAX_STORED_MESSAGES) {
      forceCleanupMessages();
    }

    repaint();
  }

  private void addAIResponseMessages(String response) {
    Vector sentences = splitBySentence(response);

    if (messages.size() > 0) {
      messages.removeElementAt(messages.size() - 1);
    }

    pendingMessages.removeAllElements();
    for (int i = 0; i < sentences.size(); i++) {
      String sentence = (String) sentences.elementAt(i);
      if (sentence.trim().length() > 0) {
        pendingMessages.addElement(sentence.trim());
      }
    }

    if (pendingMessages.size() > 0) {
      String firstMessage = (String) pendingMessages.elementAt(0);
      pendingMessages.removeElementAt(0);
      addAIMessage(firstMessage);
    }
  }

  private Vector splitBySentence(String text) {
    Vector sentences = new Vector();
    StringBuffer currentSentence = new StringBuffer();

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      currentSentence.append(c);

      if (c == '.' || c == '!' || c == '?') {
        if (i == text.length() - 1
            || (i < text.length() - 1
                && (text.charAt(i + 1) == ' '
                    || text.charAt(i + 1) == '\n'
                    || text.charAt(i + 1) == '\r'))) {
          String sentence = currentSentence.toString().trim();
          if (sentence.length() > 0) {
            sentences.addElement(sentence);
          }
          currentSentence.setLength(0);
        }
      }
    }

    if (currentSentence.length() > 0) {
      String remaining = currentSentence.toString().trim();
      if (remaining.length() > 0) {
        sentences.addElement(remaining);
      }
    }

    return sentences;
  }

  private String generateRandomId(int length) {
    final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    Random rand = new Random();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < length; i++) {
      int index = Math.abs(rand.nextInt()) % RANDOM_CHARS.length();
      sb.append(RANDOM_CHARS.charAt(index));
    }
    return sb.toString();
  }

  private int getMessageHeight(Message message, int screenWidth, Font font) {
    if (message.height != -1) {
      return message.height;
    }

    String textToDraw =
        (message == messages.elementAt(currentMessageIndex)) ? message.displayText : message.text;

    message.height = calculateMessageHeight(textToDraw, screenWidth, font);
    return message.height;
  }

  private void invalidateMessageHeights() {
    for (int i = 0; i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      message.height = -1;
    }
  }

  private void adjustScrollToBottom() {
    int height = getHeight();
    int width = getWidth();

    calculateVisibleMessageRange();

    totalContentHeight = BUBBLE_MARGIN;
    for (int i = 0; i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      String textToDraw = (i == currentMessageIndex) ? message.displayText : message.text;
      if (textToDraw.length() > 0) {
        totalContentHeight += getMessageHeight(message, width, font);
      }
    }

    totalContentHeight += BUBBLE_MARGIN;

    int maxScroll = Math.max(0, totalContentHeight - height);
    scrollOffset = maxScroll;

    if (scrollOffset < 0) {
      scrollOffset = 0;
    }

    cleanupOldMessages();
  }

  private void calculateVisibleMessageRange() {
    int height = getHeight();
    int width = getWidth();

    firstVisibleMessage = 0;
    lastVisibleMessage = messages.size() - 1;

    if (messages.size() <= MAX_VISIBLE_MESSAGES) {
      return;
    }

    int currentY = BUBBLE_MARGIN;
    int viewportTop = scrollOffset;
    int viewportBottom = scrollOffset + height;

    for (int i = 0; i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      String textToDraw = (i == currentMessageIndex) ? message.displayText : message.text;

      if (textToDraw.length() > 0) {
        int messageHeight = getMessageHeight(message, width, font);

        if (currentY + messageHeight >= viewportTop - MESSAGE_BUFFER * 20) {
          firstVisibleMessage = Math.max(0, i - MESSAGE_BUFFER);
          break;
        }
        currentY += messageHeight;
      }
    }

    currentY = BUBBLE_MARGIN;
    for (int i = 0; i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      String textToDraw = (i == currentMessageIndex) ? message.displayText : message.text;

      if (textToDraw.length() > 0) {
        int messageHeight = getMessageHeight(message, width, font);

        if (currentY > viewportBottom + MESSAGE_BUFFER * 20) {
          lastVisibleMessage = Math.min(messages.size() - 1, i + MESSAGE_BUFFER);
          break;
        }
        currentY += messageHeight;
      }
    }

    if (currentMessageIndex >= 0) {
      firstVisibleMessage = Math.min(firstVisibleMessage, currentMessageIndex);
      lastVisibleMessage = Math.max(lastVisibleMessage, currentMessageIndex);
    }
  }

  private void cleanupOldMessages() {
    if (messages.size() > CLEANUP_THRESHOLD) {
      int messagesToRemove = messages.size() - MAX_VISIBLE_MESSAGES;

      if (messagesToRemove > 0) {
        Vector messagesToKeep = new Vector(MAX_VISIBLE_MESSAGES);
        int startIndex = Math.max(0, messages.size() - MAX_VISIBLE_MESSAGES);

        for (int i = startIndex; i < messages.size(); i++) {
          messagesToKeep.addElement(messages.elementAt(i));
        }

        messages.removeAllElements();
        messages = messagesToKeep;

        currentMessageIndex = Math.max(0, currentMessageIndex - messagesToRemove);
        firstVisibleMessage = 0;
        lastVisibleMessage = messages.size() - 1;

        System.gc();
      }
    }
  }

  private void forceCleanupMessages() {
    if (messages.size() > MAX_STORED_MESSAGES) {
      Vector messagesToKeep = new Vector(MAX_VISIBLE_MESSAGES);
      int startIndex = Math.max(0, messages.size() - MAX_VISIBLE_MESSAGES);

      for (int i = startIndex; i < messages.size(); i++) {
        messagesToKeep.addElement(messages.elementAt(i));
      }

      messages.removeAllElements();
      messages = messagesToKeep;

      currentMessageIndex = Math.min(currentMessageIndex, messages.size() - 1);
      if (currentMessageIndex < 0) {
        currentMessageIndex = 0;
      }

      firstVisibleMessage = 0;
      lastVisibleMessage = messages.size() - 1;

      System.gc();
    }
  }

  private boolean isScrolledToBottom() {
    int maxScroll = Math.max(0, totalContentHeight - getHeight());
    return scrollOffset >= maxScroll - 50;
  }

  private void startTypingEffect() {
    if (timer != null) {
      timer.cancel();
    }
    timer = new Timer();
    timer.schedule(
        new TimerTask() {
          public void run() {
            if (currentMessageIndex < messages.size()) {
              Message currentMessage = (Message) messages.elementAt(currentMessageIndex);
              if (currentCharIndex < currentMessage.text.length()) {
                int nextIndex = currentCharIndex + CHAR_PER_LOOP;
                if (nextIndex > currentMessage.text.length()) {
                  nextIndex = currentMessage.text.length();
                }
                currentCharIndex = nextIndex;

                currentMessage.displayText = currentMessage.text.substring(0, currentCharIndex);
                currentMessage.height = -1;
                adjustScrollToBottom();
                repaint();
              } else {
                currentMessageIndex++;
                currentCharIndex = 0;

                if (currentMessageIndex >= messages.size() && pendingMessages.size() > 0) {
                  timer.cancel();
                  Timer delayTimer = new Timer();
                  delayTimer.schedule(
                      new TimerTask() {
                        public void run() {
                          String nextMessage = (String) pendingMessages.elementAt(0);
                          pendingMessages.removeElementAt(0);
                          addAIMessage(nextMessage);
                        }
                      },
                      MESSAGE_DELAY);
                  return;
                }

                if (currentMessageIndex >= messages.size()) {
                  timer.cancel();
                }
              }
            }
          }
        },
        300,
        40);
  }

  protected void paint(Graphics g) {
    if (font == null) {
      font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    }
    int width = getWidth();
    int height = getHeight();
    g.setFont(font);

    g.setColor(0xF0F0F0);
    g.fillRect(0, 0, width, height);

    calculateVisibleMessageRange();

    totalContentHeight = BUBBLE_MARGIN;
    for (int i = 0; i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      String textToDraw = (i == currentMessageIndex) ? message.displayText : message.text;
      if (textToDraw.length() > 0) {
        totalContentHeight += getMessageHeight(message, width, font);
      }
    }
    totalContentHeight += BUBBLE_MARGIN;

    int maxScroll = Math.max(0, totalContentHeight - height);
    if (scrollOffset > maxScroll) {
      scrollOffset = maxScroll;
    }

    int yPos = BUBBLE_MARGIN - scrollOffset;

    for (int i = 0; i <= currentMessageIndex && i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      String textToDraw = (i == currentMessageIndex) ? message.displayText : message.text;

      if (textToDraw.length() > 0) {
        int messageHeight = getMessageHeight(message, width, font);

        if (message.isClickable) {
          message.clickableY = yPos + scrollOffset;
          message.clickableHeight = messageHeight;
        }

        if (yPos + messageHeight >= -20 && yPos <= height + 20) {
          drawMessageBubble(
              g,
              textToDraw,
              message.isSent,
              message.isClickable,
              yPos,
              width,
              font,
              (message.isClickable && i == focusedClickableIndex));
        }

        yPos += messageHeight;

        if (yPos > height + 100) {
          break;
        }
      }
    }
  }

  private int calculateMessageHeight(String text, int screenWidth, Font font) {
    if (text == null || text.length() == 0) {
      return 0;
    }
    int maxBubbleWidth = screenWidth - (BUBBLE_MARGIN * 2) - 40;
    Vector wrappedLines = wrapText(text, font, maxBubbleWidth - (BUBBLE_PADDING * 2));
    int textHeight = wrappedLines.size() * font.getHeight();
    return Math.max(font.getHeight(), textHeight + (BUBBLE_PADDING * 2)) + MESSAGE_SPACING;
  }

  private int drawMessageBubble(
      Graphics g,
      String text,
      boolean isSent,
      boolean isClickable,
      int yPos,
      int screenWidth,
      Font font,
      boolean highlighted) {
    String textToRender = text;
    if (isClickable) {
      textToRender = MIDPlay.replace(textToRender, "[", "");
      textToRender = MIDPlay.replace(textToRender, "].", "");
      textToRender = MIDPlay.replace(textToRender, "]", "");
    }

    Vector wrappedLines =
        wrapText(textToRender, font, screenWidth - (BUBBLE_MARGIN * 2) - 40 - (BUBBLE_PADDING * 2));
    int textHeight = wrappedLines.size() * font.getHeight();
    int bubbleHeight = textHeight + (BUBBLE_PADDING * 2);

    if (yPos + bubbleHeight < 0 || yPos > getHeight()) {
      return bubbleHeight + MESSAGE_SPACING;
    }

    int bubbleWidth = 0;
    for (int i = 0; i < wrappedLines.size(); i++) {
      String line = (String) wrappedLines.elementAt(i);
      int lineWidth = font.stringWidth(line);
      if (lineWidth > bubbleWidth) {
        bubbleWidth = lineWidth;
      }
    }
    bubbleWidth += (BUBBLE_PADDING * 2);
    if (bubbleWidth < 20) {
      bubbleWidth = 20;
    }

    int bubbleX = isSent ? (screenWidth - bubbleWidth - BUBBLE_MARGIN) : BUBBLE_MARGIN;

    if (highlighted) {
      g.setColor(0x410A4A);
      g.drawRect(bubbleX - 1, yPos - 1, bubbleWidth + 1, bubbleHeight + 1);
    }

    drawRoundedBubble(g, bubbleX, yPos, bubbleWidth, bubbleHeight, isSent);

    if (isClickable) {
      g.setColor(0x0000FF);
    } else {
      g.setColor(isSent ? SENT_TEXT_COLOR : RECEIVED_TEXT_COLOR);
    }

    int textY = yPos + BUBBLE_PADDING;
    for (int i = 0; i < wrappedLines.size(); i++) {
      String line = (String) wrappedLines.elementAt(i);
      g.drawString(line, bubbleX + BUBBLE_PADDING, textY, Graphics.TOP | Graphics.LEFT);
      textY += font.getHeight();
    }

    return bubbleHeight + MESSAGE_SPACING;
  }

  private void drawRoundedBubble(Graphics g, int x, int y, int width, int height, boolean isSent) {
    int bubbleColor = isSent ? 0x410A4A : RECEIVED_BUBBLE_COLOR;

    if (!isSent) {
      g.setColor(BUBBLE_BORDER_COLOR);
      g.fillRect(x, y + 4, width, height - 8);
      g.fillRect(x + 4, y, width - 8, height);
      g.fillRect(x + 2, y + 2, 4, 4);
      g.fillRect(x + width - 6, y + 2, 4, 4);
      g.fillRect(x + 2, y + height - 6, 4, 4);
      g.fillRect(x + width - 6, y + height - 6, 4, 4);
      g.fillRect(x + 1, y + 3, 2, 2);
      g.fillRect(x + width - 3, y + 3, 2, 2);
      g.fillRect(x + 1, y + height - 5, 2, 2);
      g.fillRect(x + width - 3, y + height - 5, 2, 2);
      g.fillRect(x + 3, y + 1, 2, 2);
      g.fillRect(x + width - 5, y + 1, 2, 2);
      g.fillRect(x + 3, y + height - 3, 2, 2);
      g.fillRect(x + width - 5, y + height - 3, 2, 2);
    }

    g.setColor(bubbleColor);
    g.fillRect(x + 1, y + 4, width - 2, height - 8);
    g.fillRect(x + 4, y + 1, width - 8, height - 2);
    g.fillRect(x + 2, y + 2, 4, 4);
    g.fillRect(x + width - 6, y + 2, 4, 4);
    g.fillRect(x + 2, y + height - 6, 4, 4);
    g.fillRect(x + width - 6, y + height - 6, 4, 4);
    g.fillRect(x + 2, y + 3, 2, 2);
    g.fillRect(x + width - 4, y + 3, 2, 2);
    g.fillRect(x + 2, y + height - 5, 2, 2);
    g.fillRect(x + width - 4, y + height - 5, 2, 2);
    g.fillRect(x + 3, y + 2, 2, 2);
    g.fillRect(x + width - 5, y + 2, 2, 2);
    g.fillRect(x + 3, y + height - 4, 2, 2);
    g.fillRect(x + width - 5, y + height - 4, 2, 2);
  }

  private Vector wrapText(String text, Font font, int maxWidth) {
    Vector lines = new Vector();
    String[] words = splitBySpace(text);

    StringBuffer currentLineBuffer = new StringBuffer();
    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      if (word.equals("\n")) {
        lines.addElement(currentLineBuffer.toString());
        currentLineBuffer = new StringBuffer();
        continue;
      }

      if (font.stringWidth(word) > maxWidth) {
        for (int j = 0; j < word.length(); j++) {
          String ch = word.substring(j, j + 1);
          StringBuffer testBuffer = new StringBuffer(currentLineBuffer.toString());
          testBuffer.append(ch);
          if (font.stringWidth(testBuffer.toString()) > maxWidth
              && currentLineBuffer.length() > 0) {
            lines.addElement(currentLineBuffer.toString());
            currentLineBuffer = new StringBuffer();
          }
          currentLineBuffer.append(ch);
        }
        continue;
      }

      StringBuffer testLineBuffer = new StringBuffer();
      if (currentLineBuffer.length() == 0) {
        testLineBuffer.append(word);
      } else {
        testLineBuffer.append(currentLineBuffer.toString()).append(" ").append(word);
      }

      if (font.stringWidth(testLineBuffer.toString()) > maxWidth
          && currentLineBuffer.length() > 0) {
        lines.addElement(currentLineBuffer.toString());
        currentLineBuffer = new StringBuffer(word);
      } else {
        currentLineBuffer = testLineBuffer;
      }
    }

    if (currentLineBuffer.length() > 0) {
      lines.addElement(currentLineBuffer.toString());
    }

    return lines;
  }

  private String[] splitBySpace(String text) {
    Vector parts = new Vector();
    int len = text.length();
    StringBuffer word = new StringBuffer();

    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (c == ' ') {
        if (word.length() > 0) {
          parts.addElement(word.toString());
          word.setLength(0);
        }
      } else if (c == '\n') {
        if (word.length() > 0) {
          parts.addElement(word.toString());
          word.setLength(0);
        }
        parts.addElement("\n");
      } else {
        word.append(c);
      }
    }

    if (word.length() > 0) {
      parts.addElement(word.toString());
    }

    String[] arr = new String[parts.size()];
    parts.copyInto(arr);
    return arr;
  }

  protected void keyPressed(int keyCode) {
    int action = getGameAction(keyCode);
    if (action == UP || keyCode == KEY_NUM2) {
      if (!moveFocus(-1)) {
        scrollUp();
      }
    } else if (action == DOWN || keyCode == KEY_NUM8) {
      if (!moveFocus(1)) {
        scrollDown();
      }
    } else if (action == FIRE || keyCode == KEY_NUM5) {
      if (focusedClickableIndex != -1) {
        Message msg = (Message) messages.elementAt(focusedClickableIndex);

        int screenY = (msg.clickableY - scrollOffset) + (msg.clickableHeight / 2);
        handlePointerEvent(getWidth() / 2, screenY);

      } else {
        handlePointerEvent(getWidth() / 2, getHeight() / 2);
      }
    }
  }

  protected void pointerPressed(int x, int y) {
    lastPointerY = y;
    isDragging = false;
    dragStartY = y;
    dragStartScrollOffset = scrollOffset;
  }

  protected void pointerDragged(int x, int y) {
    if (lastPointerY != -1) {
      int deltaY = y - dragStartY;

      if (!isDragging && Math.abs(deltaY) > 5) {
        isDragging = true;
      }

      if (isDragging) {
        int newScrollOffset = dragStartScrollOffset - deltaY;
        int maxScroll = Math.max(0, totalContentHeight - getHeight());

        if (newScrollOffset < 0) {
          newScrollOffset = 0;
        } else if (newScrollOffset > maxScroll) {
          newScrollOffset = maxScroll;
        }

        if (newScrollOffset != scrollOffset) {
          scrollOffset = newScrollOffset;
          repaint();
        }
      }
    }
    lastPointerY = y;
  }

  protected void pointerReleased(int x, int y) {
    if (!isDragging) {
      handlePointerEvent(x, y);
    }

    lastPointerY = -1;
    isDragging = false;
    dragStartY = -1;
    dragStartScrollOffset = -1;
  }

  private void handlePointerEvent(int x, int y) {
    int absoluteY = y + scrollOffset;

    for (int i = 0; i < messages.size(); i++) {
      Message message = (Message) messages.elementAt(i);
      if (message.isClickable && message.clickableY != 0) {
        if (absoluteY >= message.clickableY
            && absoluteY <= message.clickableY + message.clickableHeight) {
          String displayText = message.text;

          if (displayText.startsWith("[")) {
            displayText = displayText.substring(1);
          }
          if (displayText.endsWith("].")) {
            displayText = displayText.substring(0, displayText.length() - 2);
          } else if (displayText.endsWith("]")) {
            displayText = displayText.substring(0, displayText.length() - 1);
          }

          final String finalDisplayText = displayText;
          searchTracks(finalDisplayText);
        }
      }
    }
  }

  private void searchTracks(final String query) {
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    MIDPlay.startOperation(
        TracksOperation.searchTracks(
            query,
            new TracksOperation.TracksListener() {
              public void onDataReceived(Tracks items) {
                String searchResultsTitle = Lang.tr("search.results") + ": " + query;
                TrackListScreen trackListScreen =
                    new TrackListScreen(searchResultsTitle, items, navigator);
                navigator.forward(trackListScreen);
              }

              public void onNoDataReceived() {
                navigator.showAlert(Lang.tr("search.status.no_results"), AlertType.INFO);
              }

              public void onError(Exception e) {
                navigator.showAlert(e.toString(), AlertType.ERROR);
              }
            }));
  }

  private boolean moveFocus(int direction) {
    int start =
        (focusedClickableIndex == -1)
            ? (direction > 0 ? -1 : messages.size())
            : focusedClickableIndex;
    int idx = start;
    while (true) {
      idx += direction;
      if (idx < 0 || idx >= messages.size()) {
        return false;
      }
      Message m = (Message) messages.elementAt(idx);
      if (m.isClickable) {
        focusedClickableIndex = idx;
        ensureFocusVisible();
        repaint();
        return true;
      }
    }
  }

  private void ensureFocusVisible() {
    if (focusedClickableIndex == -1) {
      return;
    }
    int width = getWidth();
    Font f =
        (font != null)
            ? font
            : Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    int y = BUBBLE_MARGIN;
    for (int i = 0; i < messages.size(); i++) {
      Message m = (Message) messages.elementAt(i);
      String txt = (i == currentMessageIndex) ? m.displayText : m.text;
      if (txt.length() > 0) {
        int h = getMessageHeight(m, width, f);
        if (i == focusedClickableIndex) {
          if (y - scrollOffset < 0) {
            scrollOffset = y;
          } else if (y - scrollOffset + h > getHeight()) {
            scrollOffset = y - getHeight() + h;
          }
          break;
        }
        y += h;
      }
    }
  }

  private void scrollUp() {
    int oldScroll = scrollOffset;
    scrollOffset = Math.max(0, scrollOffset - 20);
    if (oldScroll != scrollOffset) {
      if (Math.abs(oldScroll - scrollOffset) > 100) {
        invalidateMessageHeights();
      }
      repaint();
    }
  }

  private void scrollDown() {
    int maxScroll = Math.max(0, totalContentHeight - getHeight());
    int oldScroll = scrollOffset;
    scrollOffset = Math.min(maxScroll, scrollOffset + 20);
    if (oldScroll != scrollOffset) {
      if (Math.abs(oldScroll - scrollOffset) > 100) {
        invalidateMessageHeights();
      }
      repaint();
    }
  }

  public void commandAction(Command c, Displayable d) {
    if (c == Commands.ok()) {
      if (d == this && focusedClickableIndex != -1) {
        Message msg = (Message) messages.elementAt(focusedClickableIndex);
        int screenY = (msg.clickableY - scrollOffset) + (msg.clickableHeight / 2);
        handlePointerEvent(getWidth() / 2, screenY);
      } else if (d == inputBox) {
        final String text = inputBox.getString().trim();
        if (text.length() > 0 && !isWaitingForResponse) {
          addUserMessage(text);

          addAIMessage("...");

          isWaitingForResponse = true;
          sendChatMessage(text);
        }
        navigator.back();
      }
    } else if (c == Commands.back()) {
      if (d == this) {
        cleanupTimerAndOperations();
      }
      navigator.back();
    } else if (c == Commands.input()) {
      openInputBox();
    }
  }

  private void sendChatMessage(final String message) {
    MIDPlay.startOperation(
        ChatOperation.sendMessage(
            message,
            sessionId,
            new ChatOperation.ChatListener() {
              public void onDataReceived(String response) {
                addAIResponseMessages(response);
                isWaitingForResponse = false;
              }

              public void onNoDataReceived() {
                messages.removeElementAt(messages.size() - 1);
                addAIMessage(Lang.tr("error.connection"));
                isWaitingForResponse = false;
              }

              public void onError(Exception e) {
                messages.removeElementAt(messages.size() - 1);
                addAIMessage(Lang.tr("error.connection"));
                isWaitingForResponse = false;
              }
            }));
  }

  private void openInputBox() {
    inputBox = new TextBox(Lang.tr("chat.input"), "", 200, TextField.ANY);
    inputBox.addCommand(Commands.ok());
    inputBox.addCommand(Commands.back());
    inputBox.setCommandListener(this);
    navigator.forward(inputBox);
  }

  public void hideNotify() {
    cleanupTimerAndOperations();
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    cleanupTimerAndOperations();
    this.sessionId = null;
  }

  private void cleanupTimerAndOperations() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }
}
