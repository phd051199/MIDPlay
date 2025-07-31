import java.util.Hashtable;

public class Lang {
  private static String currentLang = "en";
  private static Hashtable currentLangData = new Hashtable();
  private static boolean initialized = false;

  private static synchronized void loadLanguage(String langCode) {
    currentLangData.clear();

    if ("en".equals(langCode)) {
      loadEnglish();
    } else if ("he".equals(langCode)) {
      loadHebrew();
    } else if ("tr".equals(langCode)) {
      loadTurkish();
    } else if ("vi".equals(langCode)) {
      loadVietnamese();
    } else {
      loadEnglish();
    }
  }

  private static void loadEnglish() {
    currentLangData.put("lang", "en");
    currentLangData.put("action.back", "Back");
    currentLangData.put("action.cancel", "Cancel");
    currentLangData.put("action.exit", "Exit");
    currentLangData.put("action.no", "No");
    currentLangData.put("action.ok", "OK");
    currentLangData.put("action.save", "Save");
    currentLangData.put("action.yes", "Yes");
    currentLangData.put("app.name", "MIDPlay");
    currentLangData.put("confirm.exit", "Are you sure you want to exit?");
    currentLangData.put("favorites.add", "Add to Favorites");
    currentLangData.put("favorites.error.remove_failed", "Failed to remove favorite");
    currentLangData.put("favorites.error.save_failed", "Failed to save favorite");
    currentLangData.put("favorites.remove", "Remove from Favorites");
    currentLangData.put("favorites.status.added", "Added to favorites");
    currentLangData.put("favorites.status.already_exists", "Already in favorites");
    currentLangData.put("favorites.status.removed", "Removed from favorites");
    currentLangData.put("language.en", "English");
    currentLangData.put("language.tr", "Türkçe");
    currentLangData.put("language.vi", "Tiếng Việt");
    currentLangData.put("language.he", "עברית");
    currentLangData.put("media.album", "Album");
    currentLangData.put("media.playlist", "Playlist");
    currentLangData.put("media.track", "Track");
    currentLangData.put("media.tracks", "Tracks");
    currentLangData.put("menu.discover_playlists", "Discover Playlists");
    currentLangData.put("menu.favorites", "Favorites");
    currentLangData.put("menu.hot_playlists", "Hot Playlists");
    currentLangData.put("menu.main", "Main Menu");
    currentLangData.put("menu.now_playing", "Now Playing");
    currentLangData.put("menu.reorder", "Reorder Menu");
    currentLangData.put("menu.search", "Search");
    currentLangData.put("menu.chat", "AI Chat");
    currentLangData.put("menu.settings", "Settings");
    currentLangData.put("menu.about", "About");
    currentLangData.put("menu.visibility", "Menu Visibility");
    currentLangData.put("player.next", "Next");
    currentLangData.put("player.play", "Play");
    currentLangData.put("player.previous", "Previous");
    currentLangData.put("player.repeat", "Repeat");
    currentLangData.put("player.show_playlist", "Show Playlist");
    currentLangData.put("player.shuffle", "Shuffle");
    currentLangData.put("player.status.loading", "Loading...");
    currentLangData.put("player.status.paused", "Paused");
    currentLangData.put("player.status.playing", "Playing...");
    currentLangData.put("player.status.starting", "Starting...");
    currentLangData.put("player.status.stopped", "Stopped");
    currentLangData.put("player.status.stopping", "Stopping...");
    currentLangData.put("player.status.finished", "Finished");
    currentLangData.put("player.status.ready", "Ready");
    currentLangData.put("player.stop", "Stop");
    currentLangData.put("player.volume", "Volume");
    currentLangData.put("playlist.add_track", "Add to Playlist");
    currentLangData.put("playlist.create", "Create Playlist");
    currentLangData.put(
        "playlist.confirm.delete_with_tracks",
        "This playlist contains songs. Delete playlist and all songs?");
    currentLangData.put("playlist.error.add_track_failed", "Failed to add track to playlist");
    currentLangData.put("playlist.error.track_already_exists", "Track already exists in playlist");
    currentLangData.put("playlist.error.cannot_rename_system", "Cannot rename system playlists");
    currentLangData.put("playlist.error.create_failed", "Failed to create playlist");
    currentLangData.put("playlist.error.empty_name", "Playlist name cannot be empty");
    currentLangData.put(
        "playlist.error.remove_track_failed", "Failed to remove track from playlist");
    currentLangData.put("playlist.error.rename_failed", "Failed to rename playlist");
    currentLangData.put("playlist.name", "Playlist Name");
    currentLangData.put("playlist.rename", "Rename Playlist");
    currentLangData.put("playlist.select", "Select Playlist");
    currentLangData.put("playlist.status.created", "Playlist created successfully");
    currentLangData.put("playlist.status.no_custom", "No custom playlists available");
    currentLangData.put("playlist.status.renamed", "Playlist renamed successfully");
    currentLangData.put("playlist.status.track_added", "Track added to playlist successfully");
    currentLangData.put(
        "playlist.status.track_removed", "Track removed from playlist successfully");
    currentLangData.put("search.error.empty_keyword", "Please enter a keyword to search");
    currentLangData.put("search.placeholder", "Enter keywords to search...");
    currentLangData.put("search.results", "Search Results");
    currentLangData.put("search.status.no_results", "No results found");
    currentLangData.put("search.status.searching", "Searching for: {0}");
    currentLangData.put("search.type", "Search Type");
    currentLangData.put("settings.audio_quality", "Audio Quality");
    currentLangData.put("settings.auto_update", "Automatic Updates");
    currentLangData.put("settings.check_update", "Check for Updates");
    currentLangData.put("settings.player_method", "Player Method");
    currentLangData.put("settings.player_method_options.pass_inputstream", "Pass Input Stream");
    currentLangData.put("settings.player_method_options.pass_url", "Pass URL");
    currentLangData.put("settings.error.save_failed", "Failed to save settings");
    currentLangData.put("settings.language", "Language");
    currentLangData.put(
        "settings.reorder.instructions",
        "Select an item to move, then select the destination position");
    currentLangData.put("settings.reorder.saved", "Order saved");
    currentLangData.put("settings.service", "Service");
    currentLangData.put("settings.status.saved", "Settings saved successfully");
    currentLangData.put(
        "settings.visibility.instructions",
        "Select items to hide/show. [x] = show, [] = hide. Press Save to complete.");
    currentLangData.put("settings.visibility.saved", "Visibility settings saved");
    currentLangData.put("status.error", "An error occurred");
    currentLangData.put("status.loading", "Loading...");
    currentLangData.put("status.load_more", "Load more...");
    currentLangData.put("status.no_data", "No data available!");
    currentLangData.put("status.no_updates", "No updates available. You have the latest version.");
    currentLangData.put("status.update_available", "New version available. Update now?");
    currentLangData.put("symbol.arrow", "→");
    currentLangData.put("symbol.checked", "[x]");
    currentLangData.put("symbol.unchecked", "[  ]");
    currentLangData.put("time.error.invalid_duration", "Duration must be 1-999 minutes");
    currentLangData.put("time.error.invalid_format", "Invalid time format");
    currentLangData.put("time.error.invalid_hour", "Hour must be 0-23");
    currentLangData.put("time.error.invalid_minute", "Minute must be 0-59");
    currentLangData.put("time.hours", "Hours");
    currentLangData.put("time.input.minutes", "Enter duration (1-999 minutes)");
    currentLangData.put("time.input.time", "Enter time (24-hour format)");
    currentLangData.put("time.minutes", "Minutes");
    currentLangData.put("timer.action", "Timer Action");
    currentLangData.put("timer.actions.exit_app", "Exit Application");
    currentLangData.put("timer.actions.stop_playback", "Stop Playback");
    currentLangData.put("timer.cancel", "Cancel Timer");
    currentLangData.put(
        "timer.confirm.exit", "Timer will exit the application when expired. Continue?");
    currentLangData.put("timer.mode.absolute", "Absolute Time");
    currentLangData.put("timer.mode.countdown", "Countdown Mode");
    currentLangData.put("timer.set", "Set Timer");
    currentLangData.put("timer.sleep_timer", "Sleep Timer");
    currentLangData.put("timer.status.cancelled", "Sleep timer cancelled");
    currentLangData.put("timer.status.expired", "Sleep timer expired");
    currentLangData.put("timer.status.remaining", "Time Remaining: {0}");
    currentLangData.put("timer.status.set", "Sleep timer activated");
    currentLangData.put("chat.input", "Enter message");
    currentLangData.put("chat.welcome_message", "Hello! How can I help you today?");
    currentLangData.put("error.connection", "Connection error. Please try again.");
  }

