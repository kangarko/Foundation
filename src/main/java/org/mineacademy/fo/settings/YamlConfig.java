package org.mineacademy.fo.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

public class YamlConfig extends FileConfig {

	private static final String COMMENT_PREFIX = "# ";
	private static final String BLANK_CONFIG = "{}\n";

	private final Yaml yaml;

	@Setter(value = AccessLevel.PROTECTED)
	private boolean saveEmptyValues = true;

	protected YamlConfig() {
		final YamlConstructor constructor = new YamlConstructor();

		final YamlRepresenter representer = new YamlRepresenter();
		representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setIndent(2);
		dumperOptions.setWidth(4096); // Do not wrap long lines

		// Load options only if available
		if (ReflectionUtil.isClassAvailable("org.yaml.snakeyaml.LoaderOptions")) {
			Yaml yaml;

			try {
				final LoaderOptions loaderOptions = new LoaderOptions();
				loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);

				yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);

			} catch (final NoSuchMethodError ex) {
				yaml = new Yaml(constructor, representer, dumperOptions);
			}

			this.yaml = yaml;
		}

		else
			this.yaml = new Yaml(constructor, representer, dumperOptions);

	}

	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	public boolean isValid() {
		return this.getObject("") instanceof ConfigSection;
	}

	// ------------------------------------------------------------------------------------
	// File manipulation
	// ------------------------------------------------------------------------------------

	public final void loadConfiguration(String internalPath) {
		this.loadConfiguration(internalPath, internalPath);
	}

	public final void loadConfiguration(String from, String to) {

		File file;

		if (from != null) {

			// Copy if not exists yet
			file = FileUtil.extract(from, to);

			// Initialize file early
			this.file = file;

			// Keep a loaded copy to copy default values from
			final YamlConfig defaultConfig = new YamlConfig();
			final String defaultContent = String.join("\n", FileUtil.getInternalFileContent(from));

			defaultConfig.file = file;
			defaultConfig.loadFromString(defaultContent);

			this.defaults = defaultConfig.section;
			this.defaultsPath = from;
		}

		else
			file = FileUtil.getOrMakeFile(to);

		this.load(file);
	}

	@NonNull
	@Override
	final String saveToString() {

		if (this.defaults == null) {
			final String header = this.getHeader() == null ? "" : "# " + String.join("\n# ", this.getHeader().split("\n")) + "\n\n";
			final Map<String, Object> values = this.section.getValues(false);

			if (!this.saveEmptyValues)
				removeEmptyValues(values);

			String dump = this.yaml.dump(values);

			if (dump.equals(BLANK_CONFIG))
				dump = "";

			return header + dump;
		}

		// Special case, write using comments engine
		try {
			YamlComments.writeComments(this.defaultsPath, this.file, null, this.getUncommentedSections());

		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	private static void removeEmptyValues(Map<String, Object> map) {
		for (final Iterator<Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
			final Entry<String, Object> entry = it.next();
			final Object value = entry.getValue();

			if (value instanceof ConfigSection) {
				final Map<String, Object> childMap = ((ConfigSection) value).map;

				removeEmptyValues(childMap);

				if (childMap.isEmpty())
					it.remove();
			}

			if (value == null
					|| (value instanceof Iterable<?> && !((Iterable<?>) value).iterator().hasNext())
					|| (value.getClass().isArray() && ((Object[]) value).length == 0)
					|| (value instanceof Map<?, ?>) && ((Map<?, ?>) value).isEmpty()) {

				it.remove();

				continue;
			}
		}
	}

	@Override
	final void loadFromString(@NonNull String contents) {

		Map<?, ?> input;

		try {
			input = (Map<?, ?>) this.yaml.load(contents);

		} catch (final YAMLException ex) {
			throw ex;

		} catch (final ClassCastException e) {
			throw new IllegalArgumentException("Top level is not a Map.");
		}

		final String header = this.parseHeader(contents);

		if (header.trim().length() > 0)
			this.setHeader(header);

		this.section.map.clear();

		if (input != null)
			this.convertMapsToSections(input, this.section);
	}

	private void convertMapsToSections(@NonNull Map<?, ?> input, @NonNull ConfigSection section) {
		for (final Map.Entry<?, ?> entry : input.entrySet()) {
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof Map)
				this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
			else
				section.store(key, value);
		}
	}

	@NonNull
	private String parseHeader(@NonNull String input) {
		final String[] lines = input.split("\r?\n", -1);
		final StringBuilder result = new StringBuilder();
		boolean readingHeader = true;
		boolean foundHeader = false;

		for (int i = 0; (i < lines.length) && (readingHeader); i++) {
			final String line = lines[i].trim();

			if (line.startsWith(COMMENT_PREFIX) || line.equals("#")) {
				if (i > 0)
					result.append("\n");

				if (line.length() > COMMENT_PREFIX.length())
					result.append(line.substring(COMMENT_PREFIX.length()));

				foundHeader = true;

			} else if (foundHeader && line.length() == 0)
				result.append("\n");

			else if (foundHeader)
				readingHeader = false;
		}

		final String string = result.toString();

		return string.trim().isEmpty() ? "" : string + "\n";
	}

	@NonNull
	public static final YamlConfig fromInternalPath(@NonNull String path) {

		final YamlConfig config = new YamlConfig();

		try {
			config.loadConfiguration(path);

		} catch (final Exception ex) {
			Logger.getGlobal().log(Level.SEVERE, "Cannot load " + path, ex);
		}

		return config;
	}

	@NonNull
	public static final YamlConfig fromFile(@NonNull File file) {

		final YamlConfig config = new YamlConfig();

		try {
			config.load(file);
		} catch (final Exception ex) {
			Logger.getGlobal().log(Level.SEVERE, "Cannot load " + file, ex);
		}

		return config;
	}

	/*@NonNull
	public static final YamlConfig fromReader(@NonNull Reader reader) {
	
		final YamlConfig config = new YamlConfig();
	
		try {
			config.load(reader);
	
		} catch (final Exception ex) {
			Logger.getGlobal().log(Level.SEVERE, "Cannot load configuration from stream", ex);
		}
	
		return config;
	}*/

	private final static class YamlConstructor extends SafeConstructor {

		public YamlConstructor() {
			this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
		}

		private class ConstructCustomObject extends ConstructYamlMap {

			@Override
			public Object construct(@NonNull Node node) {
				if (node.isTwoStepsConstruction())
					throw new YAMLException("Unexpected referential mapping structure. Node: " + node);

				final Map<?, ?> raw = (Map<?, ?>) super.construct(node);

				if (raw.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
					final Map<String, Object> typed = new LinkedHashMap<>(raw.size());
					for (final Map.Entry<?, ?> entry : raw.entrySet())
						typed.put(entry.getKey().toString(), entry.getValue());

					try {
						return ConfigurationSerialization.deserializeObject(typed);
					} catch (final IllegalArgumentException ex) {
						throw new YAMLException("Could not deserialize object", ex);
					}
				}

				return raw;
			}

			@Override
			public void construct2ndStep(@NonNull Node node, @NonNull Object object) {
				throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
			}
		}
	}

	private final static class YamlRepresenter extends Representer {

		public YamlRepresenter() {
			this.multiRepresenters.put(ConfigSection.class, new RepresentConfigurationSection());
			this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
			this.multiRepresenters.remove(Enum.class);
		}

		private class RepresentConfigurationSection extends RepresentMap {

			@NonNull
			@Override
			public Node representData(@NonNull Object data) {
				return super.representData(((ConfigSection) data).getValues(false));
			}
		}

		private class RepresentConfigurationSerializable extends RepresentMap {

			@NonNull
			@Override
			public Node representData(@NonNull Object data) {
				final ConfigurationSerializable serializable = (ConfigurationSerializable) data;
				final Map<String, Object> values = new LinkedHashMap<>();
				values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
				values.putAll(serializable.serialize());

				return super.representData(values);
			}
		}
	}

}
