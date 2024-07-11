package org.mineacademy.fo.library;

/**
 * Libby's utility class.
 */
final class Util {

	private Util() {
		throw new UnsupportedOperationException("Util class.");
	}

	/**
	 * Replaces the "{}" inside the provided string with a dot.
	 *
	 * @param str The string to replace
	 * @return The string with "{}" replaced
	 */

	public static String replaceWithDots(String str) {
		return str.replace("{}", ".");
	}

	/**
	 * Convert a String of hex character to a byte array
	 *
	 * @param string The string to convert
	 * @return The byte array
	 */
	public static byte[] hexStringToByteArray(String string) {
		final int len = string.length();
		final byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
			data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
					+ Character.digit(string.charAt(i + 1), 16));
		return data;
	}

	/**
	 * Constructs the partial path of a {@link Library} given its artifactId, groupId and version.
	 *
	 * @param artifactId The artifactId of the library.
	 * @param groupId The groupId of the library.
	 * @param version The version of the library.
	 * @return The partial path of the library.
	 * @see Library#getPartialPath()
	 */

	public static String craftPartialPath(String artifactId, String groupId, String version) {
		return groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/';
	}

	/**
	 * Constructs the path of a {@link Library} given its partialPath, artifactId, version and classifier.
	 *
	 * @param partialPath The partialPath of the library.
	 * @param artifactId The artifactId of the library.
	 * @param version The version of the library.
	 * @param classifier The classifier of the library. May be null.
	 * @return The path of the library.
	 * @see Library#getPath()
	 */

	public static String craftPath(String partialPath, String artifactId, String version, String classifier) {
		String path = partialPath + artifactId + '-' + version;
		if (classifier != null && !classifier.isEmpty())
			path += '-' + classifier;
		return path + ".jar";
	}
}
