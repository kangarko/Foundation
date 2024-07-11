package org.mineacademy.fo.slider;

import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.CompChatColor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A colored text slider takes in a string and then applies
 * colors to it from left to right side moving the color in the text
 * automatically.
 *
 * Example use: Animated scoreboards, menu titles, etc.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColoredTextSlider implements Slider<String> {

	/**
	 * The string to colorize.
	 */
	private final String text;

	/**
	 * How many letters of the string to colorize each time?
	 */
	private int width = 5;

	/**
	 * The primary text color. Defaults to black.
	 */
	private String primaryPrefix = CompChatColor.BLACK.toString();

	/**
	 * The colorized text color. Defaults to dark red.
	 */
	private String secondaryPrefix = CompChatColor.DARK_RED.toString();

	/*
	 * The current head in the slider.
	 */
	private int currentPointer = Integer.MIN_VALUE;

	/**
	 * Set the amount of letters of the text we apply
	 * {@link #getSecondaryPrefix()} to.
	 *
	 * @param width
	 * @return
	 */
	public ColoredTextSlider width(int width) {
		this.width = width;

		return this;
	}

	/**
	 * Set the primary color for the text.
	 *
	 * @param primaryPrefix
	 * @return
	 */
	public ColoredTextSlider primaryPrefix(CompChatColor primaryPrefix) {
		return this.primaryPrefix(primaryPrefix.toString());
	}

	/**
	 * Set the primary color for the text.
	 *
	 * @param primaryPrefix
	 * @return
	 */
	public ColoredTextSlider primaryPrefix(String primaryPrefix) {
		this.primaryPrefix = primaryPrefix;

		return this;
	}

	/**
	 * Set the secondary color to apply for the given X amount of letters
	 * in the text.
	 *
	 * @param secondaryPrefix
	 * @return
	 */
	public ColoredTextSlider secondaryPrefix(CompChatColor secondaryPrefix) {
		return this.secondaryPrefix(secondaryPrefix.toString());
	}

	/**
	 * Set the secondary color to apply for the given X amount of letters
	 * in the text.
	 *
	 * @param secondaryPrefix
	 * @return
	 */
	public ColoredTextSlider secondaryPrefix(String secondaryPrefix) {
		this.secondaryPrefix = secondaryPrefix;

		return this;
	}

	/**
	 * @see org.mineacademy.fo.slider.Slider#next()
	 */
	@Override
	public String next() {

		if (this.currentPointer == Integer.MIN_VALUE || this.currentPointer == this.text.length())
			this.currentPointer = 1 - this.width;

		final int from = MathUtil.range(this.currentPointer, 0, this.text.length());
		final int to = MathUtil.range(this.currentPointer + this.width, 0, this.text.length());

		final String before = CompChatColor.translateColorCodes(this.primaryPrefix + this.text.substring(0, from));
		final String part = CompChatColor.translateColorCodes(this.secondaryPrefix + this.text.substring(from, to));
		final String after = CompChatColor.translateColorCodes(this.primaryPrefix + this.text.substring(to));

		this.currentPointer++;

		return before + part + after;
	}

	/**
	 * Create a new slider for the given text.
	 *
	 * @param text
	 * @return
	 */
	public static ColoredTextSlider from(String text) {
		ValidCore.checkBoolean(!CompChatColor.hasLegacyColors(text), "Text in a slider may not contain colors: " + text + ", instead, use primaryColor() and secondaryColor()");

		return new ColoredTextSlider(text);
	}
}