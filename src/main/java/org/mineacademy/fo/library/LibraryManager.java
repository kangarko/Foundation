package org.mineacademy.fo.library;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.NetworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A runtime dependency manager for java applications.
 * <p>
 * The library manager can resolve a dependency jar through the configured
 * Maven repositories, download it into a local cache and then
 * load it into the classloader classpath.
 * <p>
 * Transitive dependencies for a library aren't downloaded automatically and
 * must be explicitly loaded like every other library.
 * <p>
 *
 * @author https://github.com/jonesdevelopment/libby
 *
 * @see Library
 */
public abstract class LibraryManager {

	/**
	 * Sonatype OSS repository URL.
	 */
	public static final String REPO_SONATYPE = "https://oss.sonatype.org/content/groups/public/";

	/**
	 * JitPack repository URL.
	 */
	public static final String REPO_JITPACK = "https://jitpack.io/";

	/**
	 * Directory where downloaded library jars are saved to
	 */
	protected final Path saveDirectory;

	/**
	 * Maven repositories used to resolve artifacts
	 */
	protected final Set<String> repositories = new LinkedHashSet<>();

	/**
	 * Lazily-initialized helper for transitive dependencies resolution
	 */
	protected TransitiveDependencyHelper transitiveDependencyHelper;

	/**
	 * Global isolated class loader for libraries
	 */
	protected final IsolatedClassLoader globalIsolatedClassLoader = new IsolatedClassLoader();

	/**
	 * Map of isolated class loaders and theirs id
	 */
	protected final Map<String, IsolatedClassLoader> isolatedLibraries = new HashMap<>();

	/**
	 * Creates a new library manager.
	 *
	 * @param logAdapter    logging adapter
	 * @param dataDirectory data directory
	 */
	protected LibraryManager(Path dataDirectory) {
		this.saveDirectory = dataDirectory.toAbsolutePath().resolve("lib");

		this.addRepository("https://repo1.maven.org/maven2/");
	}

	/**
	 * Adds a file to the classloader classpath.
	 *
	 * @param file the file to add
	 */
	protected abstract void addToClasspath(Path file);

	/**
	 * Get the global isolated class loader for libraries
	 *
	 * @return the isolated class loader
	 */

	public IsolatedClassLoader getGlobalIsolatedClassLoader() {
		return this.globalIsolatedClassLoader;
	}

	/**
	 * Get the isolated class loader of the library
	 *
	 * @param loaderId the id of the loader
	 * @return the isolated class loader associated with the provided id
	 */
	@Nullable
	public IsolatedClassLoader getIsolatedClassLoaderById(String loaderId) {
		return this.isolatedLibraries.get(loaderId);
	}

	/**
	 * Gets the currently added repositories used to resolve artifacts.
	 * <p>
	 * For each library this list is traversed to download artifacts after the
	 * direct download URLs have been attempted.
	 *
	 * @return current repositories
	 */

	public Collection<String> getRepositories() {
		List<String> urls;
		synchronized (this.repositories) {
			urls = new LinkedList<>(this.repositories);
		}

		return Collections.unmodifiableList(urls);
	}

	/**
	 * Adds a repository URL to this library manager.
	 * <p>
	 * Artifacts will be resolved using this repository when attempts to locate
	 * the artifact through previously added repositories are all unsuccessful.
	 *
	 * @param url repository URL to add
	 */
	public void addRepository(String url) {
		final String repo = requireNonNull(url, "url").endsWith("/") ? url : url + '/';
		synchronized (this.repositories) {
			this.repositories.add(repo);
		}
	}

	/**
	 * Adds the current user's local Maven repository.
	 */
	public void addMavenLocal() {
		this.addRepository(Paths.get(System.getProperty("user.home")).resolve(".m2/repository").toUri().toString());
	}

	/**
	 * Adds the Sonatype OSS repository.
	 */
	public void addSonatype() {
		this.addRepository(REPO_SONATYPE);
	}

	/**
	 * Adds the JitPack repository.
	 */
	public void addJitPack() {
		this.addRepository(REPO_JITPACK);
	}

	/**
	 * Gets all the possible download URLs for this library. Entries are
	 * ordered by direct download URLs first and then repository download URLs.
	 * <br>This method also resolves SNAPSHOT artifacts URLs.
	 *
	 * @param library the library to resolve
	 * @return download URLs
	 */

	public Collection<String> resolveLibrary(Library library) {
		final Set<String> urls = new LinkedHashSet<>(requireNonNull(library, "library").getUrls());
		final boolean snapshot = library.isSnapshot();
		final Collection<String> repos = this.resolveRepositories(library);

		for (final String repository : repos)
			if (snapshot) {
				final String url = this.resolveSnapshot(repository, library);
				if (url != null)
					urls.add(repository + url);
			} else
				urls.add(repository + library.getPath());

		return Collections.unmodifiableSet(urls);
	}

