package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;
import static org.mineacademy.fo.library.Util.replaceWithDots;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.mineacademy.fo.Common;

import sun.misc.Unsafe;

/**
 * An abstract class for reflection-based wrappers around class loaders for adding
 * URLs to the classpath.
 */
@SuppressWarnings("restriction")
abstract class ClassLoaderHelper {
	/**
	 * System property to set to "true" to disable the Unsafe method for initializing the class loader helper.
	 */
	public static final String SYSTEM_PROPERTY_DISABLE_UNSAFE = "libby.classloaders.unsafeDisabled";

	/**
	 * System property to set to "true" to disable the Java agent method for initializing the class loader helper.
	 */
	public static final String SYSTEM_PROPERTY_DISABLE_JAVA_AGENT = "libby.classloaders.javaAgentDisabled";

	/**
	 * Environment variable to set to "true" to disable the Unsafe method for initializing the class loader helper.
	 */
	public static final String ENV_VAR_DISABLE_UNSAFE = "LIBBY_CLASSLOADERS_UNSAFE_DISABLED";

	/**
	 * Environment variable to set to "true" to disable the Java agent method for initializing the class loader helper.
	 */
	public static final String ENV_VAR_DISABLE_JAVA_AGENT = "LIBBY_CLASSLOADERS_JAVA_AGENT_DISABLED";

	/**
	 * net.bytebuddy.agent.ByteBuddyAgent class name for reflections
	 */
	private static final String BYTE_BUDDY_AGENT_CLASS = replaceWithDots("net{}bytebuddy{}agent{}ByteBuddyAgent");

	/**
	 * java.lang.Module methods since we build against Java 8
	 */
	private static final Method getModuleMethod, addOpensMethod, getNameMethod;

	static {
		Method getModule = null, addOpens = null, getName = null;
		try {
			final Class<?> moduleClass = Class.forName("java.lang.Module");
			getModule = Class.class.getMethod("getModule");
			addOpens = moduleClass.getMethod("addOpens", String.class, moduleClass);
			getName = moduleClass.getMethod("getName");
		} catch (final Exception ignored) {
		} finally {
			getModuleMethod = getModule;
			addOpensMethod = addOpens;
			getNameMethod = getName;
		}
	}

	/**
	 * Unsafe class instance. Used in {@link #getPrivilegedMethodHandle(Method)}.
	 */
	private static final Unsafe theUnsafe;

	static {
		Unsafe unsafe = null; // Used to make theUnsafe field final

		// getDeclaredField("theUnsafe") is not used to avoid breakage on JVMs with changed field name
		for (final Field f : Unsafe.class.getDeclaredFields())
			try {
				if (f.getType() == Unsafe.class && Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					unsafe = (Unsafe) f.get(null);
				}
			} catch (final Exception ignored) {
			}
		theUnsafe = unsafe;
	}

	/**
	 * Cached {@link Instrumentation} instance. Used by {@link #initInstrumentation(LibraryManager)}.
	 */
	private static volatile Instrumentation cachedInstrumentation;

	/**
	 * The class loader being managed by this helper.
	 */
	protected final ClassLoader classLoader;

	/**
	 * Creates a new class loader helper.
	 *
	 * @param classLoader the class loader to manage
	 */
	public ClassLoaderHelper(ClassLoader classLoader) {
		this.classLoader = requireNonNull(classLoader, "classLoader");
	}

	/**
	 * Adds a URL to the class loader's classpath.
	 *
	 * @param url the URL to add
	 */
	public abstract void addToClasspath(URL url);

