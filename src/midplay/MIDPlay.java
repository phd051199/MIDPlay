package midplay;

import java.io.IOException;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import midplay.model.Tracks;
import midplay.net.CheckUpdateOperation;
import midplay.net.JsonOperation;
import midplay.net.NetworkOperation;
import midplay.player.PlayerGUI;
import midplay.player.PlayerScreen;
import midplay.store.Configuration;
import midplay.store.DownloadManager;
import midplay.store.FavoritesManager;
import midplay.store.LastSessionManager;
import midplay.store.NotesManager;
import midplay.store.RecentManager;
import midplay.store.SettingsManager;
import midplay.store.TasksManager;
import midplay.ui.Commands;
import midplay.ui.MainMenuScreen;
import midplay.ui.MenuManager;
import midplay.ui.Navigator;
import midplay.ui.PlayerNavHelper;
import midplay.ui.PlaylistsListForwarder;
import midplay.ui.screen.EqualizerScreen;
import midplay.ui.screen.FavoritesScreen;
import midplay.ui.screen.RecentListScreen;
import midplay.ui.screen.SearchScreen;
import midplay.ui.screen.SettingsScreen;
import midplay.ui.screen.DownloadsScreen;
import midplay.ui.screen.FileBrowserScreen;
import midplay.ui.screen.OpenUriScreen;
import midplay.ui.screen.NotesScreen;
import midplay.ui.screen.TasksScreen;
import midplay.util.Lang;
import midplay.util.Utils;
import mpgram.MP;
import githubj2me.GH;

public class MIDPlay extends MIDlet implements MainMenuScreen.MenuHost, MP.Host, GH.Host {
  private static NetworkOperation operation;
  public static String APP_VERSION = "1.0";
  private static PlayerScreen playerScreen;
  private static MIDPlay instance;

  public static synchronized void startOperation(NetworkOperation op) {
    if (operation != null) {
      operation.stop();
    }
    operation = op;
    operation.start();
  }

  public static synchronized void cancelOperation() {
    if (operation != null) {
      operation.stop();
      operation = null;
    }
  }

  public static PlayerScreen getPlayerScreen() {
    return playerScreen;
  }

  public static void setPlayerScreen(PlayerScreen ps) {
    playerScreen = ps;
  }

  public static MIDPlay getInstance() {
    return instance;
  }

  private final SettingsManager settingsManager;
  private final Navigator navigator;
  private final MenuManager menuManager;
  private MainMenuScreen mainMenu;
  private boolean initialized;
  private Displayable displayBeforePause;
  private boolean resumePlaybackAfterPause;
  private long interruptedPlaybackPosition;

  public MIDPlay() {
    instance = this;
    menuManager = MenuManager.getInstance();
    settingsManager = SettingsManager.getInstance();
    navigator = new Navigator(this);
    registerMenuActions();
  }