  private static void loadHebrew() {
    currentLangData.put("lang", "he");
    currentLangData.put("action.back", "חזור");
    currentLangData.put("action.cancel", "ביטול");
    currentLangData.put("action.exit", "יציאה");
    currentLangData.put("action.no", "לא");
    currentLangData.put("action.ok", "אישור");
    currentLangData.put("action.save", "שמור");
    currentLangData.put("action.yes", "כן");
    currentLangData.put("app.name", "MIDPlay");
    currentLangData.put("confirm.exit", "אתה בטוח שאתה רוצה לצאת?");
    currentLangData.put("favorites.add", "הוסף למועדפים");
    currentLangData.put("favorites.error.remove_failed", "המחיקה מהמועדפים נכשלה");
    currentLangData.put("favorites.error.save_failed", "שמירת המועדפים נכשלה");
    currentLangData.put("favorites.remove", "מחק מהמועדפים");
    currentLangData.put("favorites.status.added", "נוסף למועדפים");
    currentLangData.put("favorites.status.already_exists", "כבר נמצא במועדפים");
    currentLangData.put("favorites.status.removed", "נמחק מהמועדפים");
    currentLangData.put("language.en", "English");
    currentLangData.put("language.tr", "Türkçe");
    currentLangData.put("language.vi", "Tiếng Việt");
    currentLangData.put("language.he", "עברית");
    currentLangData.put("media.album", "אלבום");
    currentLangData.put("media.playlist", "רשימת השמעה");
    currentLangData.put("media.track", "רצועה");
    currentLangData.put("media.tracks", "רצועות");
    currentLangData.put("menu.discover_playlists", "גלה רשימות השמעה");
    currentLangData.put("menu.favorites", "מועדפים");
    currentLangData.put("menu.hot_playlists", "רשימות השמעה חמות");
    currentLangData.put("menu.main", "תפריט ראשי");
    currentLangData.put("menu.now_playing", "מושמע כעת");
    currentLangData.put("menu.reorder", "סדר תפריט");
    currentLangData.put("menu.search", "חיפוש");
    currentLangData.put("menu.chat", "צ'אט בינה מלאכותית");
    currentLangData.put("menu.settings", "הגדרות");
    currentLangData.put("menu.about", "אודות");
    currentLangData.put("menu.visibility", "נראות התפריט");
    currentLangData.put("player.next", "הבא");
    currentLangData.put("player.play", "נגן");
    currentLangData.put("player.previous", "הקודם");
    currentLangData.put("player.repeat", "חזרה");
    currentLangData.put("player.show_playlist", "הצג רשימת השמעה");
    currentLangData.put("player.shuffle", "ערבוב");
    currentLangData.put("player.status.loading", "טוען...");
    currentLangData.put("player.status.paused", "בהשהייה");
    currentLangData.put("player.status.playing", "מושמע כעת...");
    currentLangData.put("player.status.starting", "מתחיל...");
    currentLangData.put("player.status.stopped", "מופסק");
    currentLangData.put("player.status.stopping", "עוצר...");
    currentLangData.put("player.status.finished", "סיים");
    currentLangData.put("player.status.ready", "מוכן");
    currentLangData.put("player.stop", "עצור");
    currentLangData.put("player.volume", "עוצמת שמע");
    currentLangData.put("playlist.add_track", "הוסף לרשימת השמעה");
    currentLangData.put("playlist.create", "צור רשימת השמעה");
    currentLangData.put(
        "playlist.confirm.delete_with_tracks",
        "רשימת ההשמעה הזו מכילה שירים. למחוק את רשימת ההשמעה ואת כל השירים?");
    currentLangData.put("playlist.error.add_track_failed", "הוספת הרצועה לרשימת ההשמעה נכשלה");
    currentLangData.put("playlist.error.track_already_exists", "הרצועה כבר נמצאת ברשימת ההשמעה");
    currentLangData.put(
        "playlist.error.cannot_rename_system", "לא ניתן לשנות את השם של רשימת השמעה של המערכת");
    currentLangData.put("playlist.error.create_failed", "נכשל ביצירת רשימת השמעה");
    currentLangData.put("playlist.error.empty_name", "שם רשימת ההשמעה לא יכול להיות ריק");
    currentLangData.put("playlist.error.remove_track_failed", "מחיקת הרצועה מרשימת ההשמעה נכשל");
    currentLangData.put("playlist.error.rename_failed", "שינוי השם של רשימת ההשמעה נכשל");
    currentLangData.put("playlist.name", "שם רשימת השמעה");
    currentLangData.put("playlist.rename", "שנה שם רשימת השמעה");
    currentLangData.put("playlist.select", "בחר רשימת השמעה");
    currentLangData.put("playlist.status.created", "רשימת השמעה נוצרה בהצלחה");
    currentLangData.put("playlist.status.no_custom", "אין רשימות השמעה מותאמות אישית זמינות");
    currentLangData.put("playlist.status.renamed", "שמה של רשימת ההשמעה שונה בהצלחה");
    currentLangData.put("playlist.status.track_added", "הרצועה נוספה לרשימת ההשמעה בהצלחה");
    currentLangData.put("playlist.status.track_removed", "הרצועה נמחקה מרשימת ההשמעה בהצלחה");
    currentLangData.put("search.error.empty_keyword", "אנא הכנס מונח לחיפוש");
    currentLangData.put("search.placeholder", "הכנס מונח לחיפוש...");
    currentLangData.put("search.results", "תוצאות חיפוש");
    currentLangData.put("search.status.no_results", "לא נמצאו תוצאות");
    currentLangData.put("search.status.searching", "מחפש: {0}");
    currentLangData.put("search.type", "סוג חיפוש");
    currentLangData.put("settings.audio_quality", "איכות שמע");
    currentLangData.put("settings.auto_update", "עדכון אוטמטי");
    currentLangData.put("settings.check_update", "בדוק עדכונים זמינים");
    currentLangData.put("settings.player_method", "שיטת נגינה");
    currentLangData.put("settings.player_method_options.pass_inputstream", "Pass Input Stream");
    currentLangData.put("settings.player_method_options.pass_url", "Pass URL");
    currentLangData.put("settings.error.save_failed", "שמירת ההגדרות נכשלה");
    currentLangData.put("settings.language", "Language/שפה");
    currentLangData.put("settings.reorder.instructions", "בחר פריט להעביר, ואז בחר במיקום היעד");
    currentLangData.put("settings.reorder.saved", "הסידור נשמר");
    currentLangData.put("settings.service", "שירות מוזיקה");
    currentLangData.put("settings.status.saved", "ההגדרות נשמרו בהצלחה");
    currentLangData.put(
        "settings.visibility.instructions",
        "בחר פריט להציג או להסתיר. [x] = להציג, [] = להסתיר. לחץ שמור כדי לסיים.");
    currentLangData.put("settings.visibility.saved", "הגדרות הנראות נשמרו");
    currentLangData.put("status.error", "התרחשה שגיאה");
    currentLangData.put("status.loading", "טוען...");
    currentLangData.put("status.load_more", "טען עוד...");
    currentLangData.put("status.no_data", "אין מידע זמין!");
    currentLangData.put("status.no_updates", "אין עדכונים זמינים. ברשותך הגרסה העדכנית.");
    currentLangData.put("status.update_available", "גרסה חדשה זמינה. לעדכן עכשיו?");
    currentLangData.put("symbol.arrow", "→");
    currentLangData.put("symbol.checked", "[x]");
    currentLangData.put("symbol.unchecked", "[  ]");
    currentLangData.put("time.error.invalid_duration", "משך זמן חייב להיות בין 1 ל- 999");
    currentLangData.put("time.error.invalid_format", "פורמט זמן שגוי");
    currentLangData.put("time.error.invalid_hour", "השעה חייבת להיות בין 0 ל- 23");
    currentLangData.put("time.error.invalid_minute", "הדקות חייבות להיות בין 0 ל-59");
    currentLangData.put("time.hours", "שעות");
    currentLangData.put("time.input.minutes", "הכנס אורך (בין דקה ל- 999)");
    currentLangData.put("time.input.time", "הכנס זמן (פורמט 24 שעות)");
    currentLangData.put("time.minutes", "דקות");
    currentLangData.put("timer.action", "פעולת קוצב זמן");
    currentLangData.put("timer.actions.exit_app", "סגור אפליקציה");
    currentLangData.put("timer.actions.stop_playback", "עצור השמעה");
    currentLangData.put("timer.cancel", "בטל קוצב זמן");
    currentLangData.put("timer.confirm.exit", "האפליקציה תסגר כאשר קוצב הזמן ייגמר. להמשיך?");
    currentLangData.put("timer.mode.absolute", "זמן מוחלט");
    currentLangData.put("timer.mode.countdown", "מצב ספירה לאחור");
    currentLangData.put("timer.set", "קבע קוצב זמן");
    currentLangData.put("timer.sleep_timer", "קוצב זמן לשינה");
    currentLangData.put("timer.status.cancelled", "קוצב זמן לשינה בוטל");
    currentLangData.put("timer.status.expired", "קוצב זמן לשינה סיים");
    currentLangData.put("timer.status.remaining", "הזמן שנותר: {0}");
    currentLangData.put("timer.status.set", "קוצב זמן לשינה הופעל");
    currentLangData.put("chat.input", "שלח הודעה");
    currentLangData.put("chat.welcome_message", "שלום! איך אני יכול לעזור היום?");
    currentLangData.put("error.connection", "בעיה בחיבור. אנא נסה שוב.");
  }

