package app.core.data;

import java.util.Vector;

public abstract class FavoritesDataLoader implements DataLoader {

  public static final int OPERATION_LOAD_FAVORITES = 1;
  public static final int OPERATION_ADD_FAVORITE = 2;
  public static final int OPERATION_REMOVE_FAVORITE = 3;
  public static final int OPERATION_CREATE_CUSTOM_PLAYLIST = 4;
  public static final int OPERATION_RENAME_CUSTOM_PLAYLIST = 5;
  public static final int OPERATION_LOAD_CUSTOM_PLAYLIST_SONGS = 6;
  public static final int OPERATION_REMOVE_CUSTOM_PLAYLIST_WITH_SONGS = 7;

  protected final int operationType;
  protected final Object operationData;

  protected FavoritesDataLoader(int operationType, Object operationData) {
    this.operationType = operationType;
    this.operationData = operationData;
  }

  public abstract Vector load() throws Exception;

  public int getOperationType() {
    return operationType;
  }

  public Object getOperationData() {
    return operationData;
  }
}