  private void registerMenuActions() {
    menuManager.registerAction(
        Configuration.MENU_SEARCH,
        new Runnable() {
          public void run() {
            goToSearchScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_FAVORITES,
        new Runnable() {
          public void run() {
            goToFavoritesScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_DISCOVER_PLAYLISTS,
        new Runnable() {
          public void run() {
            goToDiscoverPlaylistsScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_RECENT,
        new Runnable() {
          public void run() {
            goToRecentScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_DOWNLOADS,
        new Runnable() {
          public void run() {
            goToDownloadsScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_FILES,
        new Runnable() {
          public void run() {
            goToFileBrowserScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_SETTINGS,
        new Runnable() {
          public void run() {
            goToSettingsScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_ABOUT,
        new Runnable() {
          public void run() {
            goToAboutScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_EQUALIZER,
        new Runnable() {
          public void run() {
            goToEqualizerScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_MPGRAM,
        new Runnable() {
          public void run() {
            openMPGram();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_GITHUB,
        new Runnable() {
          public void run() {
            openGitHubJ2ME();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_NOTES,
        new Runnable() {
          public void run() {
            goToNotesScreen();
          }
        });
    menuManager.registerAction(
        Configuration.MENU_TASKS,
        new Runnable() {
          public void run() {
            goToTasksScreen();
          }
        });
  }

  public void openUri() {
    navigator.forward(new OpenUriScreen(navigator));
  }

  public void openNotePickerForMPGram(String peerId, String topMessageId) {
    Display.getDisplay(this).setCurrent(new MPGramNotePicker(peerId, topMessageId));
  }

  public void openSessionNotePickerForMPGram() {
    Display.getDisplay(this).setCurrent(new MPGramSessionNotePicker());
  }

  public boolean saveMPGramMessageAsNote(String text) {
    try {
      NotesManager.getInstance().create(text == null ? "" : text);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean saveMPGramSessionCodeAsNote(String text) {
    try {
      NotesManager.Note note = NotesManager.getInstance().create(text == null ? "" : text);
      NotesManager.getInstance().updateCustomTitle(note.id, Lang.tr("mpgram.session_code_note_title"));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void openTokenNotePicker(int apiMode) {
    Display.getDisplay(this).setCurrent(new GitHubTokenNotePicker(apiMode));
  }

  public void openMIDPlayNowPlayingFromMPGram() {
    openNowPlaying();
  }

  public void openMIDPlayNowPlayingFromGH() {
    openNowPlaying();
  }

  public void openGitHubLinkFromMPGram(final String url) {
    GH.open(
        this,
        new Runnable() {
          public void run() {
            MP.returnToCurrentChat();
          }
        });
    GH.openExternalUrl(url);
  }

  public void openTelegramLink(String url) {
    MP.openLinkFromHost(
        this,
        url,
        new Runnable() {
          public void run() {
            GH.returnToCurrent();
          }
        });
  }

  public String getHostLanguage() {
    return Lang.getCurrentLang();
  }

  public void openExternalUri(String uri) throws ConnectionNotFoundException {
    platformRequest(uri);
  }

  private void openMPGram() {
    MP.open(
        this,
        new Runnable() {
          public void run() {
            Display.getDisplay(MIDPlay.this).setCurrent(mainMenu);
          }
        });
  }

  private void openGitHubJ2ME() {
    GH.open(
        this,
        new Runnable() {
          public void run() {
            Display.getDisplay(MIDPlay.this).setCurrent(mainMenu);
          }
        });
  }

  private void loadIcons() {
    try {
      Configuration.loadIcons();
    } catch (IOException e) {
      showError(e.toString());
    }
  }

  protected void startApp() throws MIDletStateChangeException {
    if (initialized) {
      final Displayable restore = displayBeforePause;
      final boolean resumePlayback = resumePlaybackAfterPause;
      final long position = interruptedPlaybackPosition;
      displayBeforePause = null;
      resumePlaybackAfterPause = false;
      interruptedPlaybackPosition = 0L;
      Display.getDisplay(this).callSerially(
          new Runnable() {
            public void run() {
              if (restore != null) {
                Display.getDisplay(MIDPlay.this).setCurrent(restore);
              }
              if (resumePlayback && playerScreen != null) {
                PlayerGUI gui = playerScreen.getPlayerGUI();
                if (gui != null) {
                  gui.resumeAfterInterruption(position);
                }
              }
            }
          });
      return;
    }
    APP_VERSION = getAppProperty("MIDlet-Version");
    try {
      settingsManager.loadSettings();
    } catch (Throwable t) {
      // A corrupt settings record must never stop the MIDlet from starting.
    }
    try {
      loadIcons();
    } catch (Throwable t) {
    }
    mainMenu = new MainMenuScreen(navigator, menuManager, this);
    navigator.forward(mainMenu);
    try {
      maybeOfferResume();
    } catch (Throwable t) {
    }
    try {
      if (settingsManager.getCurrentMpgramAutoLogin() == Configuration.MPGRAM_AUTO_LOGIN_ON
          && MP.hasSavedSession()) {
        navigator.callSerially(
            new Runnable() {
              public void run() {
                MP.warmUp(MIDPlay.this);
              }
            });
      }
    } catch (Throwable t) {
    }
    try {
      autoCheckForUpdate();
    } catch (Throwable t) {
    }
    initialized = true;
  }

  private void maybeOfferResume() {
    LastSessionManager session = LastSessionManager.getInstance();
    if (!session.hasSession()) {
      return;
    }
    final String title = session.getTitle();
    final Tracks tracks = session.getTracks();
    final int index = session.getIndex();
    final long position = session.getPosition();
    session.clear();
    if (tracks == null || tracks.getTracks() == null || tracks.getTracks().length == 0) {
      return;
    }
    navigator.showConfirmationAlert(
        Lang.tr("session.resume_prompt"),
        new Runnable() {
          public void run() {
            PlayerNavHelper.playTrackFromList(title, tracks, index, position, navigator);
          }
        });
  }

  public void pauseApp() {
    try {
      displayBeforePause = Display.getDisplay(this).getCurrent();
      if (playerScreen != null) {
        PlayerGUI gui = playerScreen.getPlayerGUI();
        if (gui != null) {
          interruptedPlaybackPosition = gui.pauseForInterruption();
          resumePlaybackAfterPause = interruptedPlaybackPosition >= 0L;
        }
      }
    } catch (Exception e) {
    }
  }

  public void destroyApp(boolean unconditional) {
    try {
      cancelOperation();
    } catch (Exception e) {
    }
    try {
      if (playerScreen != null) {
        PlayerGUI gui = playerScreen.getPlayerGUI();
        if (gui != null) {
          Tracks current = gui.getCurrentTracks();
          if (current != null && current.getTracks() != null && current.getTracks().length > 0) {
            if (SettingsManager.getInstance().getCurrentSaveLastSession()
                == Configuration.SAVE_LAST_SESSION_ON) {
              try {
                LastSessionManager.getInstance()
                    .save(
                        playerScreen.getTitle(),
                        current,
                        gui.getCurrentIndex(),
                         Math.max(gui.getCurrentTime(), gui.getLastKnownMediaTime()));
              } catch (Exception e) {
              }
            }
          }
        }
        try {
          playerScreen.close();
        } catch (Exception e) {
        }
        if (gui != null) {
          gui.cleanup();
        }
        playerScreen = null;
      }
    } catch (Exception e) {
    }
    try {
      SettingsManager.getInstance().cleanup();
    } catch (Exception e) {
    }
    try {
      FavoritesManager.getInstance().close();
    } catch (Exception e) {
    }
    try {
      MenuManager.getInstance().close();
    } catch (Exception e) {
    }
    try {
      LastSessionManager.getInstance().close();
    } catch (Exception e) {
    }
    try {
      RecentManager.getInstance().close();
    } catch (Exception e) {
    }
    try {
      DownloadManager.getInstance().cleanup();
    } catch (Exception e) {
    }
  }

  public void exitApp() {
    showExitConfirmation();
  }

  public void checkForUpdate() {
    performUpdateCheck(true);
  }

  public void openNowPlaying() {
    goToPlayerScreen();
  }

  private void goToPlayerScreen() {
    PlayerScreen currentPlayerScreen = getPlayerScreen();
    if (currentPlayerScreen != null) {
      navigator.forward(currentPlayerScreen);
    } else {
      navigator.showAlert(Lang.tr(Configuration.PLAYER_STATUS_STOPPED), AlertType.INFO);
    }
  }

  private void goToSearchScreen() {
    SearchScreen searchScreen = new SearchScreen(navigator);
    navigator.forward(searchScreen);
  }

  private void goToDiscoverPlaylistsScreen() {
    navigator.showLoadingAlert(Lang.tr("status.loading"));
    MIDPlay.startOperation(
        JsonOperation.getHotPlaylists(
            new PlaylistsListForwarder(
                navigator, Lang.tr(Configuration.MENU_DISCOVER_PLAYLISTS), "status.no_data")));
  }

  private void goToAboutScreen() {
    Form f = new Form(Lang.tr(Configuration.MENU_ABOUT));

    try {
      Image appIcon = Image.createImage("/Icon.png");
      ImageItem iconItem = new ImageItem(null, appIcon, ImageItem.LAYOUT_LEFT, null);
      f.append(iconItem);
      f.append("\n");
    } catch (Exception e) {
      e.printStackTrace();
    }
    f.append(getAppProperty("MIDlet-Name") + "\n");
    f.append("Version " + APP_VERSION + "\n");
    f.append("Author: " + getAppProperty("MIDlet-Vendor") + "\n");
    f.append("Maintained by: Mr Lima\n");
    f.append("Contributors: " + "symbuzzer, GoldenDragon, Spajciuch, gtrxAC\n");
    f.append("This fork is based on MIDPlay 1.7.1\n");

    f.addCommand(Commands.back());
    f.setCommandListener(
        new CommandListener() {
          public void commandAction(Command c, Displayable d) {
            if (c == Commands.back()) {
              navigator.back();
            }
          }
        });
    navigator.forward(f);
  }

  private void goToSettingsScreen() {
    SettingsScreen settingsScreen =
        new SettingsScreen(
            navigator,
            new SettingsScreen.Listener() {
              public void onLanguageChanged(String selectedLang) {
                if (playerScreen != null) {
                  playerScreen.clearCommands();
                  Commands.refresh();
                  playerScreen.addCommands();
                } else {
                  Commands.refresh();
                }
                int index = mainMenu != null ? mainMenu.getSelectedIndex() : -1;
                mainMenu = new MainMenuScreen(navigator, menuManager, MIDPlay.this);
                navigator.clear();
                Display.getDisplay(MIDPlay.this).setCurrent(mainMenu);
                Utils.clampAndSelect(mainMenu, index);
              }

              public void onThemeChanged() {
                try {
                  Configuration.loadPlayerIcons();
                } catch (Exception e) {
                  showError(e.toString());
                }
              }

              public void onSettingsSaved() {
                navigator.showAlert(
                    Lang.tr("settings.status.saved"), AlertType.CONFIRMATION, mainMenu);
              }
            });
    navigator.forward(settingsScreen);
  }

  private void goToEqualizerScreen() {
    navigator.forward(new EqualizerScreen(navigator));
  }

  private void goToFavoritesScreen() {
    FavoritesScreen favoritesScreen = new FavoritesScreen(navigator);
    navigator.forward(favoritesScreen);
  }

  private void goToRecentScreen() {
    RecentListScreen recentScreen = new RecentListScreen(navigator);
    navigator.forward(recentScreen);
  }

  private void goToDownloadsScreen() {
    navigator.forward(new DownloadsScreen(navigator));
  }

  private void goToFileBrowserScreen() {
    navigator.forward(new FileBrowserScreen(navigator));
  }

  private void goToNotesScreen() {
    navigator.forward(new NotesScreen(navigator));
  }

  private void goToTasksScreen() {
    navigator.forward(new TasksScreen(navigator));
  }

  private void showExitConfirmation() {
    navigator.showConfirmationAlert(
        Lang.tr("confirm.exit"),
        new Runnable() {
          public void run() {
            notifyDestroyed();
          }
        });
  }

  private void showError(String message) {
    navigator.showAlert(message, AlertType.ERROR);
  }

  private void autoCheckForUpdate() {
    if (settingsManager.getCurrentAutoUpdate() == Configuration.AUTO_UPDATE_ENABLED) {
      performUpdateCheck(false);
    }
  }

  private void performUpdateCheck(final boolean isManual) {
    if (isManual) {
      navigator.showLoadingAlert(Lang.tr("status.loading"));
    }
    MIDPlay.startOperation(
        new CheckUpdateOperation(
            new CheckUpdateOperation.Listener() {
              public void onUpdateAvailable(String updateUrl) {
                if (isManual) {
                  navigator.dismissAlert();
                }
                showUpdateDialog(updateUrl);
              }

              public void onNoUpdateAvailable() {
                if (isManual) {
                  navigator.showAlert(Lang.tr("status.no_updates"), AlertType.CONFIRMATION);
                }
              }

              public void onError(Exception e) {
                showError(e.toString());
              }
            }));
  }

  private void showUpdateDialog(final String updateUrl) {
    navigator.showConfirmationAlert(
        Lang.tr("status.update_available"),
        new Runnable() {
          public void run() {
            try {
              platformRequest(updateUrl);
            } catch (ConnectionNotFoundException e) {
              navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
            }
          }
        });
  }
}