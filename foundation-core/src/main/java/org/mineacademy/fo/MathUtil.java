package org.mineacademy.fo;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.mineacademy.fo.exception.CalculatorException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for mathematical operations.
 *
 * This is a platform-neutral class, which is extended by "MathUtil" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MathUtil {

	/**
	 * Formatter that transforms whole numbers into whole decimals with 1 decimal point
	 */
	private final static DecimalFormat oneDigitFormat = new DecimalFormat("#.#");

	/**
	 * Formatter that transforms whole numbers into whole decimals with 2 decimal points
	 */
	private final static DecimalFormat twoDigitsFormat = new DecimalFormat("#.##");

	/**
	 * Formatter that transforms whole numbers into whole decimals with 3 decimal points
	 */
	private final static DecimalFormat threeDigitsFormat = new DecimalFormat("#.###");

	/**
	 * Formatter that transforms whole numbers into whole decimals with 5 decimal points
	 */
	private final static DecimalFormat fiveDigitsFormat = new DecimalFormat("#.#####");

	/**
	 * Holds all valid roman numbers
	 */
	private final static NavigableMap<Integer, String> romanNumbers = new TreeMap<>();

	// Load the roman numbers
	static {
		romanNumbers.put(1000, "M");
		romanNumbers.put(900, "CM");
		romanNumbers.put(500, "D");
		romanNumbers.put(400, "CD");
		romanNumbers.put(100, "C");
		romanNumbers.put(90, "XC");
		romanNumbers.put(50, "L");
		romanNumbers.put(40, "XL");
		romanNumbers.put(10, "X");
		romanNumbers.put(9, "IX");
		romanNumbers.put(5, "V");
		romanNumbers.put(4, "IV");
		romanNumbers.put(1, "I");
	}

	// ----------------------------------------------------------------------------------------------------
	// Number manipulation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return the value or the minimum if the value is less than the minimum.
	 *
	 * @param minimum
	 * @param value
	 * @return
	 */
	public static int atLeast(int minimum, int value) {
		return value < minimum ? minimum : value;
	}

	/**
	 * Return the highest integer in the given number array.
	 *
	 * @param numbers
	 * @return
	 */
	public static int max(int... numbers) {
		return Arrays.stream(numbers).max().getAsInt();
	}

	/**
	 * See {@link Math#floor(double)}.
	 *
	 * @param d1
	 * @return
	 */
	public static long floor(final double d1) {
		final long i = (long) d1;

		return d1 >= i ? i : i - 1;
	}

	/**
	 * See {@link Math#ceil(double)}.
	 *
	 * @param f1
	 * @return
	 */
	public static long ceiling(final double f1) {
		final long i = (long) f1;

		return f1 >= i ? i : i - 1;
	}

	/**
	 * See {@link #range(int, int, int)}.
	 *
	 * @param value the real value
	 * @param min   the min limit
	 * @param max   the max limit
	 * @return the value in range
	 */
	public static double range(final double value, final double min, final double max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * Get a value in range. If the value is < min, returns min, if it is > max, returns max.
	 *
	 * @param value the real value
	 * @param min   the min limit
	 * @param max   the max limit
	 * @return the value in range
	 */
	public static int range(final int value, final int min, final int max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * Return the average double of the given values.
	 *
	 * @param values
	 * @return
	 */
	public static double average(final Collection<Double> values) {
		return average(values.toArray(new Double[values.size()]));
	}

	/**
	 * Return the average double of the given values.
	 *
	 * @param values
	 * @return
	 */
	public static double average(final Double... values) {
		ValidCore.checkBoolean(values.length > 0, "No values given!");

		double sum = 0;

		for (final double val : values)
			sum += val;

		return formatTwoDigitsD(sum / values.length);
	}

	// ----------------------------------------------------------------------------------------------------
	// Formatting
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Formats the given number into one digit.
	 *
	 * @param value
	 * @return
	 */
	public static String formatOneDigit(final double value) {
		return oneDigitFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into one digit as a double.
	 *
	 * @param value
	 * @return
	 */
	public static double formatOneDigitD(final double value) {
		ValidCore.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(oneDigitFormat.format(value).replace(",", "."));
	}

	/**
	 * Formats the given number into two digits.
	 *
	 * @param value
	 * @return
	 */
	public static String formatTwoDigits(final double value) {
		return twoDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into two digits as a double.
	 *
	 * @param value
	 * @return
	 */
	public static double formatTwoDigitsD(final double value) {
		ValidCore.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(twoDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * Formats the given number into three digits.
	 *
	 * @param value
	 * @return
	 */
	public static String formatThreeDigits(final double value) {
		return threeDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into three digits as a double.
	 *
	 * @param value
	 * @return
	 */
	public static double formatThreeDigitsD(final double value) {
		ValidCore.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(threeDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * Formats the given number into five digits.
	 *
	 * @param value
	 * @return
	 */
	public static String formatFiveDigits(final double value) {
		return fiveDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into five digits as a double.
	 *
	 * @param value
	 * @return
	 */
	public static double formatFiveDigitsD(final double value) {
		ValidCore.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(fiveDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * Return a roman number representation of the given number.
	 *
	 * @param number
	 * @return
	 */
	public static String toRoman(final int number) {
		if (number == 0)
			return "0"; // Actually, Romans did not know zero lol

		final int literal = romanNumbers.floorKey(number);

		if (number == literal)
			return romanNumbers.get(number);

		return romanNumbers.get(literal) + toRoman(number - literal);
	}

	// ----------------------------------------------------------------------------------------------------
	// Calculating
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Calculate the value of a mathematical expression passed as a string. e.g. 5*(4-2) returns... let me check!
	 * <p>
	 * The expression can contain addition, subtraction, multiplication, division, parentheses, and exponents.
	 * <p>
	 * Examples of expressions:
	 * <ul>
	 *   <li>calculate("2 + 3 * 4") returns 14.0</li>
	 *   <li>calculate("-(2 + 3) ^ 2") returns -25.0</li>
	 *   <li>calculate("(2 + 3) * 2") returns 10.0</li>
	 * </ul>
	 *
	 * @param expression the mathematical expression to evaluate
	 * @return the calculated result as a double
	 * @throws CalculatorException if an unexpected character or invalid input is encountered
	 */
	public static double calculate(final String expression) {
		class Parser {
			int pos = -1, c;

			void eatChar() {
				this.c = ++this.pos < expression.length() ? expression.charAt(this.pos) : -1;
			}

			void eatSpace() {
				while (Character.isWhitespace(this.c))
					this.eatChar();
			}

			double parse() {
				this.eatChar();

				final double v = this.parseExpression();

				if (this.c != -1)
					throw new CalculatorException("Unexpected: " + (char) this.c);

				return v;
			}

			double parseExpression() {
				double v = this.parseTerm();

				for (;;) {
					this.eatSpace();

					if (this.c == '+') { // addition
						this.eatChar();
						v += this.parseTerm();
					} else if (this.c == '-') { // subtraction
						this.eatChar();
						v -= this.parseTerm();
					} else
						return v;

				}
			}

			double parseTerm() {
				double v = this.parseFactor();

				for (;;) {
					this.eatSpace();

					if (this.c == '/') { // division
						this.eatChar();
						v /= this.parseFactor();
					} else if (this.c == '*' || this.c == '(') { // multiplication
						if (this.c == '*')
							this.eatChar();
						v *= this.parseFactor();
					} else
						return v;
				}
			}

			double parseFactor() {
				double v;
				boolean negate = false;

				this.eatSpace();

				if (this.c == '+' || this.c == '-') { // unary plus & minus
					negate = this.c == '-';
					this.eatChar();
					this.eatSpace();
				}

				if (this.c == '(') { // brackets
					this.eatChar();
					v = this.parseExpression();
					if (this.c == ')')
						this.eatChar();
				} else { // numbers
					final StringBuilder sb = new StringBuilder();

					while (this.c >= '0' && this.c <= '9' || this.c == '.') {
						sb.append((char) this.c);
						this.eatChar();
					}

					if (sb.length() == 0)
						throw new CalculatorException("Unexpected: " + (char) this.c);

					v = Double.parseDouble(sb.toString());
				}
				this.eatSpace();
				if (this.c == '^') { // exponentiation
					this.eatChar();
					v = Math.pow(v, this.parseFactor());
				}
				if (negate)
					v = -v; // unary minus is applied after exponentiation; e.g. -3^2=-9
				return v;
			}
		}
		return new Parser().parse();
	}
}