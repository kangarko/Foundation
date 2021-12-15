package org.mineacademy.fo.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * A frustration-free implementation of {@link Configuration} which saves all files in Yaml.
 * Note that this implementation is not synchronized.
 */
public final class SimpleYaml extends FileConfiguration {

	private static final String COMMENT_PREFIX = "# ";
	private static final String BLANK_CONFIG = "{}\n";

	private final DumperOptions yamlOptions = new DumperOptions();
	private final Representer yamlRepresenter = new YamlRepresenter();

	private final Yaml yaml;

	public SimpleYaml() {

		// Load options only if available
		if (ReflectionUtil.isClassAvailable("org.yaml.snakeyaml.LoaderOptions")) {
			Yaml yaml;

			try {
				final LoaderOptions loaderOptions = new LoaderOptions();
				loaderOptions.setMaxAliasesForCollections(512);

				yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions, loaderOptions);

			} catch (final NoSuchMethodError ex) {
				yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);
			}

			this.yaml = yaml;
		}

		else
			this.yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);
	}

	/**
	 * Removes all keys from our map
	 */
	public void clear() {
		this.map.clear();
	}

	/**
	 * Return the internal map with all value-key pairs stored in the memory
	 *
	 * @deprecated potentially dangerous
	 * @return
	 */
	@Deprecated
	public Map<String, Object> getMap() {
		return this.map;
	}

	@Override
	public String saveToString() {
		return this.saveToString(getValues(false));
	}

	public String saveToString(Map<String, Object> values) {
		yamlOptions.setIndent(2);
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yamlOptions.setWidth(4096); // Foundation: Do not wrap long lines

		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		final String header = buildHeader();
		String dump = yaml.dump(values);

		if (dump.equals(BLANK_CONFIG)) {
			dump = "";
		}

		return header + dump;
	}

	@Override
	public void loadFromString(String contents) throws InvalidConfigurationException {
		Validate.notNull(contents, "Contents cannot be null");

		Map<?, ?> input;
		try {
			//loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE); // SPIGOT-5881: Not ideal, but was default pre SnakeYAML 1.26

			input = (Map<?, ?>) yaml.load(contents);
		} catch (final YAMLException e) {
			throw new InvalidConfigurationException(e);
		} catch (final ClassCastException e) {
			throw new InvalidConfigurationException("Top level is not a Map.");
		}

		final String header = parseHeader(contents);
		if (header.length() > 0) {
			options().header(header);
		}

		if (input != null) {
			convertMapsToSections(input, this);
		}
	}

	protected void convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
		for (final Map.Entry<?, ?> entry : input.entrySet()) {
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof Map) {
				convertMapsToSections((Map<?, ?>) value, section.createSection(key));
			} else {
				section.set(key, value);
			}
		}
	}

	protected String parseHeader(String input) {
		final String[] lines = input.split("\r?\n", -1);
		final StringBuilder result = new StringBuilder();
		boolean readingHeader = true;
		boolean foundHeader = false;

		for (int i = 0; (i < lines.length) && (readingHeader); i++) {
			final String line = lines[i];

			if (line.startsWith(COMMENT_PREFIX)) {
				if (i > 0) {
					result.append("\n");
				}

				if (line.length() > COMMENT_PREFIX.length()) {
					result.append(line.substring(COMMENT_PREFIX.length()));
				}

				foundHeader = true;
			} else if ((foundHeader) && (line.length() == 0)) {
				result.append("\n");
			} else if (foundHeader) {
				readingHeader = false;
			}
		}

		return result.toString();
	}

	@Override
	protected String buildHeader() {
		final String header = options().header();

		if (options().copyHeader()) {
			final Configuration def = getDefaults();

			if ((def != null) && (def instanceof FileConfiguration)) {
				final FileConfiguration filedefaults = (FileConfiguration) def;
				final String defaultsHeader = ReflectionUtil.invoke("buildHeader", filedefaults);

				if ((defaultsHeader != null) && (defaultsHeader.length() > 0)) {
					return defaultsHeader;
				}
			}
		}

		if (header == null) {
			return "";
		}

		final StringBuilder builder = new StringBuilder();
		final String[] lines = header.split("\r?\n", -1);
		boolean startedHeader = false;

		for (int i = lines.length - 1; i >= 0; i--) {
			builder.insert(0, "\n");

			if ((startedHeader) || (lines[i].length() != 0)) {
				builder.insert(0, lines[i]);
				builder.insert(0, COMMENT_PREFIX);
				startedHeader = true;
			}
		}

		return builder.toString();
	}

	/**
	 * Creates a new {@link SimpleYaml}, loading from the given reader.
	 * <p>
	 * Any errors loading the Configuration will be logged and then ignored.
	 * If the specified input is not a valid config, a blank config will be
	 * returned.
	 *
	 * @param reader input
	 * @return resulting configuration
	 * @throws IllegalArgumentException Thrown if stream is null
	 */

	public static SimpleYaml loadConfiguration(Reader reader) {
		Validate.notNull(reader, "Stream cannot be null");

		final SimpleYaml config = new SimpleYaml();

		try {
			config.load(reader);
		} catch (final IOException ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load configuration from stream", ex);
		} catch (final InvalidConfigurationException ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load configuration from stream", ex);
		}

		return config;
	}

	/**
	 * Loads YAML configuration from file, failing if anything happens or the file does not exist
	 *
	 *
	 * @param file
	 * @return
	 * @throws RuntimeException
	 */
	public static SimpleYaml loadConfiguration(File file) throws RuntimeException {
		Valid.checkNotNull(file, "File is null!");
		Valid.checkBoolean(file.exists(), "File " + file.getName() + " does not exists");

		final SimpleYaml conf = new SimpleYaml();

		try {
			if (file.exists())
				checkFileForKnownErrors(file);

			conf.load(file);

		} catch (final FileNotFoundException ex) {
			throw new IllegalArgumentException("Configuration file missing: " + file.getName(), ex);

		} catch (final IOException ex) {
			throw new IllegalArgumentException("IO exception opening " + file.getName(), ex);

		} catch (final InvalidConfigurationException ex) {
			throw new IllegalArgumentException("Malformed YAML file " + file.getName() + " - use services like yaml-online-parser.appspot.com to check and fix it", ex);

		} catch (final Throwable t) {
			throw new IllegalArgumentException("Error reading YAML file " + file.getName(), t);
		}

		return conf;
	}

	/*
	 * Check file for known errors
	 */
	private static void checkFileForKnownErrors(File file) throws IllegalArgumentException {
		int lineNumber = 0;

		for (final String line : FileUtil.readLines(file)) {
			lineNumber++;

			if (line.contains("[*]") && !line.trim().startsWith("#"))
				throw new IllegalArgumentException("Found [*] in your .yml file " + file + " line " + lineNumber + ". Please replace it with ['*'] instead.");
		}
	}

	/**
	 * Attempts to load a yaml configuration from the given path inside of your plugin's JAR
	 *
	 * @param internalFileName
	 * @return
	 */
	public static SimpleYaml loadInternalConfiguration(String internalFileName) {
		final List<String> lines = FileUtil.getInternalResource(internalFileName);
		Valid.checkNotNull(lines, "Failed getting internal configuration from " + internalFileName);

		final SimpleYaml yaml = new SimpleYaml();

		try {
			yaml.loadFromString(String.join("\n", lines));

		} catch (final Exception ex) {
			Common.error(ex, "Failed to load inbuilt config " + internalFileName);
		}

		return yaml;
	}
}
