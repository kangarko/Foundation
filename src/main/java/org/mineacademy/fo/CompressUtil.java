package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CompressUtil {

	/**
	 * Compress the given String into a byte array
	 *
	 * @param string
	 * @return the byte array, or null if string is empty
	 */
	@SneakyThrows
	public byte[] compress(@NonNull final String string) {
		if (string.length() == 0)
			return null;

		final ByteArrayOutputStream obj = new ByteArrayOutputStream();
		final GZIPOutputStream gzip = new GZIPOutputStream(obj);

		gzip.write(string.getBytes("UTF-8"));
		gzip.flush();
		gzip.close();

		return obj.toByteArray();
	}

	/**
	 * Turn the compressed byte array into a String
	 *
	 * @param compressed
	 * @return the decompressed string
	 */
	@SneakyThrows
	public String decompress(@NonNull final byte[] compressed) {
		final StringBuilder outStr = new StringBuilder();

		if (compressed.length == 0)
			return "";

		if (isCompressed(compressed)) {
			final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

			String line;

			while ((line = bufferedReader.readLine()) != null)
				outStr.append(line);

		} else
			outStr.append(compressed);

		return outStr.toString();
	}

	/*
	 * Check if the given byte array has been compressed
	 */
	private boolean isCompressed(final byte[] compressed) {
		return compressed[0] == (byte) GZIPInputStream.GZIP_MAGIC && compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
	}
}