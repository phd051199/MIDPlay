import java.util.Hashtable;

public class Lang {
  private static String currentLang = "en";
  private static final Hashtable langEn = new Hashtable();
  private static final Hashtable langTr = new Hashtable();
  private static final Hashtable langVi = new Hashtable();

  private static final Hashtable[] langs;

  static {
    // Language: en
    langEn.put("lang", "en");
    langEn.put("action.back", "Back");
    langEn.put("action.cancel", "Cancel");
    langEn.put("action.exit", "Exit");
    langEn.put("action.no", "No");
    langEn.put("action.ok", "OK");
    langEn.put("action.save", "Save");
    langEn.put("action.yes", "Yes");
    langEn.put("app.name", "MIDPlay");
    langEn.put("confirm.exit", "Are you sure you want to exit?");
    langEn.put("favorites.add", "Add to Favorites");
    langEn.put("favorites.error.remove_failed", "Failed to remove favorite");
    langEn.put("favorites.error.save_failed", "Failed to save favorite");
    langEn.put("favorites.remove", "Remove from Favorites");
    langEn.put("favorites.status.added", "Added to favorites");
    langEn.put("favorites.status.already_exists", "Already in favorites");
    langEn.put("favorites.status.removed", "Removed from favorites");
    langEn.put("language.en", "English");
    langEn.put("language.tr", "Türkçe");
    langEn.put("language.vi", "Tiếng Việt");
    langEn.put("media.album", "Album");
    langEn.put("media.playlist", "Playlist");
    langEn.put("media.track", "Track");
    langEn.put("media.tracks", "Tracks");
    langEn.put("menu.discover_playlists", "Discover Playlists");
    langEn.put("menu.favorites", "Favorites");
    langEn.put("menu.hot_playlists", "Hot Playlists");
    langEn.put("menu.main", "Main Menu");
    langEn.put("menu.now_playing", "Now Playing");
    langEn.put("menu.reorder", "Reorder Menu");
    langEn.put("menu.search", "Search");
    langEn.put("menu.chat", "AI Chat");
    langEn.put("menu.settings", "Settings");
    langEn.put("menu.about", "About");
    langEn.put("menu.visibility", "Menu Visibility");
    langEn.put("player.next", "Next");
    langEn.put("player.play", "Play");
    langEn.put("player.previous", "Previous");
    langEn.put("player.repeat", "Repeat");
    langEn.put("player.show_playlist", "Show Playlist");
    langEn.put("player.shuffle", "Shuffle");
    langEn.put("player.status.loading", "Loading...");
    langEn.put("player.status.paused", "Paused");
    langEn.put("player.status.playing", "Playing...");
    langEn.put("player.status.starting", "Starting...");
    langEn.put("player.status.stopped", "Stopped");
    langEn.put("player.status.stopping", "Stopping...");
    langEn.put("player.status.finished", "Finished");
    langEn.put("player.status.ready", "Ready");
    langEn.put("player.stop", "Stop");
    langEn.put("player.volume", "Volume");
    langEn.put("playlist.add_track", "Add to Playlist");
    langEn.put("playlist.create", "Create Playlist");
    langEn.put(
        "playlist.confirm.delete_with_tracks",
        "This playlist contains songs. Delete playlist and all songs?");
    langEn.put("playlist.error.add_track_failed", "Failed to add track to playlist");
    langEn.put("playlist.error.track_already_exists", "Track already exists in playlist");
    langEn.put("playlist.error.cannot_rename_system", "Cannot rename system playlists");
    langEn.put("playlist.error.create_failed", "Failed to create playlist");
    langEn.put("playlist.error.empty_name", "Playlist name cannot be empty");
    langEn.put("playlist.error.remove_track_failed", "Failed to remove track from playlist");
    langEn.put("playlist.error.rename_failed", "Failed to rename playlist");
    langEn.put("playlist.name", "Playlist Name");
    langEn.put("playlist.rename", "Rename Playlist");
    langEn.put("playlist.select", "Select Playlist");
    langEn.put("playlist.status.created", "Playlist created successfully");
    langEn.put("playlist.status.no_custom", "No custom playlists available");
    langEn.put("playlist.status.renamed", "Playlist renamed successfully");
    langEn.put("playlist.status.track_added", "Track added to playlist successfully");
    langEn.put("playlist.status.track_removed", "Track removed from playlist successfully");
    langEn.put("search.error.empty_keyword", "Please enter a keyword to search");
    langEn.put("search.placeholder", "Enter keywords to search...");
    langEn.put("search.results", "Search Results");
    langEn.put("search.status.no_results", "No results found");
    langEn.put("search.status.searching", "Searching for: {0}");
    langEn.put("search.type", "Search Type");
    langEn.put("settings.audio_quality", "Audio Quality");
    langEn.put("settings.auto_update", "Automatic Updates");
    langEn.put("settings.check_update", "Check for Updates");
    langEn.put("settings.player_method", "Player Method");
    langEn.put("settings.force_pass_connection", "Force pass connection");
    langEn.put("settings.error.save_failed", "Failed to save settings");
    langEn.put("settings.language", "Language");
    langEn.put(
        "settings.reorder.instructions",
        "Select an item to move, then select the destination position");
    langEn.put("settings.reorder.saved", "Order saved");
    langEn.put("settings.service", "Service");
    langEn.put("settings.status.saved", "Settings saved successfully");
    langEn.put(
        "settings.visibility.instructions",
        "Select items to hide/show. [x] = show, [] = hide. Press Save to complete.");
    langEn.put("settings.visibility.saved", "Visibility settings saved");
    langEn.put("status.error", "An error occurred");
    langEn.put("status.loading", "Loading...");
    langEn.put("status.load_more", "Load more...");
    langEn.put("status.no_data", "No data available!");
    langEn.put("status.no_updates", "No updates available. You have the latest version.");
    langEn.put("status.update_available", "New version available. Update now?");
    langEn.put("symbol.arrow", "→");
    langEn.put("symbol.checked", "[x]");
    langEn.put("symbol.unchecked", "[  ]");
    langEn.put("time.error.invalid_duration", "Duration must be 1-999 minutes");
    langEn.put("time.error.invalid_format", "Invalid time format");
    langEn.put("time.error.invalid_hour", "Hour must be 0-23");
    langEn.put("time.error.invalid_minute", "Minute must be 0-59");
    langEn.put("time.hours", "Hours");
    langEn.put("time.input.minutes", "Enter duration (1-999 minutes)");
    langEn.put("time.input.time", "Enter time (24-hour format)");
    langEn.put("time.minutes", "Minutes");
    langEn.put("timer.action", "Timer Action");
    langEn.put("timer.actions.exit_app", "Exit Application");
    langEn.put("timer.actions.stop_playback", "Stop Playback");
    langEn.put("timer.cancel", "Cancel Timer");
    langEn.put("timer.confirm.exit", "Timer will exit the application when expired. Continue?");
    langEn.put("timer.mode.absolute", "Absolute Time");
    langEn.put("timer.mode.countdown", "Countdown Mode");
    langEn.put("timer.set", "Set Timer");
    langEn.put("timer.sleep_timer", "Sleep Timer");
    langEn.put("timer.status.cancelled", "Sleep timer cancelled");
    langEn.put("timer.status.expired", "Sleep timer expired");
    langEn.put("timer.status.remaining", "Time Remaining: {0}");
    langEn.put("timer.status.set", "Sleep timer activated");
    langEn.put("chat.input", "Enter message");
    langEn.put("chat.welcome_message", "Hello! How can I help you today?");
    langEn.put("error.connection", "Connection error. Please try again.");

    // Language: tr
    langTr.put("lang", "tr");
    langTr.put("action.back", "Geri");
    langTr.put("action.cancel", "İptal");
    langTr.put("action.exit", "Çık");
    langTr.put("action.no", "Hayır");
    langTr.put("action.ok", "Tamam");
    langTr.put("action.save", "Kaydet");
    langTr.put("action.yes", "Evet");
    langTr.put("app.name", "MIDPlay");
    langTr.put("confirm.exit", "Çıkmak istediğinizden emin misiniz?");
    langTr.put("favorites.add", "Favorilere Ekle");
    langTr.put("favorites.error.remove_failed", "Favori çıkarma hatası");
    langTr.put("favorites.error.save_failed", "Favori kaydetme hatası");
    langTr.put("favorites.remove", "Favorilerden Çıkar");
    langTr.put("favorites.status.added", "Favorilere eklendi");
    langTr.put("favorites.status.already_exists", "Zaten favorilerde");
    langTr.put("favorites.status.removed", "Favorilerden çıkarıldı");
    langTr.put("language.en", "English");
    langTr.put("language.tr", "Türkçe");
    langTr.put("language.vi", "Tiếng Việt");
    langTr.put("media.album", "Albüm");
    langTr.put("media.playlist", "Çalma Listesi");
    langTr.put("media.track", "Şarkı");
    langTr.put("media.tracks", "Şarkılar");
    langTr.put("menu.discover_playlists", "Çalma Listelerini Keşfet");
    langTr.put("menu.favorites", "Favoriler");
    langTr.put("menu.hot_playlists", "Popüler Çalma Listeleri");
    langTr.put("menu.main", "Ana Menü");
    langTr.put("menu.now_playing", "Şimdi Çalıyor");
    langTr.put("menu.reorder", "Menüyü Yeniden Sırala");
    langTr.put("menu.search", "Arama");
    langTr.put("menu.chat", "AI Sohbeti");
    langTr.put("menu.settings", "Ayarlar");
    langTr.put("menu.about", "Hakkında");
    langTr.put("menu.visibility", "Menü Görünürlüğü");
    langTr.put("player.next", "Sonraki");
    langTr.put("player.play", "Çal");
    langTr.put("player.previous", "Önceki");
    langTr.put("player.repeat", "Tekrarla");
    langTr.put("player.show_playlist", "Çalma Listesini Göster");
    langTr.put("player.shuffle", "Karıştır");
    langTr.put("player.status.loading", "Yükleniyor...");
    langTr.put("player.status.paused", "Duraklatıldı");
    langTr.put("player.status.playing", "Çalıyor...");
    langTr.put("player.status.starting", "Başlatılıyor...");
    langTr.put("player.status.stopped", "Durduruldu");
    langTr.put("player.status.stopping", "Durduruluyor...");
    langTr.put("player.status.finished", "Tamamlandı");
    langTr.put("player.status.ready", "Hazır");
    langTr.put("player.stop", "Durdur");
    langTr.put("player.volume", "Ses");
    langTr.put("playlist.add_track", "Çalma Listesine Ekle");
    langTr.put("playlist.create", "Çalma Listesi Oluştur");
    langTr.put(
        "playlist.confirm.delete_with_tracks",
        "Bu çalma listesinde şarkılar var. Çalma listesini ve tüm şarkıları sil?");
    langTr.put("playlist.error.add_track_failed", "Şarkı çalma listesine eklenemedi");
    langTr.put("playlist.error.track_already_exists", "Şarkı zaten çalma listesinde mevcut");
    langTr.put(
        "playlist.error.cannot_rename_system", "Sistem çalma listesi yeniden adlandırılamaz");
    langTr.put("playlist.error.create_failed", "Çalma listesi oluşturulamadı");
    langTr.put("playlist.error.empty_name", "Çalma listesi adı boş olamaz");
    langTr.put("playlist.error.remove_track_failed", "Şarkı çalma listesinden çıkarılamadı");
    langTr.put("playlist.error.rename_failed", "Çalma listesi yeniden adlandırılamadı");
    langTr.put("playlist.name", "Çalma Listesi Adı");
    langTr.put("playlist.rename", "Çalma Listesini Yeniden Adlandır");
    langTr.put("playlist.select", "Çalma Listesi Seç");
    langTr.put("playlist.status.created", "Çalma listesi başarıyla oluşturuldu");
    langTr.put("playlist.status.no_custom", "Özel çalma listesi yok");
    langTr.put("playlist.status.renamed", "Çalma listesi başarıyla yeniden adlandırıldı");
    langTr.put("playlist.status.track_added", "Şarkı çalma listesine eklendi");
    langTr.put("playlist.status.track_removed", "Şarkı çalma listesinden çıkarıldı");
    langTr.put("search.error.empty_keyword", "Lütfen arama için bir anahtar kelime girin");
    langTr.put("search.placeholder", "Arama için anahtar kelime girin...");
    langTr.put("search.results", "Arama Sonuçları");
    langTr.put("search.status.no_results", "Sonuç bulunamadı");
    langTr.put("search.status.searching", "Aranan: {0}");
    langTr.put("search.type", "Arama Türü");
    langTr.put("settings.audio_quality", "Ses Kalitesi");
    langTr.put("settings.auto_update", "Otomatik Güncellemeler");
    langTr.put("settings.check_update", "Güncellemeleri Kontrol Et");
    langTr.put("settings.player_method", "Oynatıcı Yöntemi");
    langTr.put("settings.force_pass_connection", "Bağlantıyı zorla geçir");
    langTr.put("settings.error.save_failed", "Ayarlar kaydedilemedi");
    langTr.put("settings.language", "Dil");
    langTr.put("settings.reorder.instructions", "Taşınacak öğeyi seçin, sonra hedef konumu seçin");
    langTr.put("settings.reorder.saved", "Sıralama kaydedildi");
    langTr.put("settings.service", "Servis");
    langTr.put("settings.status.saved", "Ayarlar başarıyla kaydedildi");
    langTr.put(
        "settings.visibility.instructions",
        "Gizle/göster öğeleri seçin. [x] = göster, [] = gizle. Tamamlamak için Kaydet'e basın.");
    langTr.put("settings.visibility.saved", "Görünürlük ayarları kaydedildi");
    langTr.put("status.error", "Bir hata oluştu");
    langTr.put("status.loading", "Yükleniyor...");
    langTr.put("status.load_more", "Daha fazla yükle...");
    langTr.put("status.no_data", "Veri yok!");
    langTr.put("status.no_updates", "Güncelleme yok. En son sürümü kullanıyorsunuz.");
    langTr.put("status.update_available", "Yeni sürüm mevcut. Şimdi güncellensin mi?");
    langTr.put("symbol.arrow", "→");
    langTr.put("symbol.checked", "[x]");
    langTr.put("symbol.unchecked", "[  ]");
    langTr.put("time.error.invalid_duration", "Süre 1-999 dakika arasında olmalı");
    langTr.put("time.error.invalid_format", "Geçersiz zaman formatı");
    langTr.put("time.error.invalid_hour", "Saat 0-23 arasında olmalı");
    langTr.put("time.error.invalid_minute", "Dakika 0-59 arasında olmalı");
    langTr.put("time.hours", "Saat");
    langTr.put("time.input.minutes", "Süre girin (1-999 dakika)");
    langTr.put("time.input.time", "Zaman girin (24 saat formatı)");
    langTr.put("time.minutes", "Dakika");
    langTr.put("timer.action", "Zamanlayıcı Eylemi");
    langTr.put("timer.actions.exit_app", "Uygulamadan Çık");
    langTr.put("timer.actions.stop_playback", "Çalmayı Durdur");
    langTr.put("timer.cancel", "Zamanlayıcıyı İptal Et");
    langTr.put(
        "timer.confirm.exit", "Zamanlayıcı süresi dolduğunda uygulamadan çıkacak. Devam et?");
    langTr.put("timer.mode.absolute", "Mutlak Zaman");
    langTr.put("timer.mode.countdown", "Geri Sayım Modu");
    langTr.put("timer.set", "Zamanlayıcı Ayarla");
    langTr.put("timer.sleep_timer", "Uyku Zamanlayıcısı");
    langTr.put("timer.status.cancelled", "Uyku zamanlayıcısı iptal edildi");
    langTr.put("timer.status.expired", "Uyku zamanlayıcısı süresi doldu");
    langTr.put("timer.status.remaining", "Kalan Süre: {0}");
    langTr.put("timer.status.set", "Uyku zamanlayıcısı etkinleştirildi");
    langTr.put("chat.input", "Mesaj girin");
    langTr.put("chat.welcome_message", "Merhaba! Size nasıl yardımcı olabilirim?");
    langTr.put("error.connection", "Bağlantı hatası. Lütfen tekrar deneyin.");

    // Language: vi
    langVi.put("lang", "vi");
    langVi.put("action.back", "Quay lại");
    langVi.put("action.cancel", "Hủy");
    langVi.put("action.exit", "Thoát");
    langVi.put("action.no", "Không");
    langVi.put("action.ok", "OK");
    langVi.put("action.save", "Lưu");
    langVi.put("action.yes", "Có");
    langVi.put("app.name", "MIDPlay");
    langVi.put("confirm.exit", "Bạn có chắc muốn thoát?");
    langVi.put("favorites.add", "Thêm vào yêu thích");
    langVi.put("favorites.error.remove_failed", "Lỗi xóa yêu thích");
    langVi.put("favorites.error.save_failed", "Lỗi lưu yêu thích");
    langVi.put("favorites.remove", "Xóa khỏi yêu thích");
    langVi.put("favorites.status.added", "Đã thêm vào yêu thích");
    langVi.put("favorites.status.already_exists", "Đã có trong yêu thích");
    langVi.put("favorites.status.removed", "Đã xóa khỏi yêu thích");
    langVi.put("language.en", "English");
    langVi.put("language.tr", "Türkçe");
    langVi.put("language.vi", "Tiếng Việt");
    langVi.put("media.album", "Album");
    langVi.put("media.playlist", "Playlist");
    langVi.put("media.track", "Bài hát");
    langVi.put("media.tracks", "Bài hát");
    langVi.put("menu.discover_playlists", "Khám phá playlist");
    langVi.put("menu.favorites", "Yêu thích");
    langVi.put("menu.hot_playlists", "Playlist nổi bật");
    langVi.put("menu.main", "Menu chính");
    langVi.put("menu.now_playing", "Đang phát");
    langVi.put("menu.reorder", "Sắp xếp menu");
    langVi.put("menu.search", "Tìm kiếm");
    langVi.put("menu.chat", "Trò chuyện AI");
    langVi.put("menu.settings", "Cài đặt");
    langVi.put("menu.about", "Giới thiệu");
    langVi.put("menu.visibility", "Hiển thị menu");
    langVi.put("player.next", "Tiếp theo");
    langVi.put("player.play", "Phát");
    langVi.put("player.previous", "Trước đó");
    langVi.put("player.repeat", "Lặp lại");
    langVi.put("player.show_playlist", "Hiện playlist");
    langVi.put("player.shuffle", "Phát ngẫu nhiên");
    langVi.put("player.status.loading", "Đang tải...");
    langVi.put("player.status.paused", "Đã tạm dừng");
    langVi.put("player.status.playing", "Đang phát...");
    langVi.put("player.status.starting", "Đang khởi động...");
    langVi.put("player.status.stopped", "Đã dừng");
    langVi.put("player.status.stopping", "Đang dừng...");
    langVi.put("player.status.finished", "Đã hoàn thành");
    langVi.put("player.status.ready", "Sẵn sàng");
    langVi.put("player.stop", "Dừng");
    langVi.put("player.volume", "Âm lượng");
    langVi.put("playlist.add_track", "Thêm vào playlist");
    langVi.put("playlist.create", "Tạo playlist");
    langVi.put(
        "playlist.confirm.delete_with_tracks",
        "Playlist này có bài hát. Xóa playlist và tất cả bài hát?");
    langVi.put("playlist.error.add_track_failed", "Lỗi thêm bài hát vào playlist");
    langVi.put("playlist.error.track_already_exists", "Bài hát đã có trong playlist");
    langVi.put("playlist.error.cannot_rename_system", "Không thể đổi tên playlist hệ thống");
    langVi.put("playlist.error.create_failed", "Lỗi tạo playlist");
    langVi.put("playlist.error.empty_name", "Tên playlist không được để trống");
    langVi.put("playlist.error.remove_track_failed", "Lỗi xóa bài hát khỏi playlist");
    langVi.put("playlist.error.rename_failed", "Lỗi đổi tên playlist");
    langVi.put("playlist.name", "Tên playlist");
    langVi.put("playlist.rename", "Đổi tên playlist");
    langVi.put("playlist.select", "Chọn playlist");
    langVi.put("playlist.status.created", "Tạo playlist thành công");
    langVi.put("playlist.status.no_custom", "Không có playlist tùy chỉnh");
    langVi.put("playlist.status.renamed", "Đổi tên playlist thành công");
    langVi.put("playlist.status.track_added", "Đã thêm bài hát vào playlist");
    langVi.put("playlist.status.track_removed", "Đã xóa bài hát khỏi playlist");
    langVi.put("search.error.empty_keyword", "Vui lòng nhập từ khóa");
    langVi.put("search.placeholder", "Nhập từ khóa để tìm kiếm...");
    langVi.put("search.results", "Kết quả tìm kiếm");
    langVi.put("search.status.no_results", "Không tìm thấy kết quả");
    langVi.put("search.status.searching", "Đang tìm: {0}");
    langVi.put("search.type", "Loại tìm kiếm");
    langVi.put("settings.audio_quality", "Chất lượng âm thanh");
    langVi.put("settings.auto_update", "Tự động cập nhật");
    langVi.put("settings.check_update", "Kiểm tra cập nhật");
    langVi.put("settings.player_method", "Phương thức phát");
    langVi.put("settings.force_pass_connection", "Ép buộc truyền kết nối");
    langVi.put("settings.error.save_failed", "Lỗi lưu cài đặt");
    langVi.put("settings.language", "Ngôn ngữ");
    langVi.put("settings.reorder.instructions", "Chọn mục để di chuyển, sau đó chọn vị trí đích");
    langVi.put("settings.reorder.saved", "Đã lưu thứ tự");
    langVi.put("settings.service", "Dịch vụ");
    langVi.put("settings.status.saved", "Lưu cài đặt thành công");
    langVi.put(
        "settings.visibility.instructions",
        "Chọn mục để ẩn/hiện. [x] = hiện, [] = ẩn. Nhấn Lưu để hoàn tất.");
    langVi.put("settings.visibility.saved", "Đã lưu cài đặt hiển thị");
    langVi.put("status.error", "Đã xảy ra lỗi");
    langVi.put("status.loading", "Đang tải...");
    langVi.put("status.load_more", "Tải thêm...");
    langVi.put("status.no_data", "Không có dữ liệu!");
    langVi.put("status.no_updates", "Không có cập nhật. Bạn đang dùng phiên bản mới nhất.");
    langVi.put("status.update_available", "Có phiên bản mới. Cập nhật ngay?");
    langVi.put("symbol.arrow", "→");
    langVi.put("symbol.checked", "[x]");
    langVi.put("symbol.unchecked", "[  ]");
    langVi.put("time.error.invalid_duration", "Thời lượng phải từ 1-999 phút");
    langVi.put("time.error.invalid_format", "Định dạng thời gian không hợp lệ");
    langVi.put("time.error.invalid_hour", "Giờ phải từ 0-23");
    langVi.put("time.error.invalid_minute", "Phút phải từ 0-59");
    langVi.put("time.hours", "Giờ");
    langVi.put("time.input.minutes", "Nhập thời lượng (1-999 phút)");
    langVi.put("time.input.time", "Nhập thời gian (định dạng 24 giờ)");
    langVi.put("time.minutes", "Phút");
    langVi.put("timer.action", "Hành động khi hết giờ");
    langVi.put("timer.actions.exit_app", "Thoát ứng dụng");
    langVi.put("timer.actions.stop_playback", "Dừng phát nhạc");
    langVi.put("timer.cancel", "Hủy hẹn giờ");
    langVi.put("timer.confirm.exit", "Hẹn giờ sẽ thoát ứng dụng khi hết thời gian. Tiếp tục?");
    langVi.put("timer.mode.absolute", "Thời gian tuyệt đối");
    langVi.put("timer.mode.countdown", "Chế độ đếm ngược");
    langVi.put("timer.set", "Đặt hẹn giờ");
    langVi.put("timer.sleep_timer", "Hẹn giờ tắt");
    langVi.put("timer.status.cancelled", "Đã hủy hẹn giờ tắt");
    langVi.put("timer.status.expired", "Hẹn giờ tắt đã hết thời gian");
    langVi.put("timer.status.remaining", "Thời gian còn lại: {0}");
    langVi.put("timer.status.set", "Đã kích hoạt hẹn giờ tắt");
    langVi.put("chat.input", "Nhập tin nhắn");
    langVi.put("chat.welcome_message", "Xin chào! Tôi có thể giúp gì cho bạn?");
    langVi.put("error.connection", "Lỗi kết nối. Vui lòng thử lại.");

    langs = new Hashtable[] {langEn, langTr, langVi};
  }

  public static void setLang(String code) {
    currentLang = code;
  }

  public static String getCurrentLang() {
    return currentLang;
  }

  public static String[] getAvailableLanguages() {
    String[] languages = new String[langs.length];
    for (int i = 0; i < langs.length; i++) {
      String langCode = (String) langs[i].get("lang");
      languages[i] = langCode != null ? langCode : "en";
    }
    return languages;
  }

  public static String tr(String key) {
    Hashtable lang = langs[0];
    if ("en".equals(currentLang)) {
      lang = langEn;
    } else if ("tr".equals(currentLang)) {
      lang = langTr;
    } else if ("vi".equals(currentLang)) {
      lang = langVi;
    }
    String value = (String) lang.get(key);
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
