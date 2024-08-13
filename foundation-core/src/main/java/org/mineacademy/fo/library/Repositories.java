package org.mineacademy.fo.library;

/**
 * Class containing URLs of public repositories.
 */
final class Repositories {

	/**
	 * Maven Central repository URL.
	 */
	public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

	/**
	 * Sonatype OSS repository URL.
	 */
	public static final String SONATYPE = "https://oss.sonatype.org/content/groups/public/";

	/**
	 * Bintray JCenter repository URL.
	 */
	public static final String JCENTER = "https://jcenter.bintray.com/";

	/**
	 * JitPack repository URL.
	 */
	public static final String JITPACK = "https://jitpack.io/";

	private Repositories() {
		throw new UnsupportedOperationException("Private constructor");
	}
}