  private static void loadTurkish() {
    currentLangData.put("lang", "tr");
    currentLangData.put("action.back", "Geri");
    currentLangData.put("action.cancel", "İptal");
    currentLangData.put("action.exit", "Çık");
    currentLangData.put("action.no", "Hayır");
    currentLangData.put("action.ok", "Tamam");
    currentLangData.put("action.save", "Kaydet");
    currentLangData.put("action.yes", "Evet");
    currentLangData.put("app.name", "MIDPlay");
    currentLangData.put("confirm.exit", "Çıkmak istediğinizden emin misiniz?");
    currentLangData.put("favorites.add", "Favorilere Ekle");
    currentLangData.put("favorites.error.remove_failed", "Favori çıkarma hatası");
    currentLangData.put("favorites.error.save_failed", "Favori kaydetme hatası");
    currentLangData.put("favorites.remove", "Favorilerden Çıkar");
    currentLangData.put("favorites.status.added", "Favorilere eklendi");
    currentLangData.put("favorites.status.already_exists", "Zaten favorilerde");
    currentLangData.put("favorites.status.removed", "Favorilerden çıkarıldı");
    currentLangData.put("language.en", "English");
    currentLangData.put("language.tr", "Türkçe");
    currentLangData.put("language.vi", "Tiếng Việt");
    currentLangData.put("language.he", "עברית");
    currentLangData.put("media.album", "Albüm");
    currentLangData.put("media.playlist", "Çalma Listesi");
    currentLangData.put("media.track", "Şarkı");
    currentLangData.put("media.tracks", "Şarkılar");
    currentLangData.put("menu.discover_playlists", "Çalma Listelerini Keşfet");
    currentLangData.put("menu.favorites", "Favoriler");
    currentLangData.put("menu.hot_playlists", "Popüler Çalma Listeleri");
    currentLangData.put("menu.main", "Ana Menü");
    currentLangData.put("menu.now_playing", "Şimdi Çalıyor");
    currentLangData.put("menu.reorder", "Menüyü Yeniden Sırala");
    currentLangData.put("menu.search", "Arama");
    currentLangData.put("menu.chat", "AI Sohbeti");
    currentLangData.put("menu.settings", "Ayarlar");
    currentLangData.put("menu.about", "Hakkında");
    currentLangData.put("menu.visibility", "Menü Görünürlüğü");
    currentLangData.put("player.next", "Sonraki");
    currentLangData.put("player.play", "Çal");
    currentLangData.put("player.previous", "Önceki");
    currentLangData.put("player.repeat", "Tekrarla");
    currentLangData.put("player.show_playlist", "Çalma Listesini Göster");
    currentLangData.put("player.shuffle", "Karıştır");
    currentLangData.put("player.status.loading", "Yükleniyor...");
    currentLangData.put("player.status.paused", "Duraklatıldı");
    currentLangData.put("player.status.playing", "Çalıyor...");
    currentLangData.put("player.status.starting", "Başlatılıyor...");
    currentLangData.put("player.status.stopped", "Durduruldu");
    currentLangData.put("player.status.stopping", "Durduruluyor...");
    currentLangData.put("player.status.finished", "Tamamlandı");
    currentLangData.put("player.status.ready", "Hazır");
    currentLangData.put("player.stop", "Durdur");
    currentLangData.put("player.volume", "Ses");
    currentLangData.put("playlist.add_track", "Çalma Listesine Ekle");
    currentLangData.put("playlist.create", "Çalma Listesi Oluştur");
    currentLangData.put(
        "playlist.confirm.delete_with_tracks",
        "Bu çalma listesinde şarkılar var. Çalma listesini ve tüm şarkıları sil?");
    currentLangData.put("playlist.error.add_track_failed", "Şarkı çalma listesine eklenemedi");
    currentLangData.put(
        "playlist.error.track_already_exists", "Şarkı zaten çalma listesinde mevcut");
    currentLangData.put(
        "playlist.error.cannot_rename_system", "Sistem çalma listesi yeniden adlandırılamaz");
    currentLangData.put("playlist.error.create_failed", "Çalma listesi oluşturulamadı");
    currentLangData.put("playlist.error.empty_name", "Çalma listesi adı boş olamaz");
    currentLangData.put(
        "playlist.error.remove_track_failed", "Şarkı çalma listesinden çıkarılamadı");
    currentLangData.put("playlist.error.rename_failed", "Çalma listesi yeniden adlandırılamadı");
    currentLangData.put("playlist.name", "Çalma Listesi Adı");
    currentLangData.put("playlist.rename", "Çalma Listesini Yeniden Adlandır");
    currentLangData.put("playlist.select", "Çalma Listesi Seç");
    currentLangData.put("playlist.status.created", "Çalma listesi başarıyla oluşturuldu");
    currentLangData.put("playlist.status.no_custom", "Özel çalma listesi yok");
    currentLangData.put("playlist.status.renamed", "Çalma listesi başarıyla yeniden adlandırıldı");
    currentLangData.put("playlist.status.track_added", "Şarkı çalma listesine eklendi");
    currentLangData.put("playlist.status.track_removed", "Şarkı çalma listesinden çıkarıldı");
    currentLangData.put("search.error.empty_keyword", "Lütfen arama için bir anahtar kelime girin");
    currentLangData.put("search.placeholder", "Arama için anahtar kelime girin...");
    currentLangData.put("search.results", "Arama Sonuçları");
    currentLangData.put("search.status.no_results", "Sonuç bulunamadı");
    currentLangData.put("search.status.searching", "Aranan: {0}");
    currentLangData.put("search.type", "Arama Türü");
    currentLangData.put("settings.audio_quality", "Ses Kalitesi");
    currentLangData.put("settings.auto_update", "Otomatik Güncellemeler");
    currentLangData.put("settings.check_update", "Güncellemeleri Kontrol Et");
    currentLangData.put("settings.player_method", "Oynatıcı Yöntemi");
    currentLangData.put("settings.player_method_options.pass_inputstream", "Input Stream Geçir");
    currentLangData.put("settings.player_method_options.pass_url", "URL Geçir");
    currentLangData.put("settings.error.save_failed", "Ayarlar kaydedilemedi");
    currentLangData.put("settings.language", "Dil");
    currentLangData.put(
        "settings.reorder.instructions", "Taşınacak öğeyi seçin, sonra hedef konumu seçin");
    currentLangData.put("settings.reorder.saved", "Sıralama kaydedildi");
    currentLangData.put("settings.service", "Servis");
    currentLangData.put("settings.status.saved", "Ayarlar başarıyla kaydedildi");
    currentLangData.put(
        "settings.visibility.instructions",
        "Gizle/göster öğeleri seçin. [x] = göster, [] = gizle. Tamamlamak için Kaydet'e basın.");
    currentLangData.put("settings.visibility.saved", "Görünürlük ayarları kaydedildi");
    currentLangData.put("status.error", "Bir hata oluştu");
    currentLangData.put("status.loading", "Yükleniyor...");
    currentLangData.put("status.load_more", "Daha fazla yükle...");
    currentLangData.put("status.no_data", "Veri yok!");
    currentLangData.put("status.no_updates", "Güncelleme yok. En son sürümü kullanıyorsunuz.");
    currentLangData.put("status.update_available", "Yeni sürüm mevcut. Şimdi güncellensin mi?");
    currentLangData.put("symbol.arrow", "→");
    currentLangData.put("symbol.checked", "[x]");
    currentLangData.put("symbol.unchecked", "[  ]");
    currentLangData.put("time.error.invalid_duration", "Süre 1-999 dakika arasında olmalı");
    currentLangData.put("time.error.invalid_format", "Geçersiz zaman formatı");
    currentLangData.put("time.error.invalid_hour", "Saat 0-23 arasında olmalı");
    currentLangData.put("time.error.invalid_minute", "Dakika 0-59 arasında olmalı");
    currentLangData.put("time.hours", "Saat");
    currentLangData.put("time.input.minutes", "Süre girin (1-999 dakika)");
    currentLangData.put("time.input.time", "Zaman girin (24 saat formatı)");
    currentLangData.put("time.minutes", "Dakika");
    currentLangData.put("timer.action", "Zamanlayıcı Eylemi");
    currentLangData.put("timer.actions.exit_app", "Uygulamadan Çık");
    currentLangData.put("timer.actions.stop_playback", "Çalmayı Durdur");
    currentLangData.put("timer.cancel", "Zamanlayıcıyı İptal Et");
    currentLangData.put(
        "timer.confirm.exit", "Zamanlayıcı süresi dolduğunda uygulamadan çıkacak. Devam et?");
    currentLangData.put("timer.mode.absolute", "Mutlak Zaman");
    currentLangData.put("timer.mode.countdown", "Geri Sayım Modu");
    currentLangData.put("timer.set", "Zamanlayıcı Ayarla");
    currentLangData.put("timer.sleep_timer", "Uyku Zamanlayıcısı");
    currentLangData.put("timer.status.cancelled", "Uyku zamanlayıcısı iptal edildi");
    currentLangData.put("timer.status.expired", "Uyku zamanlayıcısı süresi doldu");
    currentLangData.put("timer.status.remaining", "Kalan Süre: {0}");
    currentLangData.put("timer.status.set", "Uyku zamanlayıcısı etkinleştirildi");
    currentLangData.put("chat.input", "Mesaj girin");
    currentLangData.put("chat.welcome_message", "Merhaba! Size nasıl yardımcı olabilirim?");
    currentLangData.put("error.connection", "Bağlantı hatası. Lütfen tekrar deneyin.");
  }

