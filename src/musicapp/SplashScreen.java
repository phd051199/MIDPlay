package musicapp;

import java.io.IOException;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class SplashScreen extends Canvas implements Runnable {

    private Image mImage;
    private SplashObserver observer;

    public SplashScreen() {
        this.setFullScreenMode(true);

        try {
            this.mImage = Image.createImage("/images/splashscreen.png");
            Thread t = new Thread(this);
            t.start();
        } catch (IOException var2) {
        }

    }

    public void paint(Graphics g) {
        int width = this.getWidth();
        int height = this.getHeight();
        g.setColor(0);
        g.fillRect(0, 0, width, height);
        if (this.mImage != null) {
            g.drawImage(this.mImage, width / 2, height / 2, 3);
        }

    }

    public void dismiss() {
        if (this.isShown()) {
            this.observer.dismiss();
        }

    }

    public void run() {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException var2) {
            System.out.println("InterruptedException");
        }

        this.dismiss();
    }

    public void keyReleased(int keyCode) {
        this.dismiss();
    }

    public void pointerReleased(int x, int y) {
        this.dismiss();
    }

    public void setObserver(SplashObserver _observer) {
        this.observer = _observer;
    }
}
