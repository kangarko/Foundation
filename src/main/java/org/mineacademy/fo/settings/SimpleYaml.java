package org.mineacademy.fo.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
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
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.ReflectionUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import lombok.Getter;

/**
 * A frustration-free implementation of {@link Configuration} which saves all files in Yaml.
 * Note that this implementation is not synchronized.
 */
public class SimpleYaml extends FileConfiguration {

	protected static final String COMMENT_PREFIX = "# ";
	protected static final String BLANK_CONFIG = "{}\n";

	@Getter
	private final DumperOptions yamlOptions = new DumperOptions();

	//@Getter
	//private final LoaderOptions loaderOptions = new LoaderOptions();

	@Getter
	private final Representer yamlRepresenter = new YamlRepresenter();

	private final Yaml yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);

	@NotNull
	@Override
	public String saveToString() {
		yamlOptions.setIndent(2);
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yamlOptions.setWidth(4096); // Foundation: Do not wrap long lines

		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		final String header = buildHeader();
		String dump = yaml.dump(getValues(false));

		if (dump.equals(BLANK_CONFIG)) {
			dump = "";
		}

		return header + dump;
	}

	@Override
	public void loadFromString(@NotNull String contents) throws InvalidConfigurationException {
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

	protected void convertMapsToSections(@NotNull Map<?, ?> input, @NotNull ConfigurationSection section) {
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

	@NotNull
	protected String parseHeader(@NotNull String input) {
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

	@NotNull
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
	 * Creates a new {@link SimpleYaml}, loading from the given file.
	 * <p>
	 * Any errors loading the Configuration will be logged and then ignored.
	 * If the specified input is not a valid config, a blank config will be
	 * returned.
	 * <p>
	 * The encoding used may follow the system dependent default.
	 *
	 * @param file Input file
	 * @return Resulting configuration
	 * @throws IllegalArgumentException Thrown if file is null
	 */
	@NotNull
	public static SimpleYaml loadConfiguration(@NotNull File file) {
		Validate.notNull(file, "File cannot be null");

		final SimpleYaml config = new SimpleYaml();

		try {
			config.load(file);
		} catch (final FileNotFoundException ex) {
		} catch (final IOException ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
		} catch (final InvalidConfigurationException ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
		}

		return config;
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
	@NotNull
	public static SimpleYaml loadConfiguration(@NotNull Reader reader) {
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
}
