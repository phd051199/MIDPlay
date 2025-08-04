/*
* This script created by Golden Dragon for the package dragongraphics and ported here by Golden Dragon.
*/



import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.midlet.MIDlet; 

public class Theme {
	public static Vector themes = new Vector();
	public static int debugColor = 0x234D11;
	private static Theme currentTheme = null;


	private String name = "";
        private boolean useLightImages = true;
	private Hashtable colors = new Hashtable();
	private Vector fonts = new Vector();

	public Theme(String name) {
		this.name = name;

		boolean isValid = true;
		String message = "";

                if (themes.size() > 0) {
			for (int i = 0; i < themes.size(); i++) {
				Theme theme = (Theme) themes.elementAt(i);
				if (theme.name.equals(name)) {
					isValid = false;
					message = "The chosen theme name is already used. Can't add to the themes list!";
					break;
				}
			}
		}
		if (isValid) {
			themes.addElement(this);
		} else {
			System.out.println(message);
		}
	}

	public String getName() {
		return this.name;
	}
        
        public Hashtable getAllColors(){
            return this.colors;
        }

	public static Theme getCurrentTheme() {
		return Theme.currentTheme;
	}

	public static Theme setCurrentTheme(String name) {
		return setCurrentTheme(name, false);
	}

	public static Theme setCurrentTheme(String name, boolean ignoreCases) {
		if (ignoreCases) {
			name = name.toLowerCase();
		}
		if (themes.size() > 0) {
			for (int i = 0; i < themes.size(); i++) {
				Theme theme = (Theme) themes.elementAt(i);
				String themeName = theme.name;
				if (ignoreCases) {
					themeName = themeName.toLowerCase();
				}
				if (themeName.equals(name)) {
					return setCurrentTheme(theme);
				}
			}
		}
		return null;
	}

	public static Theme setCurrentTheme(int index) {
		Theme theme = (Theme) themes.elementAt(index);
		setCurrentTheme(theme);
		return theme;
	}

	public static Theme setCurrentTheme(Theme theme) {
		if (themes.contains(theme)) {
			Theme.currentTheme = theme;
		}
		return theme;
	}

	public static int addColorToCurrent(String key, int color) {
		if (Theme.currentTheme != null) {
			Theme.currentTheme.addColor(key, color);
		}
		return color;
	}

	public int addColor(String key, int color) {
		Integer colorObject = new Integer(color);
		colors.put(key, colorObject);
		return color;
	}

	public static Font getFont(int index) {
		return (Font) Theme.currentTheme.fonts.elementAt(index);
	}

	public int addFont(Font font) {
		fonts.addElement(font);
		return fonts.indexOf(font);
	}

	public Font getFontByIndex(int index) {
		return (Font) fonts.elementAt(index);
	}

	public static int getColor(String key) {
		int color = -1;
		if (currentTheme != null) {
			color = currentTheme.getColorByKey(key);
		}
		return color;

	}

	public int getColorByKey(String key) {
		Integer colorObject = (Integer) colors.get(key);
		int color = -1;
		if (colorObject != null) {
			color = colorObject.intValue();
		}
		return color;
	}
        
        public boolean getUseLightImages(){
            return this.useLightImages;
        }
        public boolean setUseLightImages(boolean use){
            this.useLightImages = use;
            return this.useLightImages;
        }
        
        public static String[] getAllThemeNames(){
            if(themes.size() == 0) return null;
            
            String[] result = new String[themes.size()];
            for(int i=0; i<result.length; i++){
                Theme theme = (Theme) themes.elementAt(i);
                result[i] = theme.getName();
            }
            
            return result;
        }

	public static Vector toLowercaseThemes() {
		int size = themes.size();
		Vector result = new Vector();

		for (int i = 0; i < size; i++) {
			Theme theme = (Theme) themes.elementAt(i);
			result.addElement(theme.name.toLowerCase());

		}

		return result;
	}

	public static Vector themeResourceNames = new Vector();

	public static void addThemeResource(String resourceName) {
		themeResourceNames.addElement(resourceName);
	}

	public static Theme loadThemeFromResource(String resoureName) {
		// Construct the full path to the theme file
		String fullPath = "/themes/" + resoureName + ".json";
		System.out.println("Loading theme from: " + fullPath);

		// Read the file content
		String fileContent = MIDPlay.readResourceFile(fullPath);
		if (fileContent == null) {
			return null;
		}

		// Parse the JSON content
		try {
			// Parse the main JSON object
                        JSONObject themeData = JSON.getObject(fileContent);
			//Hashtable themeData = JsonUtils.parseJson(fileContent);
			// Extract the "theme" object
			if (!themeData.has("theme")) {
				System.out.println("Theme file does not contain 'theme' key: " + fullPath);
				return null;
			}
			themeData = themeData.getObject("theme");

			// Extract themeName
			if (!themeData.has("themeName")) {
				System.out.println("Theme file does not contain 'themeName' key: " + fullPath);
				return null;
			}
			String themeName = themeData.getString("themeName");
                        
                       // Extract useLightImages
                       boolean useLightImages = true;
			if (themeData.has("useLightImages")) {
                                useLightImages = themeData.getBoolean("useLightImages", true);
			}

			// Create a new theme with the extracted name
			Theme theme = new Theme(themeName);
                        
                        theme.setUseLightImages(useLightImages);

			theme.addFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));

			// Extract and process color values
			if (!themeData.has("colors")) {
				System.out.println("Theme file does not contain 'values' key: " + fullPath);
				return null;
			}
			JSONObject colorValues = themeData.getObject("colors");

			// Add all colors to the theme
			Enumeration keys = colorValues.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				int value = ColorUtils.parseColorFromString((String) colorValues.getString(key));
				theme.addColor(key, value);
			}

			return theme;
		} catch (JSONException e) {
			System.out.println("Error parsing theme file: " + e.getMessage());
			return null;
		}
	}

        public static Theme[] loadAllThemes() {
            return loadAllThemes(true);
            
        }

	public static Theme[] loadAllThemes(boolean clearList) {
		if (clearList) {
			themes.removeAllElements();
		}

		Theme[] result = new Theme[themeResourceNames.size()];
		for (int i = 0; i < themeResourceNames.size(); i++) {
			Theme theme = loadThemeFromResource((String) (themeResourceNames.elementAt(i)));
			result[i] = theme;
		}

		return result;
	}
}
