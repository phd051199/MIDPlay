package musicapp;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import org.json.me.JSONObject;
import musicapp.common.ParseData;
import musicapp.common.ReadWriteRecordStore;
import musicapp.utils.Utils;

public class LoginForm extends Form implements CommandListener, LoadDataObserver {

    private TextField userName = new TextField("UserName:", "", 30, 0);
    private TextField password = new TextField("Password:", "", 30, 65536);
    private Command exitCommand;
    private Command loginComand;
    private Command nowPlayingCommand;
    StringItem message = new StringItem("", "");
    private Utils.BreadCrumbTrail observer;
    Thread mLoaDataThread;
    private String defaultlistkey;

    public LoginForm(String title) {
        super(title);
        this.readLoginRecordStore();
        this.append(this.userName);
        this.append(this.password);
        this.append(this.message);
        this.initMenu();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.exitCommand) {
            this.observer.goBack();
        } else if (c == this.loginComand) {
            this.doLogin(this.userName.getString(), this.password.getString());
        } else if (c == this.nowPlayingCommand) {
            MainList.gotoNowPlaying(this.observer);
        }

    }

    private void doLogin(final String userName, final String passWord) {
        this.message.setText("");
        if (!this.checkLoginInvalidate()) {
            this.message.setText("Xin vui lòng nhập đầy đủ thông tin tài khoản!");
        } else {
            this.displayMessage("Đang tải dữ liệu...", "loading");
            this.mLoaDataThread = new Thread(new Runnable() {
                public void run() {
                    String result = ParseData.parseLoginResult(userName, passWord);

                    try {
                        JSONObject json = new JSONObject(result);
                        result = json.getString("result");
                        LoginForm.this.defaultlistkey = json.getString("defaultlistkey");
                    } catch (Exception var4) {
                        var4.printStackTrace();
                    }

                    if ("LoginSuccess".equals(result)) {
                        ReadWriteRecordStore recordStore = new ReadWriteRecordStore();
                        recordStore.deleteRecStore();
                        LoginForm.this.writeUserNameToRecordStore(userName, passWord);
                        LoginForm.this.gotoCuaTui();
                    } else {
                        LoginForm form = new LoginForm("Đăng nhập");
                        form.setObserver(LoginForm.this.observer);
                        form.message.setText("Đăng nhập thất bại! Vui lòng kiểm tra thông tin tài khoản");
                        LoginForm.this.observer.replaceCurrent(form);
                    }

                }
            });
            this.mLoaDataThread.start();
        }
    }

    public void setObserver(Utils.BreadCrumbTrail _observer) {
        this.observer = _observer;
    }

    private void gotoCuaTui() {
        Image[] images = null;

        try {
            images = new Image[]{Image.createImage("/images/icon_listened.png"), Image.createImage("/images/icon_tui.png"), Image.createImage("/images/icon_like.png")};
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        String[] mainItems = new String[]{"Nghe NhacCuaTui", "Playlist của Tui", "Playlist Tui thích"};
        CuaTuiCanvasList myCanvas = new CuaTuiCanvasList(this.userName.getString(), mainItems, images, this.defaultlistkey);
        myCanvas.setObserver(this.observer);
        this.observer.replaceCurrent(myCanvas);
    }

    private void displayMessage(String message, String messageType) {
        MessageForm messageForm = new MessageForm("Đăng nhập", message, messageType);
        messageForm.setObserver(this.observer);
        if (messageType.equals("error")) {
            this.observer.go(messageForm);
        } else {
            this.observer.replaceCurrent(messageForm);
        }

    }

    private void writeUserNameToRecordStore(String userName, String pass) {
        try {
            ReadWriteRecordStore recordStore = new ReadWriteRecordStore();
            recordStore.openRecStore();
            recordStore.writeRecord(userName);
            recordStore.writeRecord(pass);
            recordStore.closeRecStore();
        } catch (Exception var4) {
        }

    }

    private void readLoginRecordStore() {
        try {
            ReadWriteRecordStore recordStore = new ReadWriteRecordStore();
            recordStore.openRecStore();
            Vector items = recordStore.readRecords();
            recordStore.closeRecStore();
            if (items == null) {
                return;
            }

            if (items.size() >= 2) {
                String name = (String) items.elementAt(0);
                String pass = (String) items.elementAt(1);
                if (!"".equals(name) && !"".equals(pass)) {
                    this.userName = new TextField("UserName:", name, 50, 0);
                    this.password = new TextField("Password:", pass, 50, 65536);
                }
            }
        } catch (Exception var5) {
        }

    }

    private void initMenu() {
        this.loginComand = new Command("Login", 8, 0);
        this.nowPlayingCommand = new Command("Đang chơi...", 8, 1);
        this.exitCommand = new Command("Trở lại", 7, 0);
        this.addCommand(this.loginComand);
        this.addCommand(this.exitCommand);
        this.addCommand(this.nowPlayingCommand);
        this.setCommandListener(this);
    }

    private boolean checkLoginInvalidate() {
        return !this.userName.getString().equals("") && !this.password.getString().equals("");
    }

    public void cancel() {
        this.quit();
    }

    public void quit() {
        try {
            if (this.mLoaDataThread.isAlive()) {
                this.mLoaDataThread.join();
            }
        } catch (InterruptedException var2) {
            var2.printStackTrace();
        }

    }
}
