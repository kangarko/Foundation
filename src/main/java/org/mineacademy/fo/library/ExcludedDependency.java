package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;
import static org.mineacademy.fo.library.Util.replaceWithDots;

/**
 * Represents a dependency to exclude during transitive dependency resolution for a library.
 */
public final class ExcludedDependency {
	/**
	 * Maven group ID
	 */

	private final String groupId;

	/**
	 * Maven artifact ID
	 */

	private final String artifactId;

	/**
	 * Creates a new {@link ExcludedDependency}
	 *
	 * @param groupId    Maven group ID
	 * @param artifactId Maven artifact ID
	 */
	public ExcludedDependency(String groupId, String artifactId) {
		this.groupId = replaceWithDots(requireNonNull(groupId, "groupId"));
		this.artifactId = replaceWithDots(requireNonNull(artifactId, "artifactId"));
	}

	/**
	 * Gets the Maven group ID for this excluded dependency
	 *
	 * @return Maven group ID
	 */

	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * Gets the Maven artifact ID for this excluded dependency
	 *
	 * @return Maven artifact ID
	 */

	public String getArtifactId() {
		return this.artifactId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || this.getClass() != o.getClass())
			return false;

		final ExcludedDependency that = (ExcludedDependency) o;

		if (!this.groupId.equals(that.groupId))
			return false;
		return this.artifactId.equals(that.artifactId);
	}

	@Override
	public int hashCode() {
		int result = this.groupId.hashCode();
		result = 31 * result + this.artifactId.hashCode();
		return result;
	}
}
