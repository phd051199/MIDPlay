package midplay.ui;

import midplay.store.FavoritesManager;
import midplay.util.Lang;

import javax.microedition.lcdui.AlertType;

// Maps a FavoritesManager result code to the standard alert. Replaces the
// identical three-way if/else (SUCCESS/ALREADY_EXISTS/else) that was inlined
// into every screen that adds a playlist to favorites.
public final class FavoritesActions {
  public static void showAddPlaylistResult(Navigator navigator, int result) {
    if (result == FavoritesManager.SUCCESS) {
      navigator.showAlert(Lang.tr("favorites.status.added"), AlertType.CONFIRMATION);
    } else if (result == FavoritesManager.ALREADY_EXISTS) {
      navigator.showAlert(Lang.tr("favorites.status.already_exists"), AlertType.INFO);
    } else {
      navigator.showAlert(Lang.tr("favorites.error.save_failed"), AlertType.ERROR);
    }
  }

  private FavoritesActions() {}
}
