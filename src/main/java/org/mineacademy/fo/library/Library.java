package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;
import static org.mineacademy.fo.library.Util.craftPartialPath;
import static org.mineacademy.fo.library.Util.craftPath;
import static org.mineacademy.fo.library.Util.hexStringToByteArray;
import static org.mineacademy.fo.library.Util.replaceWithDots;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * An immutable representation of a Maven artifact that can be downloaded,
 * relocated and then loaded into a classloader classpath at runtime.
 *
 * @see #builder()
 */
public class Library {

	/**
	 * Direct download URLs for this library
	 */
	private final Collection<String> urls;

	/**
	 * Repository URLs for this library
	 */

	private final Collection<String> repositories;

	/**
	 * Fallback repository URLs for this library
	 */

	private final Collection<String> fallbackRepositories;

	/**
	 * Maven group ID
	 */

	private final String groupId;

	/**
	 * Maven artifact ID
	 */

	private final String artifactId;

	/**
	 * Artifact version
	 */

	private final String version;

	/**
	 * Artifact classifier
	 */

	private final String classifier;

	/**
	 * Binary SHA-256 checksum for this library's jar file
	 */
	private final byte[] checksum;

	/**
	 * Jar relocations to apply
	 */

	private final Collection<Relocation> relocations;

	/**
	 * Relative Maven path to this library's artifact
	 */

	private final String path;

	/**
	 * Relative partial Maven path to this library
	 */

	private final String partialPath;

	/**
	 * Relative path to this library's relocated jar
	 */

	private final String relocatedPath;

	/**
	 * Should this library be loaded in an isolated class loader?
	 */
	private final boolean isolatedLoad;

	/**
	 * The isolated loader id for this library
	 */

	private final String loaderId;

	/**
	 * Should transitive dependencies be resolved for this library?
	 */
	private final boolean resolveTransitiveDependencies;

	/**
	 * Transitive dependencies that would be excluded on transitive resolution
	 */

	private final Collection<ExcludedDependency> excludedTransitiveDependencies;

	/**
	 * Creates a new library.
	 *
	 * @param urls         direct download URLs
	 * @param repositories repository URLs
	 * @param groupId      Maven group ID, any {@code "{}"} is replaced with a {@code "."}
	 * @param artifactId   Maven artifact ID, any {@code "{}"} is replaced with a {@code "."}
	 * @param version      artifact version
	 * @param classifier   artifact classifier or null
	 * @param checksum     binary SHA-256 checksum or null
	 * @param relocations  jar relocations or null
	 * @param isolatedLoad isolated load for this library
	 * @param loaderId     the loader ID for this library
	 * @param resolveTransitiveDependencies transitive dependencies resolution for this library
	 * @param excludedTransitiveDependencies excluded transitive dependencies or null
	 */
	private Library(Collection<String> urls,
			Collection<String> repositories,
			Collection<String> fallbackRepositories,
			String groupId,
			String artifactId,
			String version,
			String classifier,
			byte[] checksum,
			Collection<Relocation> relocations,
			boolean isolatedLoad,
			String loaderId,
			boolean resolveTransitiveDependencies,
			Collection<ExcludedDependency> excludedTransitiveDependencies) {

		this.urls = urls != null ? Collections.unmodifiableList(new LinkedList<>(urls)) : Collections.emptyList();
		this.groupId = replaceWithDots(requireNonNull(groupId, "groupId"));
		this.artifactId = replaceWithDots(requireNonNull(artifactId, "artifactId"));
		this.version = requireNonNull(version, "version");
		this.classifier = classifier;
		this.checksum = checksum;
		this.relocations = relocations != null ? Collections.unmodifiableList(new LinkedList<>(relocations)) : Collections.emptyList();

		this.partialPath = craftPartialPath(this.artifactId, this.groupId, version);
		this.path = craftPath(this.partialPath, this.artifactId, this.version, this.classifier);

		this.repositories = repositories != null ? Collections.unmodifiableList(new LinkedList<>(repositories)) : Collections.emptyList();
		this.fallbackRepositories = fallbackRepositories != null ? Collections.unmodifiableList(new LinkedList<>(fallbackRepositories)) : Collections.emptyList();
		this.relocatedPath = this.hasRelocations() ? this.path + "-relocated-" + Math.abs(this.relocations.hashCode()) + ".jar" : null;
		this.isolatedLoad = isolatedLoad;
		this.loaderId = loaderId;
		this.resolveTransitiveDependencies = resolveTransitiveDependencies;
		this.excludedTransitiveDependencies = excludedTransitiveDependencies != null ? Collections.unmodifiableList(new LinkedList<>(excludedTransitiveDependencies)) : Collections.emptyList();
	}