	/**
	 * Resolves the repository URLs for this library.
	 *
	 * @param library the library to resolve repositories for
	 * @return the resolved repositories
	 */
	public Collection<String> resolveRepositories(Library library) {
		return Stream.of(
				library.getRepositories(),
				this.getRepositories())
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Resolves the URL of the artifact of a snapshot library.
	 *
	 * @param repository The repository to query for snapshot information
	 * @param library    The library
	 * @return The URl of the artifact of a snapshot library or null if no information could be gathered from the
	 * provided repository
	 */
	@Nullable
	protected String resolveSnapshot(String repository, Library library) {
		final String mavenMetadata = repository.startsWith("file") ? "maven-metadata-local.xml" : "maven-metadata.xml";
		final String url = requireNonNull(repository, "repository") + requireNonNull(library, "library").getPartialPath() + mavenMetadata;
		try {
			final URLConnection connection = new URL(requireNonNull(url, "url")).openConnection();

			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", NetworkUtil.HTTP_USER_AGENT);

			try (InputStream in = connection.getInputStream()) {
				return this.getURLFromMetadata(in, library);
			}
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);

		} catch (final IOException ex) {
			if (ex instanceof FileNotFoundException)
				Common.error(ex, "File not found: " + url);
			else if (ex instanceof SocketTimeoutException)
				Common.error(ex, "Connect timed out: " + url);
			else if (ex instanceof UnknownHostException)
				Common.error(ex, "Unknown host: " + url);
			else
				Common.error(ex, "Unexpected IOException");

			return null;
		}
	}

	/**
	 * Gets the URL of the artifact of a snapshot library from the provided InputStream, which should be opened to the
	 * library's maven-metadata.xml.
	 *
	 * @param inputStream The InputStream opened to the library's maven-metadata.xml
	 * @param library     The library
	 * @return The URl of the artifact of a snapshot library or null if no information could be gathered from the
	 * provided inputStream
	 * @throws IOException If any IO errors occur
	 */
	@Nullable
	protected String getURLFromMetadata(InputStream inputStream, Library library) throws IOException {
		requireNonNull(inputStream, "inputStream");
		requireNonNull(library, "library");

		String version = library.getVersion();
		try {
			// This reads the maven-metadata.xml file and gets the snapshot info from the <snapshot> tag.
			// Example tag:
			// <snapshot>
			//     <timestamp>20220617.013635</timestamp>
			//     <buildNumber>12</buildNumber>
			// </snapshot>

			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			final Document doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();

			final NodeList nodes = doc.getElementsByTagName("snapshot");
			if (nodes.getLength() == 0)
				return null;
			final Node snapshot = nodes.item(0);
			if (snapshot.getNodeType() != Node.ELEMENT_NODE)
				return null;
			final Node localCopyNode = ((Element) snapshot).getElementsByTagName("localCopy").item(0);
			if (localCopyNode == null || localCopyNode.getNodeType() != Node.ELEMENT_NODE) {
				// Resolve with snapshot number instead of base name
				final Node timestampNode = ((Element) snapshot).getElementsByTagName("timestamp").item(0);
				if (timestampNode == null || timestampNode.getNodeType() != Node.ELEMENT_NODE)
					return null;
				final Node buildNumberNode = ((Element) snapshot).getElementsByTagName("buildNumber").item(0);
				if (buildNumberNode == null || buildNumberNode.getNodeType() != Node.ELEMENT_NODE)
					return null;
				final Node timestampChild = timestampNode.getFirstChild();
				if (timestampChild == null || timestampChild.getNodeType() != Node.TEXT_NODE)
					return null;
				final Node buildNumberChild = buildNumberNode.getFirstChild();
				if (buildNumberChild == null || buildNumberChild.getNodeType() != Node.TEXT_NODE)
					return null;
				final String timestamp = timestampChild.getNodeValue();
				final String buildNumber = buildNumberChild.getNodeValue();

				version = library.getVersion();
				// Call .substring(...) only on versions ending in "-SNAPSHOT".
				// It should never happen that a snapshot version doesn't end in "-SNAPSHOT", but better be sure
				if (version.endsWith("-SNAPSHOT"))
					version = version.substring(0, version.length() - "-SNAPSHOT".length());

				version = version + '-' + timestamp + '-' + buildNumber;
			}
		} catch (ParserConfigurationException | SAXException ex) {
			Common.error(ex, "Invalid maven-metadata.xml");

			return null;
		}
		return Library.craftPath(library.getPartialPath(), library.getArtifactId(), version, library.getClassifier());
	}

	/**
	 * Downloads a library jar and returns the contents as a byte array.
	 *
	 * @param url the URL to the library jar
	 * @return downloaded jar as byte array or null if nothing was downloaded
	 */
	protected byte[] downloadLibrary(String url) {
		try {
			//Common.log("Downloading library " + url);

			final URLConnection connection = new URL(requireNonNull(url, "url")).openConnection();

			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", NetworkUtil.HTTP_USER_AGENT);

			try (InputStream in = connection.getInputStream()) {
				int len;
				final byte[] buf = new byte[8192];
				final ByteArrayOutputStream out = new ByteArrayOutputStream();

				try {
					while ((len = in.read(buf)) != -1)
						out.write(buf, 0, len);
				} catch (final SocketTimeoutException ex) {
					Common.error(ex, "Download timed out: " + connection.getURL());
					return null;
				}

				return out.toByteArray();
			}
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		} catch (final IOException ex) {
			if (ex instanceof FileNotFoundException)
				Common.error(ex, "File not found: " + url);
			else if (ex instanceof SocketTimeoutException)
				Common.error(ex, "Connect timed out: " + url);
			else if (ex instanceof UnknownHostException)
				Common.error(ex, "Unknown host: " + url);
			else
				Common.error(ex, "Unexpected IOException");

			return null;
		}
	}

