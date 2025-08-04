/*
* This script created by Golden Dragon for the package dragongraphics and ported here by Golden Dragon.
*/



public class ColorUtils {
	public static final int adjust(int baseColor, float factor) {
		int red = (int) ((baseColor >> 16 & 0xFF) * factor);
		int green = (int) ((baseColor >> 8 & 0xFF) * factor);
		int blue = (int) ((baseColor & 0xFF) * factor);
		return (0xFF << 24) | (red << 16) | (green << 8) | blue;
	}

	public static final int[] extractComponents(int color) {
		int[] result = new int[3];

		result[0] = (color >> 16) & 0xFF;
		result[1] = (color >> 8) & 0xFF;
		result[2] = color & 0xFF;

		return result;

	}

	public static final int tint(int color, int tint, double factor) {
		int result = 0xFFFFFF;

		// Extract RGB components from colors
		int[] aComps = extractComponents(color);
		int[] bComps = extractComponents(tint);
		;

		// Calculate the grayscale value (luminance)
		int grayscale = (aComps[0] * 30 + aComps[1] * 59 + aComps[2] * 11) / 100;

		// Calculate the tint factor (inversely related to grayscale)
		float tintFactor = grayscale / 255.0f;

		// Calculate tinted RGB components
		int R_tinted = (int) (bComps[0] * (1 - tintFactor) + aComps[0] * tintFactor);
		int G_tinted = (int) (bComps[1] * (1 - tintFactor) + aComps[1] * tintFactor);
		int B_tinted = (int) (bComps[2] * (1 - tintFactor) + aComps[2] * tintFactor);

		// Reassemble tinted color
		result = (0xFF << 24) | (R_tinted << 16) | (G_tinted << 8) | B_tinted;

		return result;
	}

	public static final int interpolateColor(int colorA, int colorB, float ratio) {
		int rA = (colorA >> 16) & 0xFF;
		int gA = (colorA >> 8) & 0xFF;
		int bA = colorA & 0xFF;

		int rB = (colorB >> 16) & 0xFF;
		int gB = (colorB >> 8) & 0xFF;
		int bB = colorB & 0xFF;

		int r = (int) (rA + (rB - rA) * ratio);
		int g = (int) (gA + (gB - gA) * ratio);
		int b = (int) (bA + (bB - bA) * ratio);

		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}
	
	public static int parseColorFromString(String hexColor) {
	    if (hexColor == null || hexColor.length() != 8 || 
	        (!hexColor.startsWith("0x") && !hexColor.startsWith("0X"))) {
	        return -1;
	    }
	    
	    try {
	        // Extract the hex part (without "0x")
	        String hexValue = hexColor.substring(2);
	        
	        // For J2ME, we need to parse each component separately
	        int r = Integer.parseInt(hexValue.substring(0, 2), 16);
	        int g = Integer.parseInt(hexValue.substring(2, 4), 16);
	        int b = Integer.parseInt(hexValue.substring(4, 6), 16);
	        
	        // Combine into a single color value
	        return (0xFF << 24) | (r << 16) | (g << 8) | b;
	    } catch (Exception e) {
	        return -1;
	    }
	}
	
	/**
	 * Blend two colors together with specified ratio
	 * 
	 * @param color1 First color
	 * @param color2 Second color
	 * @param ratio Blend ratio (0.0 = all color1, 1.0 = all color2)
	 * @return Blended color
	 */
	public static int blendColors(int color1, int color2, float ratio) {
		// Clamp ratio between 0 and 1
		if (ratio < 0.0f) ratio = 0.0f;
		if (ratio > 1.0f) ratio = 1.0f;
		
		// Extract RGB components from both colors
		int r1 = (color1 >> 16) & 0xFF;
		int g1 = (color1 >> 8) & 0xFF;
		int b1 = color1 & 0xFF;
		
		int r2 = (color2 >> 16) & 0xFF;
		int g2 = (color2 >> 8) & 0xFF;
		int b2 = color2 & 0xFF;
		
		// Blend the components
		int r = (int)(r1 + (r2 - r1) * ratio);
		int g = (int)(g1 + (g2 - g1) * ratio);
		int b = (int)(b1 + (b2 - b1) * ratio);
		
		// Ensure components are within valid range
		r = Math.max(0, Math.min(255, r));
		g = Math.max(0, Math.min(255, g));
		b = Math.max(0, Math.min(255, b));
		
		// Combine into a single color value
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}
}
