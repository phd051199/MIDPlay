package musicapp.common;

public class ConvertMessage {

    public static String convertLikeMessage(String message) {
        if (message.equals("user-not-found")) {
            return "Bạn chưa đăng nhập.  Xin vui lòng đăng nhập để thực hiện chức năng này";
        } else if (message.equals("like")) {
            return "Bạn đã không thích playlist này";
        } else {
            return message.equals("unlike") ? "Cảm ơn bạn đã thích playlist này" : "Không thể kết nối tới máy chủ! Xin vui lòng kiểm tra kết nối.";
        }
    }
}
