package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A reflection-based wrapper around {@link URLClassLoader} for adding URLs to
 * the classpath.
 *
 * @author https://github.com/jonesdevelopment/libby
 *
 */
final class URLClassLoaderHelper extends ClassLoaderHelper {
	/**
	 * A reflected method in {@link URLClassLoader}, when invoked adds a URL to the classpath.
	 */
	private MethodHandle addURLMethodHandle = null;

	/**
	 * Creates a new URL class loader helper.
	 *
	 * @param classLoader the class loader to manage
	 * @param libraryManager the library manager used to download dependencies
	 */
	public URLClassLoaderHelper(URLClassLoader classLoader, LibraryManager libraryManager) {
		super(classLoader);
		requireNonNull(libraryManager, "libraryManager");

		try {
			final Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			this.setMethodAccessible(libraryManager, addURLMethod, "URLClassLoader#addURL(URL)",
					methodHandle -> {
						this.addURLMethodHandle = methodHandle;
					},
					instrumentation -> {
						this.addOpensWithAgent(instrumentation);
						addURLMethod.setAccessible(true);
					});
			if (this.addURLMethodHandle == null)
				this.addURLMethodHandle = MethodHandles.lookup().unreflect(addURLMethod).bindTo(classLoader);
		} catch (final Exception ex) {
			throw new RuntimeException("Couldn't initialize URLClassLoaderHelper", ex);
		}
	}

	@Override
	public void addToClasspath(URL url) {
		try {
			this.addURLMethodHandle.invokeWithArguments(requireNonNull(url, "url"));
		} catch (final Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	private void addOpensWithAgent(Instrumentation instrumentation) {
		// This is effectively calling:
		//
		// instrumentation.redefineModule(
		//     URLClassLoader.class.getModule(),
		//     Collections.emptySet(),
		//     Collections.emptyMap(),
		//     Collections.singletonMap("java.net", Collections.singleton(getClass().getModule())),
		//     Collections.emptySet(),
		//     Collections.emptyMap()
		// );
		//
		// For more information see https://docs.oracle.com/en/java/javase/16/docs/api/java.instrument/java/lang/instrument/Instrumentation.html
		try {
			final Method redefineModule = Instrumentation.class.getMethod("redefineModule", Class.forName("java.lang.Module"), Set.class, Map.class, Map.class, Set.class, Map.class);
			final Method getModule = Class.class.getMethod("getModule");
			final Map<String, Set<?>> toOpen = Collections.singletonMap("java.net", Collections.singleton(getModule.invoke(this.getClass())));
			redefineModule.invoke(instrumentation, getModule.invoke(URLClassLoader.class), Collections.emptySet(), Collections.emptyMap(), toOpen, Collections.emptySet(), Collections.emptyMap());
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
