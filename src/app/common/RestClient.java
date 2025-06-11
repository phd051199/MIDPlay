package app.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class RestClient {

    private static void configureConncetion(HttpConnection conn) throws IOException {
        String locale = System.getProperty("microedition.locale");
        if (locale == null) {
            locale = "en-US";
        }

        String platform = System.getProperty("microedition.platform");
        if (platform == null) {
            platform = "GenericJ2ME";
        }
        String userAgent = platform + "/1.0 (MIDP-2.1; CLDC-1.1)";
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Language", locale);
    }

    public static HttpConnection getConnection(String url) throws IOException {
        HttpConnection conn = (HttpConnection) Connector.open(url);
        configureConncetion(conn);
        return conn;
    }

    public static HttpConnection getConnection(String url, int access) throws IOException {
        HttpConnection conn = (HttpConnection) Connector.open(url, access);
        configureConncetion(conn);
        return conn;
    }

    public String get(String url) throws IOException {
        HttpConnection hcon = null;
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        String data = null;

        System.out.println(url);

        try {
            int redirectTimes = 0;

            boolean redirect;
            do {
                redirect = false;
                hcon = getConnection(url);
                inputStream = hcon.openInputStream();
                baos = new ByteArrayOutputStream();

                int ch;
                while ((ch = inputStream.read()) != -1) {
                    baos.write(ch);
                }

                byte[] responseData = baos.toByteArray();
                data = new String(responseData, "UTF-8");

                int status = hcon.getResponseCode();
                switch (status) {
                    case 200:
                        break;
                    case 301:
                    case 302:
                    case 307:
                        url = hcon.getHeaderField("location");
                        if (inputStream != null) {
                            inputStream.close();
                        }

                        if (hcon != null) {
                            hcon.close();
                        }

                        hcon = null;
                        ++redirectTimes;
                        redirect = true;
                        break;
                    default:
                        hcon.close();
                        throw new IOException("Response status not OK:" + status);
                }
            } while (redirect && redirectTimes < 5);

            if (redirectTimes == 5) {
                throw new IOException("Too much redirects");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (hcon != null) {
                    hcon.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        return data;
    }
}
