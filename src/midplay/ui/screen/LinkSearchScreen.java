package midplay.ui.screen;

import cc.nnproject.json.JSONObject;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import midplay.MIDPlay;
import midplay.model.JsonListResult;
import midplay.model.Tracks;
import midplay.net.JsonOperation;
import midplay.store.DownloadManager;
import midplay.ui.AbstractListForwarder;
import midplay.ui.BaseForm;
import midplay.ui.Commands;
import midplay.ui.FormHelpers;
import midplay.ui.Navigator;
import midplay.util.Lang;

public final class LinkSearchScreen extends BaseForm {
  private static final int MAX_KEYWORD_LENGTH = 150;

  private final DownloadManager downloadManager = DownloadManager.getInstance();
  private final Displayable returnScreen;
  private final String path;
  private final TextField keywordField;

  public LinkSearchScreen(
      Navigator navigator, Displayable returnScreen, String path, String keyword) {
    super(Lang.tr("download.link"), navigator);
    this.returnScreen = returnScreen;
    this.path = path;
    this.append(new StringItem(null, downloadManager.fileNameForPath(path)));
    this.keywordField =
        new TextField(
            Lang.tr("search.placeholder"),
            keyword != null ? keyword : "",
            MAX_KEYWORD_LENGTH,
            TextField.ANY);
    this.append(this.keywordField);
    addCommand(Commands.ok());
    addCommand(Commands.downloadLinkManual());
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.ok()) {
      search();
    } else if (c == Commands.downloadLinkManual()) {
      manualLink();
    }
  }

  private void search() {
    final String keyword = this.keywordField.getString().trim();
    if (keyword.length() == 0) {
      navigator.showAlert(Lang.tr("search.error.empty_keyword"), AlertType.ERROR);
      return;
    }
    navigator.showLoadingAlert(Lang.tr("search.status.searching", keyword));
    final String title = Lang.tr("search.results") + ": " + keyword;
    MIDPlay.startOperation(
        JsonOperation.searchTracks(
            keyword,
            new AbstractListForwarder(navigator, "search.status.no_results") {
              public void onDataReceived(JsonListResult result) {
                navigator.forward(
                    new LinkTrackPickerScreen(
                        title, (Tracks) result, navigator, path, returnScreen));
              }
            }));
  }

  private void manualLink() {
    final String fileName = downloadManager.fileNameForPath(path);
    JSONObject entry = downloadManager.getLinkedEntry(path);
    String url = entryValue(entry, "url");
    String thumb = entryValue(entry, "thumb");
    if (url.equals(path)) {
      url = "";
    }
    if (thumb.equals(path)) {
      thumb = "";
    }
    FormHelpers.promptLink(
        navigator,
        fileName,
        url,
        thumb,
        entryValue(entry, "name"),
        entryValue(entry, "artist"),
        new FormHelpers.LinkSubmitHandler() {
          public void onSubmit(String linkUrl, String linkThumb, String linkName, String linkArtist) {
            if (downloadManager.linkLocalFile(path, linkUrl, linkThumb, linkName, linkArtist)) {
              if (returnScreen instanceof LocalFileLinkScreen) {
                ((LocalFileLinkScreen) returnScreen).onFileLinked();
              }
              navigator.popTo(returnScreen);
              navigator.showAlert(
                  Lang.tr("download.link_status"), AlertType.CONFIRMATION, returnScreen);
            } else {
              navigator.showAlert(Lang.tr("download.link_error"), AlertType.ERROR);
            }
          }
        });
  }

  private static String entryValue(JSONObject entry, String key) {
    if (entry == null) {
      return "";
    }
    String value = entry.getString(key, "");
    return value != null ? value : "";
  }
}
