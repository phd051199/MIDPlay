import java.util.Hashtable;

public class Lang {
  private static String c = "en";
  private static Hashtable d;
  private static boolean i = false;

  private static final String[] KEYS = {
    "lang", "action.back", "action.cancel", "action.exit", "action.no", "action.ok", "action.save",
        "action.yes", "app.name", "confirm.exit",
    "favorites.add", "favorites.error.remove_failed", "favorites.error.save_failed",
        "favorites.remove", "favorites.status.added", "favorites.status.already_exists",
        "favorites.status.removed", "language.en", "language.tr", "language.vi",
    "language.he", "media.album", "media.playlist", "media.track", "media.tracks",
        "menu.discover_playlists", "menu.favorites", "menu.hot_playlists", "menu.main",
        "menu.now_playing",
    "menu.reorder", "menu.search", "menu.chat", "menu.settings", "menu.about", "menu.visibility",
        "player.next", "player.play", "player.previous", "player.repeat",
    "player.show_playlist", "player.shuffle", "player.status.loading", "player.status.paused",
        "player.status.playing", "player.status.starting", "player.status.stopped",
        "player.status.stopping", "player.status.finished", "player.status.ready",
    "player.stop", "player.volume", "playlist.add_track", "playlist.create",
        "playlist.confirm.delete_with_tracks", "playlist.error.add_track_failed",
        "playlist.error.track_already_exists", "playlist.error.cannot_rename_system",
        "playlist.error.create_failed", "playlist.error.empty_name",
    "playlist.error.remove_track_failed", "playlist.error.rename_failed", "playlist.name",
        "playlist.rename", "playlist.select", "playlist.status.created",
        "playlist.status.no_custom", "playlist.status.renamed", "playlist.status.track_added",
        "playlist.status.track_removed",
    "search.error.empty_keyword", "search.placeholder", "search.results",
        "search.status.no_results", "search.status.searching", "search.type",
        "settings.audio_quality", "settings.auto_update", "settings.check_update",
        "settings.player_method",
    "settings.player_method_options.pass_inputstream", "settings.player_method_options.pass_url",
        "settings.error.save_failed", "settings.language", "settings.reorder.instructions",
        "settings.reorder.saved", "settings.service", "settings.status.saved",
        "settings.visibility.instructions", "settings.visibility.saved",
    "status.error", "status.loading", "status.load_more", "status.no_data", "status.no_updates",
        "status.update_available", "symbol.arrow", "symbol.checked", "symbol.unchecked",
        "time.error.invalid_duration",
    "time.error.invalid_format", "time.error.invalid_hour", "time.error.invalid_minute",
        "time.hours", "time.input.minutes", "time.input.time", "time.minutes", "timer.action",
        "timer.actions.exit_app", "timer.actions.stop_playback",
    "timer.cancel", "timer.confirm.exit", "timer.mode.absolute", "timer.mode.countdown",
        "timer.set", "timer.sleep_timer", "timer.status.cancelled", "timer.status.expired",
        "timer.status.remaining", "timer.status.set",
    "chat.input", "chat.welcome_message", "error.connection"
  };

