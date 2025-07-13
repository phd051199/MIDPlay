package app.core.data;

import java.util.Vector;

public interface FavoritesCallback {

  void onFavoritesLoaded(Vector favorites);

  void onFavoriteAdded();

  void onFavoriteRemoved();

  void onCustomPlaylistCreated();

  void onCustomPlaylistRenamed();

  void onCustomPlaylistDeletedWithSongs();

  void onCustomPlaylistSongsLoaded(Vector songs);

  void onError(String message);
}