	/**
	 * Gets the direct download URLs for this library.
	 *
	 * @return direct download URLs
	 */

	public Collection<String> getUrls() {
		return this.urls;
	}

	/**
	 * Gets the repository URLs for this library.
	 *
	 * @return repository URLs
	 */

	public Collection<String> getRepositories() {
		return this.repositories;
	}

	/**
	 * Gets the fallback repository URLs for this library.
	 *
	 * @return fallback repository URLs
	 */

	public Collection<String> getFallbackRepositories() {
		return this.fallbackRepositories;
	}

	/**
	 * Gets the Maven group ID for this library.
	 *
	 * @return Maven group ID
	 */

	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * Gets the Maven artifact ID for this library.
	 *
	 * @return Maven artifact ID
	 */

	public String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Gets the artifact version for this library.
	 *
	 * @return artifact version
	 */

	public String getVersion() {
		return this.version;
	}

	/**
	 * Gets the artifact classifier for this library.
	 *
	 * @return artifact classifier or null
	 */

	public String getClassifier() {
		return this.classifier;
	}

	/**
	 * Gets whether this library has an artifact classifier.
	 *
	 * @return true if library has classifier, false otherwise
	 */
	public boolean hasClassifier() {
		return this.classifier != null && !this.classifier.isEmpty();
	}

	/**
	 * Gets the binary SHA-256 checksum of this library's jar file.
	 *
	 * @return checksum or null
	 */
	public byte[] getChecksum() {
		return this.checksum;
	}

	/**
	 * Gets whether this library has a checksum.
	 *
	 * @return true if library has checksum, false otherwise
	 */
	public boolean hasChecksum() {
		return this.checksum != null;
	}

	/**
	 * Gets the jar relocations to apply to this library.
	 *
	 * @return jar relocations to apply
	 */

	public Collection<Relocation> getRelocations() {
		return this.relocations;
	}

	/**
	 * Gets whether this library has any jar relocations.
	 *
	 * @return true if library has relocations, false otherwise
	 */
	public boolean hasRelocations() {
		return !this.relocations.isEmpty();
	}

	/**
	 * Gets the relative Maven path to this library's artifact.
	 *
	 * @return relative Maven path for this library
	 */

	public String getPath() {
		return this.path;
	}

	/**
	 * Gets the relative partial Maven path to this library.
	 *
	 * @return relative partial Maven path for this library
	 */

	public String getPartialPath() {
		return this.partialPath;
	}

	/**
	 * Gets the relative path to this library's relocated jar.
	 *
	 * @return path to relocated artifact or null if it has no relocations
	 */

	public String getRelocatedPath() {
		return this.relocatedPath;
	}

	/**
	 * Is the library loaded isolated?
	 *
	 * @return true if the library is loaded isolated
	 */
	public boolean isIsolatedLoad() {
		return this.isolatedLoad;
	}

	/**
	 * Get the isolated loader ID
	 *
	 * @return the loader ID
	 */

	public String getLoaderId() {
		return this.loaderId;
	}

	/**
	 * Whether the library is a snapshot.
	 *
	 * @return whether the library is a snapshot.
	 */
	public boolean isSnapshot() {
		return this.version.endsWith("-SNAPSHOT");
	}

	/**
	 * Should transitive dependencies of this resolved
	 *
	 * @return true if the transitive dependencies of this library would be resolved
	 */
	public boolean resolveTransitiveDependencies() {
		return this.resolveTransitiveDependencies;
	}