  private static final String[] EN_VALS = {
    "en", "Back", "Cancel", "Exit", "No",
    "OK", "Save", "Yes", "MIDPlay", "Are you sure you want to exit?",
    "Add to Favorites", "Failed to remove favorite", "Failed to save favorite",
        "Remove from Favorites", "Added to favorites",
    "Already in favorites", "Removed from favorites", "English", "Türkçe", "Tiếng Việt",
    "עברית", "Album", "Playlist", "Track", "Tracks",
    "Discover Playlists", "Favorites", "Hot Playlists", "Main Menu", "Now Playing",
    "Reorder Menu", "Search", "AI Chat", "Settings", "About",
    "Menu Visibility", "Next", "Play", "Previous", "Repeat",
    "Show Playlist", "Shuffle", "Loading...", "Paused", "Playing...",
    "Starting...", "Stopped", "Stopping...", "Finished", "Ready",
    "Stop", "Volume", "Add to Playlist", "Create Playlist",
        "This playlist contains songs. Delete playlist and all songs?",
    "Failed to add track to playlist", "Track already exists in playlist",
        "Cannot rename system playlists", "Failed to create playlist",
        "Playlist name cannot be empty",
    "Failed to remove track from playlist", "Failed to rename playlist", "Playlist Name",
        "Rename Playlist", "Select Playlist",
    "Playlist created successfully", "No custom playlists available",
        "Playlist renamed successfully", "Track added to playlist successfully",
        "Track removed from playlist successfully",
    "Please enter a keyword to search", "Enter keywords to search...", "Search Results",
        "No results found", "Searching for: {0}",
    "Search Type", "Audio Quality", "Automatic Updates", "Check for Updates", "Player Method",
    "Pass Input Stream", "Pass URL", "Failed to save settings", "Language",
        "Select an item to move, then select the destination position",
    "Order saved", "Service", "Settings saved successfully",
        "Select items to hide/show. [x] = show, [] = hide. Press Save to complete.",
        "Visibility settings saved",
    "An error occurred", "Loading...", "Load more...", "No data available!",
        "No updates available. You have the latest version.",
    "New version available. Update now?", "→", "[x]", "[  ]", "Duration must be 1-999 minutes",
    "Invalid time format", "Hour must be 0-23", "Minute must be 0-59", "Hours",
        "Enter duration (1-999 minutes)",
    "Enter time (24-hour format)", "Minutes", "Timer Action", "Exit Application", "Stop Playback",
    "Cancel Timer", "Timer will exit the application when expired. Continue?", "Absolute Time",
        "Countdown Mode", "Set Timer",
    "Sleep Timer", "Sleep timer cancelled", "Sleep timer expired", "Time Remaining: {0}",
        "Sleep timer activated",
    "Enter message", "Hello! How can I help you today?", "Connection error. Please try again."
  };

