package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.NonNull;

/**
 * An representation of a Maven artifact that can be downloaded,
 * relocated and then loaded into a classloader classpath at runtime.
 *
 * @author https://github.com/jonesdevelopment/libby
 */
@Getter
public final class Library {

	/**
	 * Direct download URLs for this library
	 */
	private List<String> urls = new ArrayList<>();

	/**
	 * Repository URLs for this library
	 */
	private List<String> repositories = new ArrayList<>();

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
	@Nullable
	private String classifier;

	/**
	 * Binary SHA-256 checksum for this library's jar file
	 */
	private byte[] checksum;

	/**
	 * The isolated loader id for this library
	 */
	@Nullable
	private String loaderId;

	/**
	 * Relative Maven path to this library's artifact
	 */
	private final String path;

	/**
	 * Relative partial Maven path to this library
	 */
	private final String partialPath;

	/**
	 * Whether to resolve transitive dependencies for this library
	 */
	private boolean resolveTransitiveDependencies;

	/**
	 * Creates a new library.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	public Library(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version, false);
	}

	/**
	 * Creates a new library.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param resolveTransitiveDependencies
	 */
	public Library(String groupId, String artifactId, String version, boolean resolveTransitiveDependencies) {
		this(null, null, groupId, artifactId, version, null, null, null, resolveTransitiveDependencies);
	}

	/**
	 * Creates a new library.
	 *
	 * @param urls
	 * @param repositories
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param classifier
	 * @param checksum
	 * @param loaderId
	 * @param resolveTransitiveDependencies
	 */
	public Library(List<String> urls, List<String> repositories, @NonNull String groupId, @NonNull String artifactId, @NonNull String version, String classifier, byte[] checksum, String loaderId, boolean resolveTransitiveDependencies) {
		this.urls = urls != null ? urls : new ArrayList<>();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.classifier = classifier;
		this.checksum = checksum;
		this.partialPath = craftPartialPath(this.artifactId, this.groupId, version);
		this.path = craftPath(this.partialPath, this.artifactId, this.version, this.classifier);
		this.repositories = repositories != null ? repositories : new ArrayList<>();
		this.loaderId = loaderId;
		this.resolveTransitiveDependencies = resolveTransitiveDependencies;
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
	 * Gets whether this library has a checksum.
	 *
	 * @return true if library has checksum, false otherwise
	 */
	public boolean hasChecksum() {
		return this.checksum != null;
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
	 * Adds a direct download URL for this library.
	 *
	 * @param url direct download URL
	 */
	public void setUrl(String url) {
		this.urls.add(requireNonNull(url, "url"));
	}

	/**
	 * Adds a repository URL for this library.
	 * <p>Most common repositories can be found in repositories.
	 * <p>Note that repositories should be preferably added to the {@link LibraryManager} via {@link LibraryManager#addRepository(String)}.
	 *
	 * @param url repository URL
	 */
	public void setRepository(String url) {
		this.repositories.add(requireNonNull(url, "repository").endsWith("/") ? url : url + '/');
	}

	/**
	 * Sets the Maven group ID for this library.
	 *
	 * @param groupId Maven group ID
	 */
	public void setGroupId(String groupId) {
		this.groupId = requireNonNull(groupId, "groupId");
	}

	/**
	 * Sets the Maven artifact ID for this library.
	 *
	 * @param artifactId Maven artifact ID
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = requireNonNull(artifactId, "artifactId");
	}

	/**
	 * Sets the artifact version for this library.
	 *
	 * @param version artifact version
	 */
	public void setVersion(String version) {
		this.version = requireNonNull(version, "version");
	}

	/**
	 * Sets the artifact classifier for this library.
	 *
	 * @param classifier artifact classifier
	 */
	public void setClassifier(@Nullable String classifier) {
		this.classifier = classifier;
	}

	/**
	 * Sets the binary SHA-256 checksum for this library.
	 *
	 * @param checksum binary SHA-256 checksum
	 */
	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}

	/**
	 * Sets the SHA-256 checksum for this library.
	 *
	 * @param checksum SHA-256 checksum
	 */
	public void setChecksum(@Nullable String checksum) {
		if (checksum != null) {
			final int len = checksum.length();
			final byte[] data = new byte[len / 2];
			for (int i = 0; i < len; i += 2)
				data[i / 2] = (byte) ((Character.digit(checksum.charAt(i), 16) << 4)
						+ Character.digit(checksum.charAt(i + 1), 16));

			this.setChecksum(data);

		}
	}

	/**
	 * Sets the Base64 hexadecimal bytes encoded SHA-256 checksum for this library.
	 *
	 * @param checksum Base64 binary encoded SHA-256 checksum
	 */
	public void setChecksumFromBase64(@Nullable String checksum) {
		if (checksum != null)
			this.setChecksum(Base64.getDecoder().decode(checksum));
	}

	/**
	 * Sets the loader ID for this library.
	 *
	 * @param loaderId the ID
	 */
	public void setLoaderId(@Nullable String loaderId) {
		this.loaderId = loaderId;
	}

	/**
	 * Sets whether to resolve transitive dependencies for this library.
	 *
	 * @param resolveTransitiveDependencies
	 */
	public void setResolveTransitiveDependencies(boolean resolveTransitiveDependencies) {
		this.resolveTransitiveDependencies = resolveTransitiveDependencies;
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

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Library && ((Library) obj).toString().equals(this.toString());
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
	public static String craftPartialPath(@NonNull String artifactId, @NonNull String groupId, @NonNull String version) {
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
	public static String craftPath(@NonNull String partialPath, @NonNull String artifactId, @NonNull String version, String classifier) {
		String path = partialPath + artifactId + '-' + version;

		if (classifier != null && !classifier.isEmpty())
			path += '-' + classifier;

		return path + ".jar";
	}
}