	/**
	 * Gets the excluded dependencies during transitive dependencies resolution.
	 *
	 * @return The dependencies excluded during transitive dependencies resolution.
	 */

	public Collection<ExcludedDependency> getExcludedTransitiveDependencies() {
		return this.excludedTransitiveDependencies;
	}

	/**
	 * Gets a concise, human-readable string representation of this library.
	 *
	 * @return string representation
	 */
	@Override
	public String toString() {
		String name = this.groupId + ':' + this.artifactId + ':' + this.version;
		if (this.hasClassifier())
			name += ':' + this.classifier;

		return name;
	}

	/**
	 * Creates a new library builder.
	 *
	 * @return new library builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Due to the constructor complexity of an immutable {@link Library},
	 * instead this fluent builder is used to configure and then construct
	 * a new library.
	 */
	public static class Builder {
		/**
		 * Direct download URLs for this library
		 */
		private final Collection<String> urls = new LinkedList<>();

		/**
		 * Repository URLs for this library
		 */
		private final Collection<String> repositories = new LinkedList<>();

		/**
		 * Fallback repository URLs for this library
		 */
		private final Collection<String> fallbackRepositories = new LinkedList<>();

		/**
		 * Maven group ID
		 */
		private String groupId;

		/**
		 * Maven artifact ID
		 */
		private String artifactId;

		/**
		 * Artifact version
		 */
		private String version;

		/**
		 * Artifact classifier
		 */
		private String classifier;

		/**
		 * Binary SHA-256 checksum for this library's jar file
		 */
		private byte[] checksum;

		/**
		 * Isolated load
		 */
		private boolean isolatedLoad;

		/**
		 * Loader ID
		 */
		private String loaderId;

		/**
		 * Jar relocations to apply
		 */
		private final Collection<Relocation> relocations = new LinkedList<>();

		/**
		 * Resolve transitive dependencies
		 */
		private boolean resolveTransitiveDependencies;

		/**
		 * Resolve transitive dependencies exclusions
		 */
		private final Collection<ExcludedDependency> excludedTransitiveDependencies = new LinkedList<>();

		/**
		 * Adds a direct download URL for this library.
		 *
		 * @param url direct download URL
		 * @return this builder
		 */

		public Builder url(String url) {
			this.urls.add(requireNonNull(url, "url"));
			return this;
		}

		/**
		 * Adds a repository URL for this library.
		 * <p>Most common repositories can be found in Repositories class as constants.
		 * <p>Note that repositories should be preferably added to the {@link LibraryManager} via {@link LibraryManager#addRepository(String)}.
		 *
		 * @param url repository URL
		 * @return this builder
		 */

		public Builder repository(String url) {
			this.repositories.add(requireNonNull(url, "repository").endsWith("/") ? url : url + '/');
			return this;
		}

		/**
		 * Adds a fallback repository URL for this library. See {@link #repository(String)}.
		 *
		 * @param url fallback repository URL
		 * @return this builder
		 */

		public Builder fallbackRepository(String url) {
			this.fallbackRepositories.add(requireNonNull(url, "fallbackRepository").endsWith("/") ? url : url + '/');
			return this;
		}

		/**
		 * Sets the Maven group ID for this library.
		 * <p>
		 * To avoid issues with shading and relocation, any {@code "{}"} inside the provided groupId string
		 * is replaced with a {@code "."}.
		 *
		 * @param groupId Maven group ID
		 * @return this builder
		 */

		public Builder groupId(String groupId) {
			this.groupId = requireNonNull(groupId, "groupId");
			return this;
		}

		/**
		 * Sets the Maven artifact ID for this library.
		 * <p>
		 * To avoid issues with shading and relocation, any {@code "{}"} inside the provided artifactId string
		 * is replaced with a {@code "."}.
		 *
		 * @param artifactId Maven artifact ID
		 * @return this builder
		 */

		public Builder artifactId(String artifactId) {
			this.artifactId = requireNonNull(artifactId, "artifactId");
			return this;
		}

		/**
		 * Sets the artifact version for this library.
		 *
		 * @param version artifact version
		 * @return this builder
		 */

		public Builder version(String version) {
			this.version = requireNonNull(version, "version");
			return this;
		}