  private static final String[] HE_VALS = {
    "he", "חזור", "ביטול", "יציאה", "לא",
    "אישור", "שמור", "כן", "MIDPlay", "אתה בטוח שאתה רוצה לצאת?",
    "הוסף למועדפים", "המחיקה מהמועדפים נכשלה", "שמירת המועדפים נכשלה", "מחק מהמועדפים",
        "נוסף למועדפים",
    "כבר נמצא במועדפים", "נמחק מהמועדפים", "English", "Türkçe", "Tiếng Việt",
    "עברית", "אלבום", "רשימת השמעה", "רצועה", "רצועות",
    "גלה רשימות השמעה", "מועדפים", "רשימות השמעה חמות", "תפריט ראשי", "מושמע כעת",
    "סדר תפריט", "חיפוש", "צ'אט בינה מלאכותית", "הגדרות", "אודות",
    "נראות התפריט", "הבא", "נגן", "הקודם", "חזרה",
    "הצג רשימת השמעה", "ערבוב", "טוען...", "בהשהייה", "מושמע כעת...",
    "מתחיל...", "מופסק", "עוצר...", "סיים", "מוכן",
    "עצור", "עוצמת שמע", "הוסף לרשימת השמעה", "צור רשימת השמעה",
        "רשימת ההשמעה הזו מכילה שירים. למחוק את רשימת ההשמעה ואת כל השירים?",
    "הוספת הרצועה לרשימת ההשמעה נכשלה", "הרצועה כבר נמצאת ברשימת ההשמעה",
        "לא ניתן לשנות את השם של רשימת השמעה של המערכת", "נכשל ביצירת רשימת השמעה",
        "שם רשימת ההשמעה לא יכול להיות ריק",
    "מחיקת הרצועה מרשימת ההשמעה נכשל", "שינוי השם של רשימת ההשמעה נכשל", "שם רשימת השמעה",
        "שנה שם רשימת השמעה", "בחר רשימת השמעה",
    "רשימת השמעה נוצרה בהצלחה", "אין רשימות השמעה מותאמות אישית זמינות",
        "שמה של רשימת ההשמעה שונה בהצלחה", "הרצועה נוספה לרשימת ההשמעה בהצלחה",
        "הרצועה נמחקה מרשימת ההשמעה בהצלחה",
    "אנא הכנס מונח לחיפוש", "הכנס מונח לחיפוש...", "תוצאות חיפוש", "לא נמצאו תוצאות", "מחפש: {0}",
    "סוג חיפוש", "איכות שמע", "עדכון אוטמטי", "בדוק עדכונים זמינים", "שיטת נגינה",
    "Pass Input Stream", "Pass URL", "שמירת ההגדרות נכשלה", "Language/שפה",
        "בחר פריט להעביר, ואז בחר במיקום היעד",
    "הסידור נשמר", "שירות מוזיקה", "ההגדרות נשמרו בהצלחה",
        "בחר פריט להציג או להסתיר. [x] = להציג, [] = להסתיר. לחץ שמור כדי לסיים.",
        "הגדרות הנראות נשמרו",
    "התרחשה שגיאה", "טוען...", "טען עוד...", "אין מידע זמין!",
        "אין עדכונים זמינים. ברשותך הגרסה העדכנית.",
    "גרסה חדשה זמינה. לעדכן עכשיו?", "→", "[x]", "[  ]", "משך זמן חייב להיות בין 1 ל- 999",
    "פורמט זמן שגוי", "השעה חייבת להיות בין 0 ל- 23", "הדקות חייבות להיות בין 0 ל-59", "שעות",
        "הכנס אורך (בין דקה ל- 999)",
    "הכנס זמן (פורמט 24 שעות)", "דקות", "פעולת קוצב זמן", "סגור אפליקציה", "עצור השמעה",
    "בטל קוצב זמן", "האפליקציה תסגר כאשר קוצב הזמן ייגמר. להמשיך?", "זמן מוחלט", "מצב ספירה לאחור",
        "קבע קוצב זמן",
    "קוצב זמן לשינה", "קוצב זמן לשינה בוטל", "קוצב זמן לשינה סיים", "הזמן שנותר: {0}",
        "קוצב זמן לשינה הופעל",
    "שלח הודעה", "שלום! איך אני יכול לעזור היום?", "בעיה בחיבור. אנא נסה שוב."
  };