	/**
	 * Adds a path to the class loader's classpath.
	 *
	 * @param path the path to add
	 */
	public void addToClasspath(Path path) {
		try {
			this.addToClasspath(requireNonNull(path, "path").toUri().toURL());
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Sets the method accessible using reflections, the Unsafe class or a java agent loaded at runtime.
	 * <p>The provided consumers are mutually exclusive, i.e. only 1 of the provided consumers will run (if run).
	 *
	 * @param libraryManager the {@link LibraryManager}
	 * @param method the method to set accessible
	 * @param methodSignature the signature of the method, used in error messages
	 * @param methodHandleConsumer a {@link Consumer} which might get called with a {@link MethodHandle} to the method
	 * @param instrumentationConsumer a {@link Consumer} which might get called with an {@link Instrumentation} instance
	 */
	protected void setMethodAccessible(LibraryManager libraryManager, Method method, String methodSignature, Consumer<MethodHandle> methodHandleConsumer, Consumer<Instrumentation> instrumentationConsumer) {
		if (Modifier.isPublic(method.getModifiers()))
			return; // Already public

		try {
			// Try to open the method's module to avoid warnings
			openModule(method.getDeclaringClass());
		} catch (final Exception ignored) {
		}

		try {
			// In Java 8 calling setAccessible(true) is enough
			method.setAccessible(true);
			return;
		} catch (final Exception e) {
			this.handleInaccessibleObjectException(e, methodSignature);
		}

		Exception unsafeException = null; // Used below in error messages handling
		unsafe:
		if (theUnsafe != null && this.canUseUnsafe()) {
			MethodHandle methodHandle;
			try {
				methodHandle = this.getPrivilegedMethodHandle(method).bindTo(this.classLoader);
			} catch (final Exception e) {
				unsafeException = e; // Save exception for later
				break unsafe; // An exception occurred, don't continue
			}
			methodHandleConsumer.accept(methodHandle);
			return;
		}

		Exception javaAgentException = null; // Used below in error messages handling
		javaAgent:
		if (this.canUseJavaAgent()) {
			Instrumentation instrumentation;
			try {
				instrumentation = this.initInstrumentation(libraryManager);
			} catch (final Exception e) {
				javaAgentException = e; // Save exception for later
				break javaAgent; // An exception occurred, don't continue
			}
			try {
				// instrumentationConsumer might try to set the method accessible
				instrumentationConsumer.accept(instrumentation);
				return;
			} catch (final Exception e) {
				this.handleInaccessibleObjectException(e, methodSignature);
			}
		}

		// Couldn't init, print errors and throw a RuntimeException
		if (unsafeException != null)
			Common.error(unsafeException, "Cannot set accessible " + methodSignature + " using unsafe");
		if (javaAgentException != null)
			Common.error(javaAgentException, "Cannot set accessible " + methodSignature + " using java agent");

		final String packageName = method.getDeclaringClass().getPackage().getName();
		String moduleName = null;
		try {
			moduleName = (String) getNameMethod.invoke(getModuleMethod.invoke(method.getDeclaringClass()));
		} catch (final Exception ignored) {
			// Don't throw an exception in case module reflections failed
		}
		if (moduleName != null)
			Common.warning("Cannot set accessible " + methodSignature + ", if you are using Java 9+ try to add the following option to your java command: --add-opens " + moduleName + "/" + packageName + "=ALL-UNNAMED");
		else
			// In case the try-and-catch above failed, should never happen
			Common.warning("Cannot set accessible " + methodSignature);

		throw new RuntimeException("Cannot set accessible " + methodSignature);
	}

	private void handleInaccessibleObjectException(Exception exception, String methodSignature) {
		// InaccessibleObjectException has been added in Java 9
		if (!exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException"))
			throw new RuntimeException("Cannot set accessible " + methodSignature, exception);
	}

	/**
	 * Opens the module of the provided class using reflections.
	 *
	 * @param toOpen The class
	 * @throws Exception if an error occurs
	 */
	protected static void openModule(Class<?> toOpen) throws Exception {
		//
		// Snippet originally from lucko (Luck) <luck@lucko.me>, who used it in his own class loader
		//
		// This is a workaround used to maintain Java 9+ support with reflections
		// Thanks to this you will be able to run this class loader with Java 8+

		// This is effectively calling:
		//
		// toOpen.getModule().addOpens(
		//     toOpen.getPackage().getName(),
		//     ClassLoaderHelper.class.getModule()
		// );
		//
		// We use reflection since we build against Java 8.

		final Object urlClassLoaderModule = getModuleMethod.invoke(toOpen);
		final Object thisModule = getModuleMethod.invoke(ClassLoaderHelper.class);

		addOpensMethod.invoke(urlClassLoaderModule, toOpen.getPackage().getName(), thisModule);
	}

	/**
	 * Try to get a MethodHandle for the given method.
	 *
	 * @param method the method to get the handle for
	 * @return the method handle
	 */
	protected MethodHandle getPrivilegedMethodHandle(Method method) {
		// The Unsafe class is used to get a privileged MethodHandles.Lookup instance.

		// Looking for MethodHandles.Lookup#IMPL_LOOKUP private static field
		// getDeclaredField("IMPL_LOOKUP") is not used to avoid breakage on JVMs with changed field name
		for (final Field trustedLookup : MethodHandles.Lookup.class.getDeclaredFields()) {
			if (trustedLookup.getType() != MethodHandles.Lookup.class || !Modifier.isStatic(trustedLookup.getModifiers()) || trustedLookup.isSynthetic())
				continue;

			try {
				final MethodHandles.Lookup lookup = (MethodHandles.Lookup) theUnsafe.getObject(theUnsafe.staticFieldBase(trustedLookup), theUnsafe.staticFieldOffset(trustedLookup));
				return lookup.unreflect(method);
			} catch (final Exception ignored) {
				// Unreflect went wrong, trying the next field
			}
		}

		// Every field has been tried
		throw new RuntimeException("Cannot get privileged method handle.");
	}

	/**
	 * Load ByteBuddy agent and return an {@link Instrumentation} instance.
	 *
	 * @param libraryManager the library manager used to download dependencies
	 * @return an {@link Instrumentation} instance
	 * @throws Exception if an error occurs
	 */
	protected Instrumentation initInstrumentation(LibraryManager libraryManager) throws Exception {
		final Instrumentation instr = cachedInstrumentation;
		if (instr != null)
			return instr;

		// To open the class-loader's module we need permissions.
		// Try to add a java agent at runtime (specifically, ByteBuddy's agent) and use it to open the module,
		// since java agents should have such permission.

		// Download ByteBuddy's agent and load it through an IsolatedClassLoader
		final IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
		try {
			isolatedClassLoader.addPath(libraryManager.downloadLibrary(
					Library.builder()
							.groupId("net{}bytebuddy")
							.artifactId("byte-buddy-agent")
							.version("1.12.1")
							.checksumFromBase64("mcCtBT9cljUEniB5ESpPDYZMfVxEs1JRPllOiWTP+bM=")
							.fallbackRepository(Repositories.MAVEN_CENTRAL)
							.build()));

			final Class<?> byteBuddyAgent = isolatedClassLoader.loadClass(BYTE_BUDDY_AGENT_CLASS);

			// This is effectively calling:
			//
			// Instrumentation instrumentation = ByteBuddyAgent.install();
			//
			// For more information see https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html

			final Instrumentation instrumentation = (Instrumentation) byteBuddyAgent.getMethod("install").invoke(null);
			cachedInstrumentation = instrumentation;
			return instrumentation;
		} finally {
			try {
				isolatedClassLoader.close();
			} catch (final Exception ignored) {
			}
		}
	}

	/**
	 * Checks if the Unsafe method can be used to initialize the class loader helper.
	 *
	 * @return {@code true} if either the system property {@link #SYSTEM_PROPERTY_DISABLE_UNSAFE} or the environment
	 *         variable {@link #ENV_VAR_DISABLE_UNSAFE} are set to {@code "true"}, {@code false} otherwise.
	 * @see #setMethodAccessible(LibraryManager, Method, String, Consumer, Consumer)
	 */
	protected boolean canUseUnsafe() {
		return !Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_DISABLE_UNSAFE)) && !Boolean.parseBoolean(System.getenv(ENV_VAR_DISABLE_UNSAFE));
	}

	/**
	 * Checks if the Java agent method can be used to initialize the class loader helper.
	 *
	 * @return {@code true} if either the system property {@link #SYSTEM_PROPERTY_DISABLE_JAVA_AGENT} or the environment
	 *         variable {@link #ENV_VAR_DISABLE_JAVA_AGENT} are set to {@code "true"}, {@code false} otherwise.
	 * @see #setMethodAccessible(LibraryManager, Method, String, Consumer, Consumer)
	 */
	protected boolean canUseJavaAgent() {
		return !Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_DISABLE_JAVA_AGENT)) && !Boolean.parseBoolean(System.getenv(ENV_VAR_DISABLE_JAVA_AGENT));
	}
}