  private static void loadVietnamese() {
    currentLangData.put("lang", "vi");
    currentLangData.put("action.back", "Quay lại");
    currentLangData.put("action.cancel", "Hủy");
    currentLangData.put("action.exit", "Thoát");
    currentLangData.put("action.no", "Không");
    currentLangData.put("action.ok", "OK");
    currentLangData.put("action.save", "Lưu");
    currentLangData.put("action.yes", "Có");
    currentLangData.put("app.name", "MIDPlay");
    currentLangData.put("confirm.exit", "Bạn có chắc muốn thoát?");
    currentLangData.put("favorites.add", "Thêm vào yêu thích");
    currentLangData.put("favorites.error.remove_failed", "Lỗi xóa yêu thích");
    currentLangData.put("favorites.error.save_failed", "Lỗi lưu yêu thích");
    currentLangData.put("favorites.remove", "Xóa khỏi yêu thích");
    currentLangData.put("favorites.status.added", "Đã thêm vào yêu thích");
    currentLangData.put("favorites.status.already_exists", "Đã có trong yêu thích");
    currentLangData.put("favorites.status.removed", "Đã xóa khỏi yêu thích");
    currentLangData.put("language.en", "English");
    currentLangData.put("language.tr", "Türkçe");
    currentLangData.put("language.vi", "Tiếng Việt");
    currentLangData.put("language.he", "עברית");
    currentLangData.put("media.album", "Album");
    currentLangData.put("media.playlist", "Playlist");
    currentLangData.put("media.track", "Bài hát");
    currentLangData.put("media.tracks", "Bài hát");
    currentLangData.put("menu.discover_playlists", "Khám phá playlist");
    currentLangData.put("menu.favorites", "Yêu thích");
    currentLangData.put("menu.hot_playlists", "Playlist nổi bật");
    currentLangData.put("menu.main", "Menu chính");
    currentLangData.put("menu.now_playing", "Đang phát");
    currentLangData.put("menu.reorder", "Sắp xếp menu");
    currentLangData.put("menu.search", "Tìm kiếm");
    currentLangData.put("menu.chat", "Trò chuyện AI");
    currentLangData.put("menu.settings", "Cài đặt");
    currentLangData.put("menu.about", "Giới thiệu");
    currentLangData.put("menu.visibility", "Hiển thị menu");
    currentLangData.put("player.next", "Tiếp theo");
    currentLangData.put("player.play", "Phát");
    currentLangData.put("player.previous", "Trước đó");
    currentLangData.put("player.repeat", "Lặp lại");
    currentLangData.put("player.show_playlist", "Hiện playlist");
    currentLangData.put("player.shuffle", "Phát ngẫu nhiên");
    currentLangData.put("player.status.loading", "Đang tải...");
    currentLangData.put("player.status.paused", "Đã tạm dừng");
    currentLangData.put("player.status.playing", "Đang phát...");
    currentLangData.put("player.status.starting", "Đang khởi động...");
    currentLangData.put("player.status.stopped", "Đã dừng");
    currentLangData.put("player.status.stopping", "Đang dừng...");
    currentLangData.put("player.status.finished", "Đã hoàn thành");
    currentLangData.put("player.status.ready", "Sẵn sàng");
    currentLangData.put("player.stop", "Dừng");
    currentLangData.put("player.volume", "Âm lượng");
    currentLangData.put("playlist.add_track", "Thêm vào playlist");
    currentLangData.put("playlist.create", "Tạo playlist");
    currentLangData.put(
        "playlist.confirm.delete_with_tracks",
        "Playlist này có bài hát. Xóa playlist và tất cả bài hát?");
    currentLangData.put("playlist.error.add_track_failed", "Lỗi thêm bài hát vào playlist");
    currentLangData.put("playlist.error.track_already_exists", "Bài hát đã có trong playlist");
    currentLangData.put(
        "playlist.error.cannot_rename_system", "Không thể đổi tên playlist hệ thống");
    currentLangData.put("playlist.error.create_failed", "Lỗi tạo playlist");
    currentLangData.put("playlist.error.empty_name", "Tên playlist không được để trống");
    currentLangData.put("playlist.error.remove_track_failed", "Lỗi xóa bài hát khỏi playlist");
    currentLangData.put("playlist.error.rename_failed", "Lỗi đổi tên playlist");
    currentLangData.put("playlist.name", "Tên playlist");
    currentLangData.put("playlist.rename", "Đổi tên playlist");
    currentLangData.put("playlist.select", "Chọn playlist");
    currentLangData.put("playlist.status.created", "Tạo playlist thành công");
    currentLangData.put("playlist.status.no_custom", "Không có playlist tùy chỉnh");
    currentLangData.put("playlist.status.renamed", "Đổi tên playlist thành công");
    currentLangData.put("playlist.status.track_added", "Đã thêm bài hát vào playlist");
    currentLangData.put("playlist.status.track_removed", "Đã xóa bài hát khỏi playlist");
    currentLangData.put("search.error.empty_keyword", "Vui lòng nhập từ khóa");
    currentLangData.put("search.placeholder", "Nhập từ khóa để tìm kiếm...");
    currentLangData.put("search.results", "Kết quả tìm kiếm");
    currentLangData.put("search.status.no_results", "Không tìm thấy kết quả");
    currentLangData.put("search.status.searching", "Đang tìm: {0}");
    currentLangData.put("search.type", "Loại tìm kiếm");
    currentLangData.put("settings.audio_quality", "Chất lượng âm thanh");
    currentLangData.put("settings.auto_update", "Tự động cập nhật");
    currentLangData.put("settings.check_update", "Kiểm tra cập nhật");
    currentLangData.put("settings.player_method", "Phương thức phát");
    currentLangData.put("settings.player_method_options.pass_inputstream", "Truyền Input Stream");
    currentLangData.put("settings.player_method_options.pass_url", "Truyền URL");
    currentLangData.put("settings.error.save_failed", "Lỗi lưu cài đặt");
    currentLangData.put("settings.language", "Ngôn ngữ");
    currentLangData.put(
        "settings.reorder.instructions", "Chọn mục để di chuyển, sau đó chọn vị trí đích");
    currentLangData.put("settings.reorder.saved", "Đã lưu thứ tự");
    currentLangData.put("settings.service", "Dịch vụ");
    currentLangData.put("settings.status.saved", "Lưu cài đặt thành công");
    currentLangData.put(
        "settings.visibility.instructions",
        "Chọn mục để ẩn/hiện. [x] = hiện, [] = ẩn. Nhấn Lưu để hoàn tất.");
    currentLangData.put("settings.visibility.saved", "Đã lưu cài đặt hiển thị");
    currentLangData.put("status.error", "Đã xảy ra lỗi");
    currentLangData.put("status.loading", "Đang tải...");
    currentLangData.put("status.load_more", "Tải thêm...");
    currentLangData.put("status.no_data", "Không có dữ liệu!");
    currentLangData.put(
        "status.no_updates", "Không có cập nhật. Bạn đang dùng phiên bản mới nhất.");
    currentLangData.put("status.update_available", "Có phiên bản mới. Cập nhật ngay?");
    currentLangData.put("symbol.arrow", "→");
    currentLangData.put("symbol.checked", "[x]");
    currentLangData.put("symbol.unchecked", "[  ]");
    currentLangData.put("time.error.invalid_duration", "Thời lượng phải từ 1-999 phút");
    currentLangData.put("time.error.invalid_format", "Định dạng thời gian không hợp lệ");
    currentLangData.put("time.error.invalid_hour", "Giờ phải từ 0-23");
    currentLangData.put("time.error.invalid_minute", "Phút phải từ 0-59");
    currentLangData.put("time.hours", "Giờ");
    currentLangData.put("time.input.minutes", "Nhập thời lượng (1-999 phút)");
    currentLangData.put("time.input.time", "Nhập thời gian (định dạng 24 giờ)");
    currentLangData.put("time.minutes", "Phút");
    currentLangData.put("timer.action", "Hành động khi hết giờ");
    currentLangData.put("timer.actions.exit_app", "Thoát ứng dụng");
    currentLangData.put("timer.actions.stop_playback", "Dừng phát nhạc");
    currentLangData.put("timer.cancel", "Hủy hẹn giờ");
    currentLangData.put(
        "timer.confirm.exit", "Hẹn giờ sẽ thoát ứng dụng khi hết thời gian. Tiếp tục?");
    currentLangData.put("timer.mode.absolute", "Thời gian tuyệt đối");
    currentLangData.put("timer.mode.countdown", "Chế độ đếm ngược");
    currentLangData.put("timer.set", "Đặt hẹn giờ");
    currentLangData.put("timer.sleep_timer", "Hẹn giờ tắt");
    currentLangData.put("timer.status.cancelled", "Đã hủy hẹn giờ tắt");
    currentLangData.put("timer.status.expired", "Hẹn giờ tắt đã hết thời gian");
    currentLangData.put("timer.status.remaining", "Thời gian còn lại: {0}");
    currentLangData.put("timer.status.set", "Đã kích hoạt hẹn giờ tắt");
    currentLangData.put("chat.input", "Nhập tin nhắn");
    currentLangData.put("chat.welcome_message", "Xin chào! Tôi có thể giúp gì cho bạn?");
    currentLangData.put("error.connection", "Lỗi kết nối. Vui lòng thử lại.");
  }

  public static synchronized void setLang(String code) {
    if (!code.equals(currentLang)) {
      currentLang = code;
      loadLanguage(code);
      initialized = true;
    }
  }

  public static String getCurrentLang() {
    return currentLang;
  }

  public static String[] getAvailableLanguages() {
    return new String[] {"en", "he", "tr", "vi"};
  }

  public static String tr(String key) {
    if (!initialized) {
      loadLanguage(currentLang);
      initialized = true;
    }
    String value = (String) currentLangData.get(key);
    return value == null ? key : value;
  }

  public static String tr(String key, String arg0) {
    String template = tr(key);
    template = MIDPlay.replace(template, "{0}", arg0);
    return template;
  }

  public static String tr(String key, String arg0, String arg1) {
    String template = tr(key);
    template = MIDPlay.replace(template, "{0}", arg0);
    template = MIDPlay.replace(template, "{1}", arg1);
    return template;
  }

  private Lang() {}
}
