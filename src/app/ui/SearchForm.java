package app.ui;

import app.MIDPlay;
import app.common.Common;
import app.common.ParseData;
import app.common.SettingManager;
import app.interfaces.DataLoader;
import app.interfaces.LoadDataListener;
import app.interfaces.LoadDataObserver;
import app.model.Playlist;
import app.utils.I18N;
import app.utils.Utils;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import org.json.me.JSONObject;

public class SearchForm extends Form implements CommandListener, LoadDataObserver {

  private final TextField symbolField = new TextField(I18N.tr("search_hint"), "", 300, 0);
  private final ChoiceGroup searchTypeGroup =
      new ChoiceGroup(I18N.tr("search_type"), ChoiceGroup.EXCLUSIVE);
  private Command exitCommand;
  private Command searchCommand;
  private Command nowPlayingCommand;
  private String keyWord = "";
  private String searchType = "playlist";
  private Utils.BreadCrumbTrail observer;
  Thread mLoadDataThread;

  private final SettingManager settingManager;

  public SearchForm(String title) {
    super(title);

    settingManager = SettingManager.getInstance();

    this.append(this.symbolField);
    this.searchTypeGroup.append(I18N.tr("playlist"), null);
    this.searchTypeGroup.append(I18N.tr("album"), null);
    this.searchTypeGroup.append(I18N.tr("track"), null);

    this.loadSearchConfig();

    this.append(this.searchTypeGroup);
    this.initMenu();
  }

  private void loadSearchConfig() {
    JSONObject config = settingManager.loadConfigSync();
    int savedTypeIndex = config.optInt("searchTypeIndex", 0);

    if (savedTypeIndex >= 0 && savedTypeIndex < searchTypeGroup.size()) {
      searchTypeGroup.setSelectedIndex(savedTypeIndex, true);
    }
  }

  private void saveSearchConfig() {
    try {
      JSONObject config = new JSONObject();
      config.put("searchTypeIndex", new Integer(searchTypeGroup.getSelectedIndex()));

      settingManager.saveConfig(config);
    } catch (Exception e) {

    }
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
    if (keyword.length() == 0) {
      showAlert("", I18N.tr("search_keyword_empty"), AlertType.ERROR);
      return;
    }

    switch (this.searchTypeGroup.getSelectedIndex()) {
      case 0:
        this.searchType = "playlist";
        break;
      case 1:
        this.searchType = "album";
        break;
      case 2:
        this.searchType = "track";
        break;
    }

    saveSearchConfig();

    this.displayMessage(I18N.tr("search_hint") + ": " + keyword, I18N.tr("loading"), "loading");
    Common.loadDataAsync(
        new DataLoader() {
          public Vector load() throws Exception {
            if (SearchForm.this.searchType.equals("track")) {
              return ParseData.parseSearchTracks(SearchForm.this.keyWord);
            } else {
              return ParseData.parseSearch(
                  "", SearchForm.this.keyWord, curPage, perPage, searchType);
            }
          }
        },
        new LoadDataListener() {
          public void loadDataCompleted(Vector listItems) {

            if (SearchForm.this.searchType.equals("track")) {
              String searchResultsTitle =
                  I18N.tr("search_results") + ": " + SearchForm.this.keyWord;
              Playlist searchPlaylist = new Playlist();
              searchPlaylist.setName(searchResultsTitle);
              searchPlaylist.setId("search");

              SongList songList = new SongList(searchResultsTitle, listItems, searchPlaylist);
              songList.setObserver(SearchForm.this.observer);
              SearchForm.this.observer.replaceCurrent(songList);
            } else {
              String searchResultsTitle =
                  I18N.tr("search_results") + ": " + SearchForm.this.keyWord;
              PlaylistList playlistList =
                  new PlaylistList(
                      searchResultsTitle,
                      listItems,
                      "search",
                      SearchForm.this.keyWord,
                      SearchForm.this.searchType);
              playlistList.setObserver(SearchForm.this.observer);
              SearchForm.this.observer.replaceCurrent(playlistList);
            }
          }

          public void loadError() {
            SearchForm.this.displayMessage("", I18N.tr("connection_error"), "error");
          }

          public void noData() {
            MainList.displayMessage(
                I18N.tr("search"),
                I18N.tr("no_results"),
                "error",
                SearchForm.this.observer,
                SearchForm.this);
          }
        },
        this.mLoadDataThread);
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

  private void showAlert(String title, String message, AlertType type) {
    Alert alert = new Alert(title, message, null, type);
    alert.setTimeout(2000);
    MIDPlay.getInstance().getDisplay().setCurrent(alert, SearchForm.this);
  }

  public void cancel() {
    this.quit();
  }

  public void quit() {
    if (settingManager != null) {
      settingManager.shutdown();
    }

    try {
      if (this.mLoadDataThread != null && this.mLoadDataThread.isAlive()) {
        this.mLoadDataThread.join();
      }
    } catch (InterruptedException var2) {
    }
  }
}
