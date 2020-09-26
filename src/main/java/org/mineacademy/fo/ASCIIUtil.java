package org.mineacademy.fo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.NonNull;

/**
 * Textual ASCII generator.
 */
public class ASCIIUtil {

	public static final int SMALL = 12;
	public static final int MEDIUM = 18;
	public static final int LARGE = 24;

	/**
	 * Prints ASCII art for the specified text using {@link #MEDIUM} size and the * symbol
	 *
	 * @param message
	 * @param textHeight - Use a predefined sizes from this class or a custom type
	 * @param letterSymbols - The symbols that will be used to draw the text, randomly mixed
	 *
	 * @return the list of text lines you can print in console or send to a player
	 */
	public static List<String> generate(String message) {
		return generate(message, MEDIUM, Arrays.asList("*"));
	}

	/**
	 * Prints ASCII art for the specified text using {@link #MEDIUM} size and the given symbols
	 *
	 * @param message
	 * @param textHeight - Use a predefined sizes from this class or a custom type
	 * @param letterSymbols - The symbols that will be used to draw the text, randomly mixed, split by |
	 *
	 * @return the list of text lines you can print in console or send to a player
	 */
	public static List<String> generate(String message, @NonNull String letterSymbols) {
		return generate(message, MEDIUM, Arrays.asList(letterSymbols.split("\\|")));
	}

	/**
	 * Prints ASCII art for the specified text. For size, you can use predefined sizes or a custom size.
	 *
	 * @param message
	 * @param textHeight - Use a predefined sizes from this class or a custom type
	 * @param letterSymbols - The symbols that will be used to draw the text, randomly mixed
	 *
	 * @return the list of text lines you can print in console or send to a player
	 */
	public static List<String> generate(String message, int textHeight, @NonNull List<String> letterSymbols) {
		final List<String> texts = new ArrayList<>();

		final int imageWidth = findImageWidth(textHeight, message, "SansSerif");
		final BufferedImage bufferedImage = new BufferedImage(imageWidth, textHeight, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
		final Font font = new Font("SansSerif", Font.BOLD, textHeight);

		graphics.setFont(font);
		graphics.drawString(message, 0, getBaselinePosition(graphics, font));

		for (int y = 0; y < textHeight; y++) {
			final StringBuilder sb = new StringBuilder();

			for (int x = 0; x < imageWidth; x++)
				sb.append(bufferedImage.getRGB(x, y) == Color.WHITE.getRGB() ? RandomUtil.nextItem(letterSymbols) : " ");

			if (sb.toString().trim().isEmpty())
				continue;

			texts.add(sb.toString());
		}

		return texts;
	}

	/*
	 * Using the Current font and current art text find the width of the full image
	 */
	private static int findImageWidth(int textHeight, String artText, String fontName) {
		final BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics graphics = bufferedImage.getGraphics();

		graphics.setFont(new Font(fontName, Font.BOLD, textHeight));

		return graphics.getFontMetrics().stringWidth(artText);
	}

	/*
	 * Find where the text baseline should be drawn so that the characters are within image
	 */
	private static int getBaselinePosition(Graphics g, Font font) {
		final FontMetrics metrics = g.getFontMetrics(font);
		final int y = metrics.getAscent() - metrics.getDescent();

		return y;
	}
}