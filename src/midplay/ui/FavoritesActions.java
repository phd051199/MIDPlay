package midplay.ui;

import javax.microedition.lcdui.AlertType;
import midplay.store.FavoritesManager;
import midplay.util.Lang;

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