  private static final String[] TR_VALS = {
    "tr", "Geri", "İptal", "Çık", "Hayır",
    "Tamam", "Kaydet", "Evet", "MIDPlay", "Çıkmak istediğinizden emin misiniz?",
    "Favorilere Ekle", "Favori çıkarma hatası", "Favori kaydetme hatası", "Favorilerden Çıkar",
        "Favorilere eklendi",
    "Zaten favorilerde", "Favorilerden çıkarıldı", "English", "Türkçe", "Tiếng Việt",
    "עברית", "Albüm", "Çalma Listesi", "Şarkı", "Şarkılar",
    "Çalma Listelerini Keşfet", "Favoriler", "Popüler Çalma Listeleri", "Ana Menü", "Şimdi Çalıyor",
    "Menüyü Yeniden Sırala", "Arama", "AI Sohbeti", "Ayarlar", "Hakkında",
    "Menü Görünürlüğü", "Sonraki", "Çal", "Önceki", "Tekrarla",
    "Çalma Listesini Göster", "Karıştır", "Yükleniyor...", "Duraklatıldı", "Çalıyor...",
    "Başlatılıyor...", "Durduruldu", "Durduruluyor...", "Tamamlandı", "Hazır",
    "Durdur", "Ses", "Çalma Listesine Ekle", "Çalma Listesi Oluştur",
        "Bu çalma listesinde şarkılar var. Çalma listesini ve tüm şarkıları sil?",
    "Şarkı çalma listesine eklenemedi", "Şarkı zaten çalma listesinde mevcut",
        "Sistem çalma listesi yeniden adlandırılamaz", "Çalma listesi oluşturulamadı",
        "Çalma listesi adı boş olamaz",
    "Şarkı çalma listesinden çıkarılamadı", "Çalma listesi yeniden adlandırılamadı",
        "Çalma Listesi Adı", "Çalma Listesini Yeniden Adlandır", "Çalma Listesi Seç",
    "Çalma listesi başarıyla oluşturuldu", "Özel çalma listesi yok",
        "Çalma listesi başarıyla yeniden adlandırıldı", "Şarkı çalma listesine eklendi",
        "Şarkı çalma listesinden çıkarıldı",
    "Lütfen arama için bir anahtar kelime girin", "Arama için anahtar kelime girin...",
        "Arama Sonuçları", "Sonuç bulunamadı", "Aranan: {0}",
    "Arama Türü", "Ses Kalitesi", "Otomatik Güncellemeler", "Güncellemeleri Kontrol Et",
        "Oynatıcı Yöntemi",
    "Input Stream Geçir", "URL Geçir", "Ayarlar kaydedilemedi", "Dil",
        "Taşınacak öğeyi seçin, sonra hedef konumu seçin",
    "Sıralama kaydedildi", "Servis", "Ayarlar başarıyla kaydedildi",
        "Gizle/göster öğeleri seçin. [x] = göster, [] = gizle. Tamamlamak için Kaydet'e basın.",
        "Görünürlük ayarları kaydedildi",
    "Bir hata oluştu", "Yükleniyor...", "Daha fazla yükle...", "Veri yok!",
        "Güncelleme yok. En son sürümü kullanıyorsunuz.",
    "Yeni sürüm mevcut. Şimdi güncellensin mi?", "→", "[x]", "[  ]",
        "Süre 1-999 dakika arasında olmalı",
    "Geçersiz zaman formatı", "Saat 0-23 arasında olmalı", "Dakika 0-59 arasında olmalı", "Saat",
        "Süre girin (1-999 dakika)",
    "Zaman girin (24 saat formatı)", "Dakika", "Zamanlayıcı Eylemi", "Uygulamadan Çık",
        "Çalmayı Durdur",
    "Zamanlayıcıyı İptal Et", "Zamanlayıcı süresi dolduğunda uygulamadan çıkacak. Devam et?",
        "Mutlak Zaman", "Geri Sayım Modu", "Zamanlayıcı Ayarla",
    "Uyku Zamanlayıcısı", "Uyku zamanlayıcısı iptal edildi", "Uyku zamanlayıcısı süresi doldu",
        "Kalan Süre: {0}", "Uyku zamanlayıcısı etkinleştirildi",
    "Mesaj girin", "Merhaba! Size nasıl yardımcı olabilirim?",
        "Bağlantı hatası. Lütfen tekrar deneyin."
  };

