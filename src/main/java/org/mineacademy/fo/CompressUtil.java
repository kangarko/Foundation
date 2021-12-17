package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.bukkit.ChatColor;

import lombok.SneakyThrows;

/**
 * A simple class to compress or decompress text
 */
public final class CompressUtil {

	/**
	 * Converts the text byte array into compressed data in base64 string format
	 *
	 * @param text
	 * @return
	 */
	@SneakyThrows
	public static String compressB64(String text) {
		return new String(Base64.getEncoder().encode(_compress(text.replace(String.valueOf(ChatColor.COLOR_CHAR), "%CLRCHAR%"))), StandardCharsets.UTF_8);
	}

	/**
	 * Converts the compressed data from base64 into uncompressed text
	 *
	 * @param b64Compressed
	 * @return
	 */
	@SneakyThrows
	public static String decompressB64(String b64Compressed) {
		return _decompress(Base64.getDecoder().decode(b64Compressed)).replace("%CLRCHAR%", String.valueOf(ChatColor.COLOR_CHAR));
	}

	/**
	 * Converts the text into compressed data
	 *
	 * @param text
	 * @return
	 */
	@SneakyThrows
	public static byte[] _compress(String text) {
		return _compress(text.getBytes());
	}

	/**
	 * Converts the text byte array into compressed data
	 *
	 * @param byteArray
	 * @return
	 */
	@SneakyThrows
	public static byte[] _compress(byte[] byteArray) {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		try (DeflaterOutputStream deflater = new DeflaterOutputStream(stream)) {
			deflater.write(byteArray);
		}

		return stream.toByteArray();
	}

	/**
	 * Converts the given compressed data back into text
	 *
	 * @param compressedText
	 * @return
	 */
	@SneakyThrows
	public static String _decompress(byte[] compressedText) {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		try (OutputStream inflater = new InflaterOutputStream(stream)) {
			inflater.write(compressedText);
		}

		return new String(stream.toByteArray(), StandardCharsets.UTF_8);
	}
}