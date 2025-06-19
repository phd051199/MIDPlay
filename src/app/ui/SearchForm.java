package app.ui;

import app.common.ParseData;
import app.interfaces.LoadDataObserver;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

public class SearchForm extends Form implements CommandListener, LoadDataObserver {

  private TextField symbolField = new TextField(I18N.tr("search_hint"), "", 300, 0);
  private ChoiceGroup searchTypeGroup =
      new ChoiceGroup(I18N.tr("search_type"), ChoiceGroup.EXCLUSIVE);
  private Command exitCommand;
  private Command searchCommand;
  private Command nowPlayingCommand;
  private String keyWord = "";
  private String searchType = "playlist";
  private Utils.BreadCrumbTrail observer;
  Thread mLoaDataThread;

  public SearchForm(String title) {
    super(title);
    this.append(this.symbolField);
    this.searchTypeGroup.append(I18N.tr("playlist"), null);
    this.searchTypeGroup.append(I18N.tr("album"), null);
    this.searchTypeGroup.setSelectedIndex(0, true);
    this.append(this.searchTypeGroup);
    this.initMenu();
  }

  public TextField getSymbolField() {
    return this.symbolField;
  }

  public Command getExitCommand() {
    return this.exitCommand;
  }

  public Command getGetCommand() {
    return this.searchCommand;
  }

  public void setObserver(Utils.BreadCrumbTrail _observer) {
    this.observer = _observer;
  }

  public void commandAction(Command c, Displayable d) {
    if (c == this.exitCommand) {
      this.observer.goBack();
    } else if (c == this.searchCommand) {
      this.keyWord = this.symbolField.getString();
      this.gotoSearchPlaylist(this.keyWord, 1, 10);
    } else if (c == this.nowPlayingCommand) {
      MainList.gotoNowPlaying(this.observer);
    }
  }

  private void gotoSearchPlaylist(String keyword, final int curPage, final int perPage) {
    this.searchType = this.searchTypeGroup.isSelected(0) ? "playlist" : "album";
    this.displayMessage(I18N.tr("search_hint") + ": " + keyword, I18N.tr("loading"), "loading");
    this.mLoaDataThread =
        new Thread(
            new Runnable() {
              public void run() {
                Vector listItems =
                    ParseData.parseSearch(
                        "", SearchForm.this.keyWord, curPage, perPage, searchType);
                if (listItems == null) {
                  SearchForm.this.displayMessage("", I18N.tr("connection_error"), "error");
                } else if (listItems.isEmpty()) {
                  MainList.displayMessage(
                      I18N.tr("search"),
                      I18N.tr("no_results"),
                      "error",
                      SearchForm.this.observer,
                      SearchForm.this);
                } else {
                  String searchResultsTitle =
                      I18N.tr("search_results") + ": " + SearchForm.this.keyWord;
                  PlaylistList cateCanvas =
                      new PlaylistList(
                          searchResultsTitle,
                          listItems,
                          "search",
                          SearchForm.this.keyWord,
                          SearchForm.this.searchType);
                  cateCanvas.setObserver(SearchForm.this.observer);
                  SearchForm.this.observer.replaceCurrent(cateCanvas);
                }
              }
            });
    this.mLoaDataThread.start();
  }

  private void displayMessage(String title, String message, String messageType) {
    MainList.displayMessage(title, message, messageType, this.observer, this);
  }

  private void initMenu() {
    this.searchCommand = new Command(I18N.tr("search"), Command.OK, 0);
    this.nowPlayingCommand = new Command(I18N.tr("now_playing"), Command.SCREEN, 1);
    this.exitCommand = new Command(I18N.tr("back"), Command.BACK, 0);
    this.addCommand(this.searchCommand);
    this.addCommand(this.exitCommand);
    this.addCommand(this.nowPlayingCommand);
    this.setCommandListener(this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    try {
      if (this.mLoaDataThread != null && this.mLoaDataThread.isAlive()) {
        this.mLoaDataThread.join();
      }
    } catch (InterruptedException var2) {
      var2.printStackTrace();
    }
  }
}