  private static final String[] VI_VALS = {
    "vi", "Quay lại", "Hủy", "Thoát", "Không",
    "OK", "Lưu", "Có", "MIDPlay", "Bạn có chắc muốn thoát?",
    "Thêm vào yêu thích", "Lỗi xóa yêu thích", "Lỗi lưu yêu thích", "Xóa khỏi yêu thích",
        "Đã thêm vào yêu thích",
    "Đã có trong yêu thích", "Đã xóa khỏi yêu thích", "English", "Türkçe", "Tiếng Việt",
    "עברית", "Album", "Playlist", "Bài hát", "Bài hát",
    "Khám phá playlist", "Yêu thích", "Playlist nổi bật", "Menu chính", "Đang phát",
    "Sắp xếp menu", "Tìm kiếm", "Trò chuyện AI", "Cài đặt", "Giới thiệu",
    "Hiển thị menu", "Tiếp theo", "Phát", "Trước đó", "Lặp lại",
    "Hiện playlist", "Phát ngẫu nhiên", "Đang tải...", "Đã tạm dừng", "Đang phát...",
    "Đang khởi động...", "Đã dừng", "Đang dừng...", "Đã hoàn thành", "Sẵn sàng",
    "Dừng", "Âm lượng", "Thêm vào playlist", "Tạo playlist",
        "Playlist này có bài hát. Xóa playlist và tất cả bài hát?",
    "Lỗi thêm bài hát vào playlist", "Bài hát đã có trong playlist",
        "Không thể đổi tên playlist hệ thống", "Lỗi tạo playlist",
        "Tên playlist không được để trống",
    "Lỗi xóa bài hát khỏi playlist", "Lỗi đổi tên playlist", "Tên playlist", "Đổi tên playlist",
        "Chọn playlist",
    "Tạo playlist thành công", "Không có playlist tùy chỉnh", "Đổi tên playlist thành công",
        "Đã thêm bài hát vào playlist", "Đã xóa bài hát khỏi playlist",
    "Vui lòng nhập từ khóa", "Nhập từ khóa để tìm kiếm...", "Kết quả tìm kiếm",
        "Không tìm thấy kết quả", "Đang tìm: {0}",
    "Loại tìm kiếm", "Chất lượng âm thanh", "Tự động cập nhật", "Kiểm tra cập nhật",
        "Phương thức phát",
    "Truyền Input Stream", "Truyền URL", "Lỗi lưu cài đặt", "Ngôn ngữ",
        "Chọn mục để di chuyển, sau đó chọn vị trí đích",
    "Đã lưu thứ tự", "Dịch vụ", "Lưu cài đặt thành công",
        "Chọn mục để ẩn/hiện. [x] = hiện, [] = ẩn. Nhấn Lưu để hoàn tất.",
        "Đã lưu cài đặt hiển thị",
    "Đã xảy ra lỗi", "Đang tải...", "Tải thêm...", "Không có dữ liệu!",
        "Không có cập nhật. Bạn đang dùng phiên bản mới nhất.",
    "Có phiên bản mới. Cập nhật ngay?", "→", "[x]", "[  ]", "Thời lượng phải từ 1-999 phút",
    "Định dạng thời gian không hợp lệ", "Giờ phải từ 0-23", "Phút phải từ 0-59", "Giờ",
        "Nhập thời lượng (1-999 phút)",
    "Nhập thời gian (định dạng 24 giờ)", "Phút", "Hành động khi hết giờ", "Thoát ứng dụng",
        "Dừng phát nhạc",
    "Hủy hẹn giờ", "Hẹn giờ sẽ thoát ứng dụng khi hết thời gian. Tiếp tục?", "Thời gian tuyệt đối",
        "Chế độ đếm ngược", "Đặt hẹn giờ",
    "Hẹn giờ tắt", "Đã hủy hẹn giờ tắt", "Hẹn giờ tắt đã hết thời gian", "Thời gian còn lại: {0}",
        "Đã kích hoạt hẹn giờ tắt",
    "Nhập tin nhắn", "Xin chào! Tôi có thể giúp gì cho bạn?", "Lỗi kết nối. Vui lòng thử lại."
  };

  private static void l(String code) {
    if (d == null) d = new Hashtable();
    else d.clear();
    String[] vals = EN_VALS;
    if ("he".equals(code)) vals = HE_VALS;
    if ("tr".equals(code)) vals = TR_VALS;
    if ("vi".equals(code)) vals = VI_VALS;
    for (int j = 0; j < KEYS.length; j++) {
      d.put(KEYS[j], vals[j]);
    }
  }

  public static void setLang(String code) {
    if (!code.equals(c)) {
      c = code;
      l(code);
      i = true;
    }
  }

  public static String getCurrentLang() {
    return c;
  }

  public static String[] getAvailableLanguages() {
    return new String[] {"en", "he", "tr", "vi"};
  }

  public static String tr(String k) {
    if (!i) {
      l(c);
      i = true;
    }
    String v = (String) d.get(k);
    return v != null ? v : k;
  }

  public static String tr(String k, String a) {
    return MIDPlay.replace(tr(k), "{0}", a);
  }

  public static String tr(String k, String a, String b) {
    String t = tr(k);
    return MIDPlay.replace(MIDPlay.replace(t, "{0}", a), "{1}", b);
  }

  private Lang() {}
}