	/**
	 * Downloads a library jar to the save directory if it doesn't already
	 * exist (snapshot libraries are always re-downloaded) and returns
	 * the local file path.
	 * <p>
	 * If the library has a checksum, it will be compared against the
	 * downloaded jar's checksum to verify the integrity of the download. If
	 * the checksums don't match, a warning is generated and the next download
	 * URL is attempted.
	 * <p>
	 * Checksum comparison is ignored if the library doesn't have a checksum
	 * or if the library jar already exists in the save directory.
	 * <p>
	 * Most of the time it is advised to use loadLibrary(Library)
	 * instead of this method because this one is only concerned with
	 * downloading the jar and returning the local path. It's usually more
	 * desirable to download the jar and add it to the classloader's classpath in
	 * one operation.
	 * <p>
	 * Once the library is downloaded, relocations are applied automatically and
	 * its returned the path to the relocated jar.
	 *
	 * @param library the library to download
	 * @return local file path to library
	 */

	public Path downloadLibrary(Library library) {
		final Path file = this.saveDirectory.resolve(requireNonNull(library, "library").getPath());
		if (Files.exists(file)) {
			// Early return only if library isn't a snapshot, since snapshot libraries are always re-downloaded
			if (!library.isSnapshot())
				return file;

			// Delete the file since the Files.move call down below will fail if it exists
			try {
				Files.delete(file);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		final Collection<String> urls = this.resolveLibrary(library);
		if (urls.isEmpty())
			throw new RuntimeException("Library '" + library + "' couldn't be resolved, add a repository");

		MessageDigest md = null;
		if (library.hasChecksum())
			try {
				md = MessageDigest.getInstance("SHA-256");
			} catch (final NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

		final Path out = file.resolveSibling(file.getFileName() + ".tmp");
		out.toFile().deleteOnExit();

		try {
			Files.createDirectories(file.getParent());

			for (final String url : urls) {
				final byte[] bytes = this.downloadLibrary(url);
				if (bytes == null)
					continue;

				if (md != null) {
					final byte[] checksum = md.digest(bytes);
					if (!Arrays.equals(checksum, library.getChecksum())) {
						Common.logFramed(
								"*** INVALID CHECKSUM ***",
								" Library :  " + library, "URL :  " + url,
								" Expected :  " + Base64.getEncoder().encodeToString(library.getChecksum()),
								" Actual :  " + Base64.getEncoder().encodeToString(checksum));
						continue;
					}
				}

				Files.write(out, bytes);
				Files.move(out, file);

				return file;
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				Files.deleteIfExists(out);
			} catch (final IOException ignored) {
			}
		}

		throw new RuntimeException("Failed to download library '" + library + "'");
	}

	/**
	 * Resolves and loads transitive libraries for a given library. This method ensures that
	 * all libraries on which the provided library depends are properly loaded.
	 *
	 * @param library the primary library for which transitive libraries need to be resolved and loaded.
	 * @throws NullPointerException if the provided library is null.
	 * @see #loadLibrary(Library)
	 */
	protected void resolveTransitiveLibraries(Library library) {
		requireNonNull(library, "library");

		synchronized (this) {
			if (this.transitiveDependencyHelper == null)
				this.transitiveDependencyHelper = new TransitiveDependencyHelper(this, this.saveDirectory);
		}

		for (final Library transitiveLibrary : this.transitiveDependencyHelper.findTransitiveLibraries(library))
			this.loadLibrary(transitiveLibrary, true);
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	public void loadLibrary(String groupId, String artifactId, String version) {
		this.loadLibrary(new Library(groupId, artifactId, version, true), false);
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 *
	 * @param library
	 */
	public void loadLibrary(Library library) {
		this.loadLibrary(library, false);
	}

	/*
	 * Loads the library instantly.
	 */
	private void loadLibrary(Library library, boolean isTransitiveNow) {

		//if (!isTransitiveNow)
		//	Common.log("Loading library " + library);

		final Path file = this.downloadLibrary(requireNonNull(library, "library"));

		if (library.isResolveTransitiveDependencies())
			this.resolveTransitiveLibraries(library);

		this.addToClasspath(file);
	}

	/**
	 * Returns an input stream for reading the specified resource.
	 *
	 * @param path the path to the resource
	 * @return input stream for the resource
	 */
	@Nullable
	protected InputStream getResourceAsStream(String path) {
		return this.getClass().getClassLoader().getResourceAsStream(path);
	}
}
