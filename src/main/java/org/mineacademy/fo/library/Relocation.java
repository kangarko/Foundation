package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;
import static org.mineacademy.fo.library.Util.replaceWithDots;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Relocations are used to describe a search and replace pattern for renaming
 * packages in a library jar for the purpose of preventing namespace conflicts
 * with other java applications that bundle their own version of the same library.
 */
public class Relocation {
	/**
	 * Search pattern
	 */

	private final String pattern;

	/**
	 * Replacement pattern
	 */

	private final String relocatedPattern;

	/**
	 * Classes and resources to include
	 */

	private final Collection<String> includes;

	/**
	 * Classes and resources to exclude
	 */

	private final Collection<String> excludes;

	/**
	 * Creates a new relocation.
	 *
	 * @param pattern          search pattern
	 * @param relocatedPattern replacement pattern
	 * @param includes         classes and resources to include
	 * @param excludes         classes and resources to exclude
	 */
	public Relocation(String pattern, String relocatedPattern, Collection<String> includes, Collection<String> excludes) {
		this.pattern = replaceWithDots(requireNonNull(pattern, "pattern"));
		this.relocatedPattern = replaceWithDots(requireNonNull(relocatedPattern, "relocatedPattern"));
		this.includes = includes != null ? Collections.unmodifiableSet(includes.stream()
				.map(Util::replaceWithDots)
				.collect(Collectors.toSet())) : Collections.emptySet();
		this.excludes = excludes != null ? Collections.unmodifiableSet(excludes.stream()
				.map(Util::replaceWithDots)
				.collect(Collectors.toSet())) : Collections.emptySet();
	}

	/**
	 * Creates a new relocation with empty includes and excludes.
	 *
	 * @param pattern          search pattern
	 * @param relocatedPattern replacement pattern
	 */
	public Relocation(String pattern, String relocatedPattern) {
		this(pattern, relocatedPattern, null, null);
	}

	/**
	 * Gets the search pattern.
	 *
	 * @return pattern to search
	 */

	public String getPattern() {
		return this.pattern;
	}

	/**
	 * Gets the replacement pattern.
	 *
	 * @return pattern to replace with
	 */

	public String getRelocatedPattern() {
		return this.relocatedPattern;
	}

	/**
	 * Gets included classes and resources.
	 *
	 * @return classes and resources to include
	 */

	public Collection<String> getIncludes() {
		return this.includes;
	}

	/**
	 * Gets excluded classes and resources.
	 *
	 * @return classes and resources to exclude
	 */

	public Collection<String> getExcludes() {
		return this.excludes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || this.getClass() != o.getClass())
			return false;

		Relocation that = (Relocation) o;

		if (!this.pattern.equals(that.pattern) || !this.relocatedPattern.equals(that.relocatedPattern) || !this.includes.equals(that.includes))
			return false;
		return this.excludes.equals(that.excludes);
	}

	@Override
	public int hashCode() {
		int result = this.pattern.hashCode();
		result = 31 * result + this.relocatedPattern.hashCode();
		result = 31 * result + this.includes.hashCode();
		result = 31 * result + this.excludes.hashCode();
		return result;
	}

	/**
	 * Creates a new relocation builder.
	 *
	 * @return new relocation builder
	 */

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Provides an alternative method of creating a {@link Relocation}. This
	 * builder may be more intuitive for configuring relocations that also have
	 * any includes or excludes.
	 */
	public static class Builder {
		/**
		 * Search pattern
		 */
		private String pattern;

		/**
		 * Replacement pattern
		 */
		private String relocatedPattern;

		/**
		 * Classes and resources to include
		 */
		private final Collection<String> includes = new LinkedList<>();

		/**
		 * Classes and resources to exclude
		 */
		private final Collection<String> excludes = new LinkedList<>();

		/**
		 * Sets the search pattern.
		 *
		 * @param pattern pattern to search
		 * @return this builder
		 */

		public Builder pattern(String pattern) {
			this.pattern = requireNonNull(pattern, "pattern");
			return this;
		}

		/**
		 * Sets the replacement pattern.
		 *
		 * @param relocatedPattern pattern to replace with
		 * @return this builder
		 */

		public Builder relocatedPattern(String relocatedPattern) {
			this.relocatedPattern = requireNonNull(relocatedPattern, "relocatedPattern");
			return this;
		}

		/**
		 * Adds a class or resource to be included.
		 *
		 * @param include class or resource to include
		 * @return this builder
		 */

		public Builder include(String include) {
			this.includes.add(requireNonNull(include, "include"));
			return this;
		}

		/**
		 * Adds a class or resource to be excluded.
		 *
		 * @param exclude class or resource to exclude
		 * @return this builder
		 */

		public Builder exclude(String exclude) {
			this.excludes.add(requireNonNull(exclude, "exclude"));
			return this;
		}

		/**
		 * Creates a new relocation using this builder's configuration.
		 *
		 * @return new relocation
		 */

		public Relocation build() {
			return new Relocation(this.pattern, this.relocatedPattern, this.includes, this.excludes);
		}
	}
}
