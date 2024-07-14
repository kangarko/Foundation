package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * A reflection-based wrapper around SystemClassLoader for adding URLs to
 * the classpath.
 */
final class SystemClassLoaderHelper extends ClassLoaderHelper {

	/**
	 * A reflected method in SystemClassLoader, when invoked adds a URL to the classpath.
	 */
	private MethodHandle appendMethodHandle = null;
	private Instrumentation appendInstrumentation = null;

	/**
	 * Creates a new SystemClassLoader helper.
	 *
	 * @param classLoader the class loader to manage
	 * @param libraryManager the library manager used to download dependencies
	 */
	public SystemClassLoaderHelper(ClassLoader classLoader, LibraryManager libraryManager) {
		super(classLoader);
		requireNonNull(libraryManager, "libraryManager");

		try {
			final Method appendMethod = classLoader.getClass().getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
			this.setMethodAccessible(libraryManager, appendMethod, classLoader.getClass().getName() + "#appendToClassPathForInstrumentation(String)",
					methodHandle -> {
						this.appendMethodHandle = methodHandle;
					},
					instrumentation -> {
						this.appendInstrumentation = instrumentation;
					});
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't initialize SystemClassLoaderHelper", e);
		}
	}

	@Override
	public void addToClasspath(URL url) {
		try {
			if (this.appendInstrumentation != null)
				this.appendInstrumentation.appendToSystemClassLoaderSearch(new JarFile(url.toURI().getPath()));
			else
				this.appendMethodHandle.invokeWithArguments(url.toURI().getPath());
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
