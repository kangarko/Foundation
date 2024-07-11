package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * A reflection-based helper for resolving transitive dependencies. It automatically
 * downloads Libby Maven Resolver to resolve transitive dependencies.
 *
 * @author https://github.com/jonesdevelopment/libby
 */
final class TransitiveDependencyHelper {

	/**
	 * com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector class name for reflections
	 */
	private static final String TRANSITIVE_DEPENDENCY_COLLECTOR_CLASS = "com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector";

	/**
	 * org.eclipse.aether.artifact.Artifact class name for reflections
	 */
	private static final String ARTIFACT_CLASS = "org.eclipse.aether.artifact.Artifact";

	/**
	 * TransitiveDependencyCollector class instance, used in {@link #findTransitiveLibraries(Library)}
	 */
	private final Object transitiveDependencyCollectorObject;

	/**
	 * Reflected method for resolving transitive dependencies
	 */
	private final Method resolveTransitiveDependenciesMethod;

	/**
	 * Reflected getter methods of Artifact class
	 */
	private final Method artifactGetGroupIdMethod, artifactGetArtifactIdMethod, artifactGetVersionMethod, artifactGetBaseVersionMethod, artifactGetClassifierMethod;

	/**
	 * LibraryManager instance, used in {@link #findTransitiveLibraries(Library)}
	 */
	private final LibraryManager libraryManager;

	/**
	 * Creates a new transitive dependency helper using the provided library manager to
	 * download the dependencies required for transitive dependency resolution in runtime.
	 *
	 * @param libraryManager the library manager used to download dependencies
	 * @param saveDirectory  the directory where all transitive dependencies would be saved
	 */
	@SuppressWarnings("resource")
	public TransitiveDependencyHelper(LibraryManager libraryManager, Path saveDirectory) {
		requireNonNull(libraryManager, "libraryManager");
		this.libraryManager = libraryManager;

		final IsolatedClassLoader classLoader = new IsolatedClassLoader();
		final Library resolver = new Library("com.alessiodp.libby.maven.resolver", "libby-maven-resolver", "1.0.1");

		resolver.setChecksumFromBase64("EmsSUwjtqSeYTt8WEw7LPI/5Yz8bWSxf23XcdLEM7dk=");
		resolver.setRepository("https://repo.alessiodp.com/releases");

		classLoader.addPath(libraryManager.downloadLibrary(resolver));

		try {
			final Class<?> transitiveDependencyCollectorClass = classLoader.loadClass(TRANSITIVE_DEPENDENCY_COLLECTOR_CLASS);
			final Class<?> artifactClass = classLoader.loadClass(ARTIFACT_CLASS);

			// com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector(Path)
			final Constructor<?> constructor = transitiveDependencyCollectorClass.getConstructor(Path.class);
			constructor.setAccessible(true);
			this.transitiveDependencyCollectorObject = constructor.newInstance(saveDirectory);
			// com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector#findTransitiveDependencies(String, String, String, String, Stream<String>)
			this.resolveTransitiveDependenciesMethod = transitiveDependencyCollectorClass.getMethod("findTransitiveDependencies", String.class, String.class, String.class, String.class, Stream.class);
			this.resolveTransitiveDependenciesMethod.setAccessible(true);
			// org.eclipse.aether.artifact.Artifact#getGroupId()
			this.artifactGetGroupIdMethod = artifactClass.getMethod("getGroupId");
			// org.eclipse.aether.artifact.Artifact#getArtifactId()
			this.artifactGetArtifactIdMethod = artifactClass.getMethod("getArtifactId");
			// org.eclipse.aether.artifact.Artifact#getVersion()
			this.artifactGetVersionMethod = artifactClass.getMethod("getVersion");
			// org.eclipse.aether.artifact.Artifact#getBaseVersion()
			this.artifactGetBaseVersionMethod = artifactClass.getMethod("getBaseVersion");
			// org.eclipse.aether.artifact.Artifact#getClassifier()
			this.artifactGetClassifierMethod = artifactClass.getMethod("getClassifier");
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finds and returns a collection of transitive libraries for a given library.
	 * <p>
	 * This method fetches the transitive dependencies of the provided library using reflection-based
	 * interaction with the underlying transitive dependency collector. The method ensures to filter out
	 * any excluded transitive dependencies as specified by the provided library.
	 * </p>
	 * <p>
	 * Note: The method merges the repositories from both the library manager and the given library
	 * for dependency resolution. It also clones all relocations into transitive libraries.
	 * </p>
	 *
	 * @param library The primary library for which transitive dependencies need to be found.
	 * @return A collection of {@link Library} objects representing the transitive libraries
	 * excluding the ones marked as excluded in the provided library.
	 * @throws RuntimeException If there's any exception during the reflection-based operations.
	 */

	public Collection<Library> findTransitiveLibraries(Library library) {
		final List<Library> transitiveLibraries = new ArrayList<>();

		final Collection<String> globalRepositories = this.libraryManager.getRepositories();
		final Collection<String> libraryRepositories = library.getRepositories();

		if (globalRepositories.isEmpty() && libraryRepositories.isEmpty())
			throw new IllegalArgumentException("No repositories have been added before resolving transitive dependencies");

		final Stream<String> repositories = this.libraryManager.resolveRepositories(library).stream();
		try {
			final Collection<?> resolvedArtifacts = (Collection<?>) this.resolveTransitiveDependenciesMethod.invoke(this.transitiveDependencyCollectorObject,
					library.getGroupId(),
					library.getArtifactId(),
					library.getVersion(),
					library.getClassifier(),
					repositories);
			for (final Object resolved : resolvedArtifacts) {
				final Entry<?, ?> resolvedEntry = (Entry<?, ?>) resolved;
				final Object artifact = resolvedEntry.getKey();
				@Nullable
				String repository = (String) resolvedEntry.getValue();

				final String groupId = (String) this.artifactGetGroupIdMethod.invoke(artifact);
				final String artifactId = (String) this.artifactGetArtifactIdMethod.invoke(artifact);
				final String baseVersion = (String) this.artifactGetBaseVersionMethod.invoke(artifact);
				final String classifier = (String) this.artifactGetClassifierMethod.invoke(artifact);

				if (library.getGroupId().equals(groupId) && library.getArtifactId().equals(artifactId))
					continue;

				final Library lib = new Library(groupId, artifactId, baseVersion);

				lib.setLoaderId(library.getLoaderId());

				if (classifier != null && !classifier.isEmpty())
					lib.setClassifier(classifier);

				if (repository != null) {
					// Construct direct download URL

					// Add ending "/" if missing
					if (!repository.endsWith("/"))
						repository = repository + '/';

					// TODO Uncomment the line below once LibraryManager#resolveLibrary stops resolving snapshots
					//      for every repository before trying direct URLs
					// Make sure the repository is added as fallback if the dependency isn't found at the constructed URL
					// libraryBuilder.fallbackRepository(repository);

					// For snapshots, getVersion() returns version-timestamp-buildNumber instead of version-SNAPSHOT
					final String version = (String) this.artifactGetVersionMethod.invoke(artifact);

					final String partialPath = Library.craftPartialPath(artifactId, groupId, baseVersion);
					final String path = Library.craftPath(partialPath, artifactId, version, classifier);

					lib.setUrl(repository + path);

				} else
					library.getRepositories().forEach(lib::setRepository);

				transitiveLibraries.add(lib);
			}
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}

		return Collections.unmodifiableCollection(transitiveLibraries);
	}
}
