package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * This class loader is a simple child of {@code URLClassLoader} that uses
 * the JVM's Extensions Class Loader as the parent instead of the system class
 * loader to provide an unpolluted classpath.
 */
final class IsolatedClassLoader extends URLClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	/**
	 * Creates a new isolated class loader for the given URLs.
	 *
	 * @param urls the URLs to add to the classpath
	 */
	public IsolatedClassLoader(URL... urls) {
		super(requireNonNull(urls, "urls"), ClassLoader.getSystemClassLoader().getParent());
	}

	/**
	 * Adds a URL to the classpath.
	 *
	 * @param url the URL to add
	 */
	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	/**
	 * Adds a path to the classpath.
	 *
	 * @param path the path to add
	 */
	public void addPath(Path path) {
		try {
			this.addURL(requireNonNull(path, "path").toUri().toURL());
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Defines and loads a class.
	 *
	 * @param name The expected binary name of the class
	 * @param classBytes An {@link InputStream} which provides the class bytes. It will be automatically closed by this
	 *                   method after being read.
	 * @return The defined class
	 * @throws IOException If an exception occurs while reading the provided {@link InputStream}
	 * @throws ClassFormatError If the bytes provided by the {@link InputStream} doesn't contain valid class
	 */
	public Class<?> defineClass(String name, InputStream classBytes) throws IOException, ClassFormatError {
		final byte[] bytes = readAllBytes(classBytes);
		return super.defineClass(name, bytes, 0, bytes.length);
	}

	/**
	 * Reads all bytes, and safely closes {@link InputStream}. Always has buffer length 4 KB.
	 *
	 * @return The all bytes of {@link InputStream}
	 * @param inputStream {@link InputStream} that would be read
	 * @throws IOException If {@link InputStream} has been closed, or bytes cannot be read, or other I/O error occurs.
	 * @see InputStream#read(byte[], int, int)
	 */
	private static byte[] readAllBytes(InputStream inputStream) throws IOException {
		final int bufLen = 4 * 0x400; // 4KB
		final byte[] buf = new byte[bufLen];
		int readLen;
		IOException exception = null;

		try {
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
					outputStream.write(buf, 0, readLen);

				return outputStream.toByteArray();
			}
		} catch (final IOException e) {
			exception = e;
			throw e;
		} finally {
			if (exception == null)
				inputStream.close();
			else
				try {
					inputStream.close();
				} catch (final IOException e) {
					exception.addSuppressed(e);
				}
		}
	}
}
