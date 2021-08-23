package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

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
	 * @throws IOException
	 */
	@SneakyThrows
	public static String compressB64(String text) {
		return text;

		//return new String(Base64.getEncoder().encode(compress(text.replace(String.valueOf(ChatColor.COLOR_CHAR), "%CLRCHAR%"))), StandardCharsets.UTF_8);
	}

	/**
	 * Converts the compressed data from base64 into uncompressed text
	 *
	 * @param b64Compressed
	 * @return
	 * @throws IOException
	 */
	@SneakyThrows
	public static String decompressB64(String b64Compressed) {
		return b64Compressed;

		//return decompress(Base64.getDecoder().decode(b64Compressed)).replace("%CLRCHAR%", String.valueOf(ChatColor.COLOR_CHAR));
	}

	/**
	 * Converts the text into compressed data
	 *
	 * @param text
	 * @return
	 * @throws IOException
	 */
	@SneakyThrows
	public static byte[] compress(String text) {
		return compress(text.getBytes());
	}

	/**
	 * Converts the text byte array into compressed data
	 *
	 * @param byteArray
	 * @return
	 * @throws IOException
	 */
	@SneakyThrows
	public static byte[] compress(byte[] byteArray) {
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
	 * @throws IOException
	 */
	@SneakyThrows
	public static String decompress(byte[] compressedText) {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		try (OutputStream inflater = new InflaterOutputStream(stream)) {
			inflater.write(compressedText);
		}

		return new String(stream.toByteArray(), StandardCharsets.UTF_8);
	}
}