		/**
		 * Sets the artifact classifier for this library.
		 *
		 * @param classifier artifact classifier
		 * @return this builder
		 */

		public Builder classifier(String classifier) {
			this.classifier = classifier;
			return this;
		}

		/**
		 * Sets the binary SHA-256 checksum for this library.
		 *
		 * @param checksum binary SHA-256 checksum
		 * @return this builder
		 */

		public Builder checksum(byte[] checksum) {
			this.checksum = checksum;
			return this;
		}

		/**
		 * Sets the SHA-256 checksum for this library.
		 *
		 * @param checksum SHA-256 checksum
		 * @return this builder
		 */

		public Builder checksum(String checksum) {
			return checksum != null ? this.checksum(hexStringToByteArray(checksum)) : this;
		}

		/**
		 * Sets the Base64 hexadecimal bytes encoded SHA-256 checksum for this library.
		 *
		 * @param checksum Base64 binary encoded SHA-256 checksum
		 * @return this builder
		 */

		public Builder checksumFromBase64(String checksum) {
			return checksum != null ? this.checksum(Base64.getDecoder().decode(checksum)) : this;
		}

		/**
		 * Sets the isolated load for this library.
		 *
		 * @param isolatedLoad the isolated load boolean
		 * @return this builder
		 */

		public Builder isolatedLoad(boolean isolatedLoad) {
			this.isolatedLoad = isolatedLoad;
			return this;
		}

		/**
		 * Sets the loader ID for this library.
		 *
		 * @param loaderId the ID
		 * @return this builder
		 */

		public Builder loaderId(String loaderId) {
			this.loaderId = loaderId;
			return this;
		}

		/**
		 * Adds a jar relocation to apply to this library.
		 *
		 * @param relocation jar relocation to apply
		 * @return this builder
		 */

		public Builder relocate(Relocation relocation) {
			requireNonNull(relocation, "relocation");
			if (!relocation.getPattern().equals(relocation.getRelocatedPattern()))
				this.relocations.add(relocation);
			return this;
		}

		/**
		 * Adds a jar relocation to apply to this library.
		 *
		 * @param pattern          search pattern
		 * @param relocatedPattern replacement pattern
		 * @return this builder
		 */

		public Builder relocate(String pattern, String relocatedPattern) {
			return this.relocate(new Relocation(pattern, relocatedPattern));
		}

		/**
		 * Sets the transitive dependency resolution for this library.
		 *
		 * @param resolveTransitiveDependencies the transitive dependency resolution
		 * @return this builder
		 * @see #excludeTransitiveDependency(ExcludedDependency)
		 */

		public Builder resolveTransitiveDependencies(boolean resolveTransitiveDependencies) {
			this.resolveTransitiveDependencies = resolveTransitiveDependencies;
			return this;
		}

		/**
		 * Excludes transitive dependency for this library.
		 *
		 * @param excludedDependency Excluded transitive dependency
		 * @return this builder
		 * @see #resolveTransitiveDependencies(boolean)
		 */

		public Builder excludeTransitiveDependency(ExcludedDependency excludedDependency) {
			this.excludedTransitiveDependencies.add(requireNonNull(excludedDependency, "excludedDependency"));
			return this;
		}

		/**
		 * Excludes transitive dependency for this library.
		 *
		 * @param groupId Excluded transitive dependency group ID
		 * @param artifactId Excluded transitive dependency artifact ID
		 * @return this builder
		 * @see #excludeTransitiveDependency(ExcludedDependency)
		 */

		public Builder excludeTransitiveDependency(String groupId, String artifactId) {
			return this.excludeTransitiveDependency(new ExcludedDependency(groupId, artifactId));
		}

		/**
		 * Creates a new library using this builder's configuration.
		 *
		 * @return new library
		 */

		public Library build() {
			return new Library(this.urls, this.repositories, this.fallbackRepositories, this.groupId, this.artifactId, this.version, this.classifier, this.checksum, this.relocations, this.isolatedLoad, this.loaderId, this.resolveTransitiveDependencies, this.excludedTransitiveDependencies);
		}
	}
}
