package org.mineacademy.fo.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextTranslator {

	private static final Pattern GRADIENT_PATTERN = Pattern.compile("(<#[a-fA-F0-9]{6}:#[a-fA-F0-9]{6}>)");
	private static final List<String> SPECIAL_SIGN = Arrays.asList("&l", "&n", "&o", "&k", "&m");

	/**
	 * This is for component when you want to send message
	 * thru vanilla minecraft(MNS).
	 * <p>
	 * Type your message/string text here. you use
	 * <p>
	 * §/& colorcode or <#55F758> for normal hex and
	 * <#5e4fa2:#f79459> for gradient (use §/&r to stop gradient).
	 *
	 * @param message your string message.
	 * @return json to string.
	 */

	public static String toComponent(String message) {
		return toComponent(message, null);
	}

	/**
	 * This is for component when you want to send message
	 * thru vanilla minecraft(MNS).
	 * <p>
	 * Type your message/string text here. you use
	 * <p>
	 * §/& colorcode or <#55F758> for normal hex and
	 * <#5e4fa2:#f79459> for gradient (use §/&r to stop gradient).
	 *
	 * @param message      your string message.
	 * @param defaultColor set default color when colors are not set in the message.
	 * @return json to string.
	 */

	public static String toComponent(String message, String defaultColor) {
		JsonArray jsonArray = new JsonArray();
		Component.Builder component = new Component.Builder();
		//Matcher matcherGradient = GRADIENT_PATTERN.matcher(message);
		if (defaultColor == null || defaultColor.equals(""))
			defaultColor = "white";

	/*	if (matcherGradient.find()) {
			message = createGradients(message);
		}*/
		StringBuilder builder = new StringBuilder(message.length());
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < message.length(); i++) {
			char letter = message.charAt(i);
			boolean checkChar;
			boolean checkHex = false;

			if (i + 1 < message.length() && letter == ChatColors.COLOR_CHAR || letter == '&' || letter == '#') {
				char msg = message.charAt(i + 1);

				if (checkIfColor(msg)) {
					checkChar = true;
				} else if (msg == '#') {
					hex = new StringBuilder();
					for (int j = 0; j < 7; j++) {
						hex.append(message.charAt(i + 1 + j));
					}
					boolean isHexCode = isValidHexCode(hex.toString());
					checkChar = isHexCode;
					checkHex = isHexCode;
				} else checkChar = false;
			} else checkChar = false;

			if (checkChar) {
				if (++i >= message.length()) {
					break;
				}
				letter = message.charAt(i);

				if (letter >= 'A' && letter <= 'Z') {
					letter += 32;
				}
				String format;
				if (checkHex) {
					format = hex.toString();
					i += 7;
				} else {
					try {
						format = ChatColors.getByChar(letter).getName();
					} catch (Exception ignore) {
						format = null;
					}
				}
				if (format == null) {
					System.out.println("foramt nulllll");
					continue;
				}
				if (builder.length() > 0) {
					component.message(builder.toString());
					builder = new StringBuilder();
					jsonArray.add(component.build().toJson());
					component = new Component.Builder();

				}
				if (format.equals(ChatColors.BOLD.getName())) {
					component.bold(true);
				} else if (format.equals(ChatColors.ITALIC.getName())) {
					component.italic(true);
				} else if (format.equals(ChatColors.UNDERLINE.getName())) {
					component.underline(true);
				} else if (format.equals(ChatColors.STRIKETHROUGH.getName())) {
					component.strikethrough(true);
				} else if (format.equals(ChatColors.MAGIC.getName())) {
					component.obfuscated(true);
				} else if (format.equals(ChatColors.RESET.getName())) {
					format = defaultColor;
					component.reset(true);
					component.colorCode(format);
				} else {
					component.colorCode(format);
				}
				continue;
			}
			builder.append(letter);
		}

		component.message(builder.toString());
		jsonArray.add(component.build().toJson());

		if (jsonArray.size() > 1) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("extra", jsonArray);
			jsonObject.addProperty("text", "");
			return jsonObject.toString();
		}
		return component.build() + "";
	}


	/**
	 * Create a string with colors added on every letter
	 * (some are after the color code and to message length or to &r).
	 *
	 * @param message message you want to check and translate
	 * @return string with added gradient and rest unaffected some are outside the gradient range.
	 */

	private static String createGradients(String message) {

		Matcher gradientsMatcher = GRADIENT_PATTERN.matcher(message);
		StringBuilder specialSign = new StringBuilder();
		StringBuilder builder = new StringBuilder();
		int messageLength = message.length();
		int nextGradiensPos = 0;
		while (gradientsMatcher.find()) {
			String match = gradientsMatcher.group(0);

			int startGradient = nextGradiensPos > 0 ? nextGradiensPos : message.indexOf(match);
			int stopGradient = checkForR(message.substring(startGradient)) != -1 ? startGradient + checkForR(message.substring(startGradient)) : message.length();

			String gradientsRaw = match.substring(1, match.length() - 1);
			int splitPos = gradientsRaw.indexOf(":");
			String colorizeMsg = message.substring(startGradient + match.length(), stopGradient);

			for (String color : SPECIAL_SIGN) {
				if (colorizeMsg.contains(color)) {
					specialSign = new StringBuilder();
					specialSign.append(color);
					colorizeMsg = colorizeMsg.replace(color, "");
				} else specialSign = new StringBuilder();
			}
			int step = colorizeMsg.length();

			Color firstColor = hexToRgb(gradientsRaw.substring(0, splitPos));
			Color endColor = hexToRgb(gradientsRaw.substring(splitPos + 1));
			int stepRed = Math.abs(firstColor.getRed() - endColor.getRed()) / (step - 1);
			int stepGreen = Math.abs(firstColor.getGreen() - endColor.getGreen()) / (step - 1);
			int stepBlue = Math.abs(firstColor.getBlue() - endColor.getBlue()) / (step - 1);

			int[] direction = new int[]{
					firstColor.getRed() < endColor.getRed() ? +1 : -1,
					firstColor.getGreen() < endColor.getGreen() ? +1 : -1,
					firstColor.getBlue() < endColor.getBlue() ? +1 : -1};

			builder.append(message, Math.max(nextGradiensPos, 0), startGradient);
			for (int i = 0; i < step; i++) {
				String colors = convertRGBtoHex(firstColor.getRed() + ((stepRed * i) * direction[0]), firstColor.getGreen() + ((stepGreen * i) * direction[1]), firstColor.getBlue() + ((stepBlue * i) * direction[2]));
				builder.append("<").append(colors).append(">").append(specialSign).append(colorizeMsg.charAt(i));
			}

			String gradentMessage = message.substring(stopGradient, messageLength);
			if (gradentMessage.contains("<#")) {
				Matcher gradiensMatch = GRADIENT_PATTERN.matcher(gradentMessage);
				if (gradiensMatch.find()) {
					nextGradiensPos = (stopGradient + gradentMessage.indexOf(gradiensMatch.group(0)));
				} else nextGradiensPos = messageLength;
			} else if (nextGradiensPos <= 0) {
				nextGradiensPos = messageLength;
			}
			builder.append(message, Math.min(nextGradiensPos, stopGradient), Math.min(nextGradiensPos, messageLength));

		}
		return builder.toString();
	}

	/**
	 * Convert RGB to hex.
	 *
	 * @param R red color.
	 * @param G green color.
	 * @param B blue color.
	 * @return hex color or 0 if RGB values are over 255 or below 0.
	 */
	private static String convertRGBtoHex(int R, int G, int B) {
		if ((R >= 0 && R <= 255)
				&& (G >= 0 && G <= 255)
				&& (B >= 0 && B <= 255)) {

			Color color = new Color(R, G, B);
			StringBuilder hex = new StringBuilder(Integer.toHexString(color.getRGB() & 0xffffff));
			while (hex.length() < 6) {
				hex.insert(0, "0");
			}
			hex.insert(0, "#");
			return hex.toString();
		}
		// The hex color code doesn't exist
		else
			return "0";
	}

	/**
	 * convert hex to RGB.
	 *
	 * @param colorStr hex you want to transform.
	 * @return RBGcolors.
	 */
	private static Color hexToRgb(String colorStr) {
		return new Color(
				Integer.valueOf(colorStr.substring(1, 3), 16),
				Integer.valueOf(colorStr.substring(3, 5), 16),
				Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	/**
	 * Check if added &r  or §r.
	 *
	 * @param message the text you want to check.
	 * @return true if contains &r or §r.
	 */

	private static int checkForR(String message) {
		String msg = message.toLowerCase(Locale.ROOT);
		return msg.contains("&r") ? msg.indexOf("&r") : msg.contains("§r") ? msg.indexOf("§r") : -1;
	}

	/**
	 * Check if it valid color symbol.
	 *
	 * @param message check color symbol.
	 * @return true if it is valid color symbol.
	 */

	private static boolean checkIfColor(char message) {

		for (String color : ChatColors.ALL_CODES)
			if (color.equals(String.valueOf(message)))
				return true;
		return false;
	}

	/**
	 * Check if it is a valid hex or not.
	 *
	 * @param str you want to check
	 * @return true if it valid hex color.
	 */
	public static boolean isValidHexCode(String str) {
		// Regex to check valid hexadecimal color code.
		String regex = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";

		// Compile the ReGex
		Pattern pattern = Pattern.compile(regex);

		// If the string is empty
		// return false
		if (str == null) {
			return false;
		}

		// Pattern class contains matcher() method
		// to find matching between given string
		// and regular expression.
		Matcher matcher = pattern.matcher(str);

		// Return if the string
		// matched the ReGex
		return matcher.matches();
	}
}
