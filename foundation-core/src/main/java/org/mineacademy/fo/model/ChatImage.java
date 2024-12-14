package org.mineacademy.fo.model;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ValidCore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.text.format.TextColor;

/**
 * Represents a way to show an image in chat.
 *
 * @author bobacadodl and kangarko
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatImage {

	/**
	 * Represents the height of the image.
	 */
	private int height = 8;

	/**
	 * Represents the fillter character of the image.
	 */
	private FillerCharacter fillerCharacter = FillerCharacter.BLOCK;

	/**
	 * The strategy to resize the image. By default, the TYPE_NEAREST_NEIGHBOR does not
	 * "smooth" edges, which is suitable for avatars and Minecraft assets. Change to
	 * {@link AffineTransformOp#TYPE_BILINEAR} to antialias the downsized image.
	 *
	 * @see AffineTransformOp
	 */
	private int resizeMethod = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;

	/**
	 * Edit the color used as background for PNG images, by default WHITE.
	 */
	private Color backgroundColor = Color.WHITE;

	/**
	 * Represents the currently loaded lines.
	 */
	private String[] lines;

	/**
	 * Sets the height of the image.
	 *
	 * @param height
	 * @return
	 */
	public ChatImage height(final int height) {
		this.height = height;

		return this;
	}

	/**
	 * Sets the character type for the image.
	 *
	 * @param fillerCharacter
	 * @return
	 */
	public ChatImage fillerCharacter(final FillerCharacter fillerCharacter) {
		this.fillerCharacter = fillerCharacter;

		return this;
	}

	/**
	 * Sets the resize method for the image.
	 *
	 * @param resizeMethod
	 * @return
	 */
	public ChatImage resizeMethod(final int resizeMethod) {
		this.resizeMethod = resizeMethod;

		return this;
	}

	/**
	 * Sets the background color for PNG images.
	 *
	 * @param backgroundColor
	 * @return
	 */
	public ChatImage backgroundColor(final Color backgroundColor) {
		this.backgroundColor = backgroundColor;

		return this;
	}

	/**
	 * Sets the lines of the image.
	 *
	 * @param lines
	 * @return
	 */
	public ChatImage lines(final String[] lines) {
		this.lines = lines;

		return this;
	}

	/**
	 * Draw the image from the given player's head. Warning: This is a blocking operation.
	 *
	 * @param playerName
	 * @return
	 * @throws IOException
	 */
	public ChatImage drawFromHead(final String playerName) throws IOException {
		return this.drawFromUrl("https://mc-heads.net/avatar/" + playerName + "/" + this.height + ".png");
	}

	/**
	 * Draw the image from the given file. Warning: This is a blocking operation.
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public ChatImage drawFromFile(final File file) throws IOException {
		ValidCore.checkBoolean(file.exists(), "Cannot load image from non existing file " + file.toPath());

		return this.draw(ImageIO.read(file));
	}

	/**
	 * Draw the image from the given URL. Warning: This is a blocking operation.
	 *
	 * @param webUrl
	 * @return
	 * @throws IOException
	 */
	public ChatImage drawFromUrl(final String webUrl) throws IOException {
		return this.draw(ImageIO.read(new URL(webUrl)));
	}

	/**
	 * Draw the image from the given Java image.
	 *
	 * @param image
	 * @return
	 */
	public ChatImage draw(final BufferedImage image) {
		ValidCore.checkBoolean(this.height >= 2, "File image height must be equal or above 2");

		final BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		newImage.createGraphics().drawImage(image, 0, 0, this.backgroundColor, null);

		final TextColor[][] colors = this.parseImage(newImage);

		this.lines = this.parseColors(colors);

		return this;
	}

	/*
	 * Parse the given image into chat colors.
	 */
	private TextColor[][] parseImage(final BufferedImage newImage) {
		final double ratio = (double) newImage.getHeight() / newImage.getWidth();
		int width = (int) (this.height / ratio);

		if (width > 10)
			width = 10;

		final BufferedImage resized = this.resizeImage(newImage, (int) (this.height / ratio), this.height);
		final TextColor[][] chatImg = new TextColor[resized.getWidth()][resized.getHeight()];

		for (int x = 0; x < resized.getWidth(); x++)
			for (int y = 0; y < resized.getHeight(); y++)
				chatImg[x][y] = TextColor.color(resized.getRGB(x, y));

		return chatImg;
	}

	/*
	 * Resize the given image.
	 */
	private BufferedImage resizeImage(final BufferedImage originalImage, final int width, final int height) {
		final AffineTransform af = new AffineTransform();

		af.scale(
				width / (double) originalImage.getWidth(),
				height / (double) originalImage.getHeight());

		final AffineTransformOp operation = new AffineTransformOp(af, this.resizeMethod);

		return operation.filter(originalImage, null);
	}

	/*
	 * Parse the given 2D colors to fit lines.
	 */
	private String[] parseColors(final TextColor[][] colors) {
		final String[] lines = new String[colors[0].length];

		for (int y = 0; y < colors[0].length; y++) {
			String line = "";

			for (final TextColor[] lineColors : colors) {
				final TextColor color = lineColors[y];

				line += color != null ? "<" + color.toString() + ">" + this.fillerCharacter : ' ';
			}

			lines[y] = line + CompChatColor.RESET;
		}

		return lines;
	}

	/**
	 * Appends the given text next to the image. We use MiniMessage tags for the color
	 * which you need to parse yourself.
	 *
	 * @param text
	 * @return
	 */
	public String[] toString(final Collection<String> text) {
		return this.toString(CommonCore.toArray(text));
	}

	/**
	 * Appends the given text next to the image. We use MiniMessage tags for the color
	 * which you need to parse yourself.
	 *
	 * @param text
	 * @return
	 */
	public String[] toString(@NonNull final String... text) {
		ValidCore.checkBoolean(this.lines != null && this.lines.length > 0, "Set lines first using draw() methods or setLines()");

		final String[] lines = this.lines.clone();

		for (int y = 0; y < lines.length; y++)
			if (text.length > y) {
				final String line = text[y];

				lines[y] += " " + line;
			}

		return lines;
	}

	/**
	 * Return the raw image lines.
	 *
	 * @return
	 */
	public String[] getLines() {
		return this.lines;
	}

	/**
	 * Return if the image has lines.
	 *
	 * @return
	 */
	public boolean hasLines() {
		return this.lines != null && this.lines.length > 0;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new chat image builder.
	 *
	 * @return
	 */
	public static ChatImage builder() {
		return new ChatImage();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Represents common image characters.
	 *
	 * @author bobacadodl
	 */
	public enum FillerCharacter {

		BLOCK('\u2588'),
		DARK_SHADE('\u2593'),
		MEDIUM_SHADE('\u2592'),
		LIGHT_SHADE('\u2591');

		/**
		 * The character used to build the image.
		 */
		@Getter
		private final char character;

		FillerCharacter(final char c) {
			this.character = c;
		}

		/**
		 * Return the character
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return String.valueOf(this.character);
		}
	}
}