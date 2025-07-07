package app.ui;

import app.MIDPlay;
import app.common.Common;
import app.common.ParseData;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.interfaces.MainObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.TextUtil;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class ChatCanvas extends Canvas implements CommandListener, LoadDataObserver {

  private static final int MAX_VISIBLE_MESSAGES = 5;
  private static final int MAX_STORED_MESSAGES = 500;
  private static final int MESSAGE_BUFFER = 3;

  private static final int BACKGROUND_COLOR = 0xF0F0F0;
  private static final int SENT_BUBBLE_COLOR = 0x410A4A;
  private static final int RECEIVED_BUBBLE_COLOR = 0xFFFFFF;
  private static final int SENT_TEXT_COLOR = 0xFFFFFF;
  private static final int RECEIVED_TEXT_COLOR = 0x000000;
  private static final int BUBBLE_BORDER_COLOR = 0xE0E0E0;

  private static final int BUBBLE_PADDING = 8;
  private static final int BUBBLE_MARGIN = 6;
  private static final int MESSAGE_SPACING = 5;

  private static final int CHAR_PER_LOOP = 4;
  private static final int MESSAGE_DELAY = 50;

  private Vector messages = new Vector();
  private int currentMessageIndex = 0;
  private int currentCharIndex = 0;
  private Timer timer;
  private boolean isWaitingForResponse = false;
  private Displayable previousDisplay;
  private final Vector pendingMessages = new Vector();
  private Thread mLoadDataThread;
  private int firstVisibleMessage = 0;
  private int lastVisibleMessage = 0;

  private int scrollOffset = 0;
  private int focusedClickableIndex = -1;
  private int totalContentHeight = 0;

  private final Command backCommand = new Command(I18N.tr("back"), Command.BACK, 0);
  private final Command inputCommand = new Command(I18N.tr("input"), Command.SCREEN, 1);
  private final Command selectCommand = new Command(I18N.tr("select"), Command.OK, 1);

  private TextBox inputBox;
  private Font font;
  private String sessionId;

  private final MainObserver observer;

  public ChatCanvas(String title, MainObserver observer) {
    this.observer = observer;
    this.setTitle(title);
    this.addCommand(selectCommand);
    this.addCommand(backCommand);
    this.addCommand(inputCommand);
    this.setCommandListener(this);
    this.sessionId = TextUtil.generateRandomId(12);
  }

  public void showNotify() {
    super.showNotify();

    if (this.messages.isEmpty()) {
      addAIMessage(I18N.tr("welcome_message"));
      startTypingEffect();
    }
  }

  private void addAIMessage(String text) {
    Message msg = new Message(text, false);
    messages.addElement(msg);
    currentMessageIndex = messages.size() - 1;
    currentCharIndex = 0;
    msg.displayText = "";
    startTypingEffect();
  }

  private void addUserMessage(String text) {
    Message msg = new Message(text, true);
    messages.addElement(msg);
    currentMessageIndex = messages.size() - 1;
    currentCharIndex = 0;
    msg.displayText = "";
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

    if (messages.size() > MAX_STORED_MESSAGES && isScrolledToBottom()) {
      int messagesToRemove = messages.size() - MAX_VISIBLE_MESSAGES;

      if (messagesToRemove > 0 && messagesToRemove < messages.size() / 2) {

        Vector messagesToKeep = new Vector();
        for (int i = messagesToRemove; i < messages.size(); i++) {
          messagesToKeep.addElement(messages.elementAt(i));
        }

        messages = messagesToKeep;

        currentMessageIndex -= messagesToRemove;
        if (currentMessageIndex < 0) {
          currentMessageIndex = 0;
        }

        firstVisibleMessage = Math.max(0, firstVisibleMessage - messagesToRemove);
        lastVisibleMessage = Math.max(0, lastVisibleMessage - messagesToRemove);
      }
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
                  timer = new Timer();
                  timer.schedule(
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

    g.setColor(BACKGROUND_COLOR);
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
      textToRender = Common.replace(textToRender, "[", "");
      textToRender = Common.replace(textToRender, "].", "");
      textToRender = Common.replace(textToRender, "]", "");
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
      g.setColor(SENT_BUBBLE_COLOR);
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
    int bubbleColor = isSent ? SENT_BUBBLE_COLOR : RECEIVED_BUBBLE_COLOR;

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
    String currentLine = "";

    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      if (word.equals("\n")) {
        lines.addElement(currentLine);
        currentLine = "";
        continue;
      }

      if (font.stringWidth(word) > maxWidth) {
        for (int j = 0; j < word.length(); j++) {
          String ch = word.substring(j, j + 1);
          if (font.stringWidth(currentLine + ch) > maxWidth && currentLine.length() > 0) {
            lines.addElement(currentLine);
            currentLine = "";
          }
          currentLine += ch;
        }
        continue;
      }

      String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

      if (font.stringWidth(testLine) > maxWidth && currentLine.length() > 0) {
        lines.addElement(currentLine);
        currentLine = word;
      } else {
        currentLine = testLine;
      }
    }

    if (currentLine.length() > 0) {
      lines.addElement(currentLine);
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
    handlePointerEvent(x, y);
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

          Common.loadDataAsync(
              new DataLoader() {
                public Vector load() {
                  return ParseData.parseSearchTracks(finalDisplayText);
                }
              },
              new LoadDataListener() {
                public void loadDataCompleted(Vector data) {
                  String searchResultsTitle = I18N.tr("search_results") + ": " + finalDisplayText;
                  Playlist searchPlaylist = new Playlist();
                  searchPlaylist.setName(searchResultsTitle);
                  searchPlaylist.setId("search");

                  SongList songList = new SongList(searchResultsTitle, data, searchPlaylist);
                  songList.setObserver(observer);
                  observer.go(songList);
                }

                public void loadError() {
                  displayAlert(I18N.tr("connection_error"), AlertType.ERROR);
                }

                public void noData() {
                  displayAlert(I18N.tr("no_results"), AlertType.ERROR);
                }
              },
              this.mLoadDataThread);
        }
      }
    }
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
    int maxScroll = Math.max(0, totalContentHeight - getHeight() + BUBBLE_MARGIN);
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
    if (c == selectCommand) {
      if (focusedClickableIndex != -1) {
        Message msg = (Message) messages.elementAt(focusedClickableIndex);
        int screenY = (msg.clickableY - scrollOffset) + (msg.clickableHeight / 2);
        handlePointerEvent(getWidth() / 2, screenY);
      }
    } else if (c == backCommand && observer != null) {
      if (timer != null) {
        timer.cancel();
      }
      observer.goBack();
    } else if (c == inputCommand) {
      previousDisplay = MIDPlay.getInstance().getDisplay().getCurrent();
      openInputBox();
    } else if (d == inputBox && c.getCommandType() == Command.OK) {
      final String text = inputBox.getString().trim();
      if (text.length() > 0 && !isWaitingForResponse) {

        addUserMessage(text);

        addAIMessage("...");

        isWaitingForResponse = true;
        Common.loadDataAsync(
            new DataLoader() {
              public Vector load() throws Exception {
                Vector v = new Vector();
                v.addElement(ParseData.sendChatMessage(text, sessionId));
                return v;
              }
            },
            new LoadDataListener() {
              public void loadDataCompleted(Vector data) {
                String response = (String) data.elementAt(0);
                addAIResponseMessages(response);
                isWaitingForResponse = false;
              }

              public void loadError() {
                messages.removeElementAt(messages.size() - 1);
                addAIMessage(I18N.tr("error_connect"));
                isWaitingForResponse = false;
              }

              public void noData() {
                messages.removeElementAt(messages.size() - 1);
                addAIMessage(I18N.tr("error_connect"));
                isWaitingForResponse = false;
              }
            },
            this.mLoadDataThread);
      }
      MIDPlay.getInstance().getDisplay().setCurrent(previousDisplay);
    } else if (d == inputBox && c.getCommandType() == Command.CANCEL) {
      MIDPlay.getInstance().getDisplay().setCurrent(previousDisplay);
    }
  }

  private void openInputBox() {
    inputBox = new TextBox(I18N.tr("input_message"), "", 200, TextField.ANY);
    inputBox.addCommand(new Command(I18N.tr("send"), Command.OK, 1));
    inputBox.addCommand(new Command(I18N.tr("cancel"), Command.CANCEL, 2));
    inputBox.setCommandListener(this);
    MIDPlay.getInstance().getDisplay().setCurrent(inputBox);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoadDataThread != null && this.mLoadDataThread.isAlive()) {
        this.mLoadDataThread.join();
      }
      this.sessionId = null;
    } catch (InterruptedException var2) {
    }
  }

  private void displayAlert(String message, AlertType messageType) {
    Alert alert = new Alert("", message, null, messageType);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, this);
  }

  private class Message {
    String text;
    boolean isSent;
    boolean isClickable;
    String displayText = "";
    int height = -1;
    int clickableY;
    int clickableHeight;

    Message(String text, boolean isSent) {
      this.text = text;
      this.isSent = isSent;
      this.isClickable = text.startsWith("[") && (text.endsWith("]") || text.endsWith("]."));
    }
  }
}
