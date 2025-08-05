import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;

public class ThemeColorOperation extends NetworkOperation {
  private final String colorHex;
  private final ThemeColorListener listener;

  public ThemeColorOperation(String colorHex, ThemeColorListener listener) {
    this.colorHex = colorHex;
    this.listener = listener;
  }

  protected void execute() {
    this.network = new Network(this);
    this.network.startHttpGet(URLProvider.getThemeColor(colorHex));
  }

  protected void processResponse(String response) {
    try {
      JSONObject jsonResponse = JSON.getObject(response);
      if (jsonResponse.has("light") && jsonResponse.has("dark")) {
        listener.onThemeColorsReceived(
            jsonResponse.getObject("light"), jsonResponse.getObject("dark"));
      } else {
        listener.onError(new Exception("Light or dark theme not found in response"));
      }
    } catch (Exception e) {
      listener.onError(e);
    }
  }

  protected void handleNoData() {
    listener.onError(new Exception("No theme color data received"));
  }

  protected void handleError(Exception e) {
    listener.onError(e);
  }

  public interface ThemeColorListener {
    void onThemeColorsReceived(JSONObject lightColors, JSONObject darkColors);

    void onError(Exception e);
  }
}
