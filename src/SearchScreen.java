import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStoreException;
import model.Playlists;
import model.Tracks;

public final class SearchScreen extends BaseForm {
  private static final int MAX_INPUT_LENGTH = 150;
  private static final String[] SEARCH_TYPE_LABELS = {
    Lang.tr(Configuration.SearchType.PLAYLIST),
    Lang.tr(Configuration.SearchType.ALBUM),
    Lang.tr(Configuration.SearchType.TRACK)
  };
  private final TextField searchField =
      new TextField(Lang.tr("search.placeholder"), "", MAX_INPUT_LENGTH, 0);
  private final ChoiceGroup searchTypeGroup =
      new ChoiceGroup(Lang.tr("search.type"), ChoiceGroup.EXCLUSIVE);
  private String searchType;
  private final SettingsManager settingsManager;

  public SearchScreen(Navigator navigator) {
    super(Lang.tr(Configuration.Menu.SEARCH), navigator);
    this.settingsManager = SettingsManager.getInstance();
    this.searchType = settingsManager.getCurrentSearchType();
    addComponents();
    addCommand(Commands.ok());
  }

  private void addComponents() {
    this.append(this.searchField);
    for (int i = 0; i < SEARCH_TYPE_LABELS.length; i++) {
      this.searchTypeGroup.append(SEARCH_TYPE_LABELS[i], null);
    }
    int selectedIndex = getSearchTypeIndex(this.searchType);
    this.searchTypeGroup.setSelectedIndex(selectedIndex, true);
    this.append(this.searchTypeGroup);
  }

  private int getSearchTypeIndex(String searchType) {
    for (int i = 0; i < Configuration.SearchType.ALL.length; i++) {
      if (Configuration.SearchType.ALL[i].equals(searchType)) {
        return i;
      }
    }
    return 0;
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.ok()) {
      search();
    }
  }

  private void search() {
    final String keyword = this.searchField.getString().trim();
    if (keyword.length() == 0) {
      navigator.showAlert(Lang.tr("search.error.empty_keyword"), AlertType.ERROR);
      return;
    }
    int selectedIndex = this.searchTypeGroup.getSelectedIndex();
    this.searchType = Configuration.SearchType.ALL[selectedIndex];
    try {
      settingsManager.saveSearchType(this.searchType);
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
    navigator.showLoadingAlert(Lang.tr("search.status.searching", keyword));
    final String title = Lang.tr("search.results") + ": " + keyword;
    if (searchType.equals(Configuration.SearchType.TRACK)) {
      MIDPlay.startOperation(
          TracksOperation.searchTracks(
              keyword,
              new TracksOperation.TracksListener() {
                public void onDataReceived(Tracks items) {
                  TrackListScreen trackListScreen = new TrackListScreen(title, items, navigator);
                  navigator.forward(trackListScreen);
                }

                public void onNoDataReceived() {
                  navigator.showAlert(Lang.tr("search.status.no_results"), AlertType.INFO);
                }

                public void onError(Exception e) {
                  navigator.showAlert(e.toString(), AlertType.ERROR);
                }
              }));
    } else {
      MIDPlay.startOperation(
          PlaylistsOperation.searchPlaylists(
              keyword,
              searchType,
              new PlaylistsOperation.PlaylistsListener() {
                public void onDataReceived(Playlists items) {
                  PlaylistListScreen playlistScreen =
                      new PlaylistListScreen(title, items, navigator, keyword, searchType);
                  navigator.forward(playlistScreen);
                }

                public void onNoDataReceived() {
                  navigator.showAlert(Lang.tr("search.status.no_results"), AlertType.INFO);
                }

                public void onError(Exception e) {
                  navigator.showAlert(e.toString(), AlertType.ERROR);
                }
              }));
    }
  }
}
