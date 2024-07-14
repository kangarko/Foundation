package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;
import static org.mineacademy.fo.library.Util.replaceWithDots;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A reflection-based helper for relocating library jars. It automatically
 * downloads and invokes Luck's Jar Relocator to perform jar relocations.
 *
 */
@SuppressWarnings("resource")
final class RelocationHelper {
	/**
	 * me.lucko.jarrelocator.JarRelocator class name for reflections
	 */
	private static final String JAR_RELOCATOR_CLASS = replaceWithDots("me{}lucko{}jarrelocator{}JarRelocator");

	/**
	 * me.lucko.jarrelocator.Relocation class name for reflections
	 */
	private static final String RELOCATION_CLASS = replaceWithDots("me{}lucko{}jarrelocator{}Relocation");

	/**
	 * Reflected constructor for creating new jar relocator instances
	 */
	private final Constructor<?> jarRelocatorConstructor;

	/**
	 * Reflected method for running a jar relocator
	 */
	private final Method jarRelocatorRunMethod;

	/**
	 * Reflected constructor for creating relocation instances
	 */
	private final Constructor<?> relocationConstructor;

	/**
	 * Creates a new relocation helper using the provided library manager to
	 * download the dependencies required for runtime relocation.
	 *
	 * @param libraryManager the library manager used to download dependencies
	 */
	public RelocationHelper(LibraryManager libraryManager) {
		requireNonNull(libraryManager, "libraryManager");

		final IsolatedClassLoader classLoader = new IsolatedClassLoader();

		// ObjectWeb ASM Commons
		classLoader.addPath(libraryManager.downloadLibrary(
				Library.builder()
						.groupId("org{}ow2{}asm")
						.artifactId("asm-commons")
						.version("9.7")
						.checksumFromBase64("OJvCR5WOBJ/JoECNOYySxtNwwYA1EgOV1Muh2dkwS3o=")
						.fallbackRepository(Repositories.MAVEN_CENTRAL)
						.build()));

		// ObjectWeb ASM
		classLoader.addPath(libraryManager.downloadLibrary(
				Library.builder()
						.groupId("org{}ow2{}asm")
						.artifactId("asm")
						.version("9.7")
						.checksumFromBase64("rfRtXjSUC98Ujs3Sap7o7qlElqcgNP9xQQZrPupcTp0=")
						.fallbackRepository(Repositories.MAVEN_CENTRAL)
						.build()));

		// Luck's Jar Relocator
		classLoader.addPath(libraryManager.downloadLibrary(
				Library.builder()
						.groupId("me{}lucko")
						.artifactId("jar-relocator")
						.version("1.7")
						.checksumFromBase64("b30RhOF6kHiHl+O5suNLh/+eAr1iOFEFLXhwkHHDu4I=")
						.fallbackRepository(Repositories.MAVEN_CENTRAL)
						.build()));

		try {
			final Class<?> jarRelocatorClass = classLoader.loadClass(JAR_RELOCATOR_CLASS);
			final Class<?> relocationClass = classLoader.loadClass(RELOCATION_CLASS);

			// me.lucko.jarrelocator.JarRelocator(File, File, Collection)
			this.jarRelocatorConstructor = jarRelocatorClass.getConstructor(File.class, File.class, Collection.class);

			// me.lucko.jarrelocator.JarRelocator#run()
			this.jarRelocatorRunMethod = jarRelocatorClass.getMethod("run");

			// me.lucko.jarrelocator.Relocation(String, String, Collection, Collection)
			this.relocationConstructor = relocationClass.getConstructor(String.class, String.class, Collection.class, Collection.class);
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Invokes the jar relocator to process the input jar and generate an
	 * output jar with the provided relocation rules applied.
	 *
	 * @param in          input jar
	 * @param out         output jar
	 * @param relocations relocations to apply
	 */
	public void relocate(Path in, Path out, Collection<Relocation> relocations) {
		requireNonNull(in, "in");
		requireNonNull(out, "out");
		requireNonNull(relocations, "relocations");

		try {
			final List<Object> rules = new LinkedList<>();
			for (final Relocation relocation : relocations)
				rules.add(this.relocationConstructor.newInstance(
						relocation.getPattern(),
						relocation.getRelocatedPattern(),
						relocation.getIncludes(),
						relocation.getExcludes()));

			this.jarRelocatorRunMethod.invoke(this.jarRelocatorConstructor.newInstance(in.toFile(), out.toFile(), rules));
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
