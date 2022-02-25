package org.mineacademy.fo.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.Remain;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import lombok.NonNull;

public class YamlConfig extends FileConfig {

	private static final String COMMENT_PREFIX = "# ";
	private static final String BLANK_CONFIG = "{}\n";

	private final Yaml yaml;

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

			// Keep a loaded copy to copy default values from
			final YamlConfig defaultConfig = new YamlConfig();
			final String defaultContent = FileUtil.getInternalFileContent(from);

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
			final String header = this.buildHeader();
			final Map<String, Object> values = this.section.getValues(false);

			removeNuls(values);

			String dump = this.yaml.dump(values);

			if (dump.equals(BLANK_CONFIG))
				dump = "";

			return header + dump;
		}

		return this.saveCommentedString();
	}

	private static void removeNuls(Map<String, Object> map) {
		for (final Iterator<Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
			final Entry<String, Object> entry = it.next();
			//final String key = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof ConfigSection) {
				final Map<String, Object> childMap = ((ConfigSection) value).map;

				removeNuls(childMap);

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

	private String saveCommentedString() {
		Valid.checkNotNull(this.getUncommentedSections(), "getUncommentedSections() cannot be null, return an empty list instead in " + this);

		final String defaultContent = FileUtil.getInternalFileContent(this.defaultsPath);
		final StringBuilder content = new StringBuilder();

		// ignoredSections can ONLY contain configurations sections
		for (final String ignoredSection : this.getUncommentedSections())
			if (this.defaults.isStored(ignoredSection))
				Valid.checkBoolean(this.defaults.retrieve(ignoredSection) instanceof ConfigSection,
						"Can only ignore config sections in " + this.defaultsPath + " (file " + this.getFileName() + ")" + " not '" + ignoredSection + "' that is " + this.defaults.retrieve(ignoredSection));

		// Save keys added to config that are not in default and would otherwise be lost
		final Set<String> newKeys = this.defaults.getKeys(true);
		final Map<String, Object> removedKeys = new LinkedHashMap<>();

		outerLoop:
		for (final Map.Entry<String, Object> oldEntry : this.section.getValues(true).entrySet()) {
			final String oldKey = oldEntry.getKey();

			for (final String ignoredKey : this.getUncommentedSections())
				if (oldKey.startsWith(ignoredKey))
					continue outerLoop;

			if (!newKeys.contains(oldKey))
				removedKeys.put(oldKey, oldEntry.getValue());
		}

		// Move to unused/ folder and retain old path
		if (!removedKeys.isEmpty()) {
			final YamlConfig backupConfig = new YamlConfig();

			backupConfig.loadConfiguration(NO_DEFAULT, "unused/" + getFileName());

			for (final Map.Entry<String, Object> entry : removedKeys.entrySet())
				backupConfig.set(entry.getKey(), entry.getValue());

			backupConfig.save();

			Common.warning("The following entries in " + getFileName() + " are unused and were moved into " + backupConfig.getFileName() + ": " + removedKeys.keySet());
		}

		final Map<String, String> comments = this.dumpComments(defaultContent.split("\n"));

		try {
			this.write(comments, content);
		} catch (final Throwable t) {
			Remain.sneaky(t);
		}

		return content.toString();
	}

	// It checks if key has a comment associated with it and writes comment then the key and value
	// Notice: Various authors, original credit lost. Please contact us for crediting you.
	private void write(Map<String, String> comments, StringBuilder writer) throws IOException {

		final Set<String> copyAllowed = new HashSet<>();
		final Set<String> reverseCopy = new HashSet<>();

		outerloop:
		for (final String key : this.defaults.getKeys(true)) {

			checkIgnore:
			{

				for (final String allowed : copyAllowed)
					if (key.startsWith(allowed))
						break checkIgnore;

				// These keys are already written below
				for (final String allowed : reverseCopy)
					if (key.startsWith(allowed))
						continue outerloop;

				for (final String ignoredKey : this.getUncommentedSections()) {
					if (key.equals(ignoredKey)) {
						final ConfigSection ignoredSection = this.section.retrieveConfigurationSection(ignoredKey);
						final boolean sectionExists = ignoredSection != null;

						// Write from new to old config
						if (!this.isSet(ignoredKey) || (sectionExists && ignoredSection.getKeys(false).isEmpty())) {
							copyAllowed.add(ignoredKey);

							break;
						}

						// Write from old to new, copying all keys and subkeys manually
						else {
							this.write0(key, true, comments, writer);

							if (sectionExists)
								for (final String oldKey : ignoredSection.getKeys(true))
									this.write0(ignoredKey + "." + oldKey, true, comments, writer);

							reverseCopy.add(ignoredKey);
							continue outerloop;
						}
					}

					if (key.startsWith(ignoredKey))
						continue outerloop;
				}
			}

			this.write0(key, false, comments, writer);
		}

		final String danglingComments = comments.get(null);

		if (danglingComments != null)
			writer.append(danglingComments);
	}

	private void write0(String key, boolean forceNew, Map<String, String> comments, StringBuilder writer) throws IOException {

		final String[] keys = key.split("\\.");
		final String actualKey = keys[keys.length - 1];
		final String comment = comments.remove(key);

		final StringBuilder prefixBuilder = new StringBuilder();
		final int indents = keys.length - 1;
		this.appendPrefixSpaces(prefixBuilder, indents);
		final String prefixSpaces = prefixBuilder.toString();

		// No \n character necessary, new line is automatically at end of comment
		if (comment != null)
			writer.append(comment);

		final Object newObj = this.defaults.retrieve(key);
		final Object oldObj = this.section.retrieve(key);

		// Write the old section
		if (newObj instanceof ConfigSection && !forceNew && oldObj instanceof ConfigSection)
			this.writeSection(writer, actualKey, prefixSpaces, (ConfigSection) oldObj);

		// Write the new section, old value is no more
		else if (newObj instanceof ConfigSection)
			this.writeSection(writer, actualKey, prefixSpaces, (ConfigSection) newObj);

		// Write the old object
		else if (oldObj != null && !forceNew)
			this.write(oldObj, actualKey, prefixSpaces, writer);

		// Write new object
		else
			this.write(newObj, actualKey, prefixSpaces, writer);
	}

	// Doesn't work with configuration sections, must be an actual object
	// Auto checks if it is serializable and writes to file
	private void write(Object obj, String actualKey, String prefixSpaces, StringBuilder writer) throws IOException {

		if (obj instanceof String || obj instanceof Character) {
			if (obj instanceof String) {
				final String string = (String) obj;

				// Split multi line strings using |-
				if (string.contains("\n")) {
					writer.append(prefixSpaces + actualKey + ": |-\n");

					for (final String line : string.split("\n"))
						writer.append(prefixSpaces + "    " + line + "\n");

					return;
				}
			}

			writer.append(prefixSpaces + actualKey + ": " + this.yaml.dump(SerializeUtil.serialize(obj)));

		} else if (obj instanceof List)
			this.writeList((List<?>) obj, actualKey, prefixSpaces, writer);

		else
			writer.append(prefixSpaces + actualKey + ": " + this.yaml.dump(SerializeUtil.serialize(obj)));

	}

	// Writes a configuration section
	private void writeSection(StringBuilder writer, String actualKey, String prefixSpaces, ConfigSection section) throws IOException {
		if (section.getKeys(false).isEmpty())
			writer.append(prefixSpaces + actualKey + ":");

		else
			writer.append(prefixSpaces + actualKey + ":");

		writer.append("\n");
	}

	// Writes a list of any object
	private void writeList(List<?> list, String actualKey, String prefixSpaces, StringBuilder writer) throws IOException {
		writer.append(this.dumpListAsString(list, actualKey, prefixSpaces));
	}

	private String dumpListAsString(List<?> list, String actualKey, String prefixSpaces) {
		final StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

		if (list.isEmpty()) {
			builder.append(" []\n");
			return builder.toString();
		}

		builder.append("\n");

		for (int i = 0; i < list.size(); i++) {
			final Object o = list.get(i);

			if (o instanceof String || o instanceof Character)
				builder.append(prefixSpaces).append("- '").append(o.toString().replace("'", "''")).append("'");
			else if (o instanceof List)
				builder.append(prefixSpaces).append("- '").append(this.yaml.dump(SerializeUtil.serialize(o)));
			else
				builder.append(prefixSpaces).append("- ").append(SerializeUtil.serialize(o));

			if (i != list.size())
				builder.append("\n");
		}

		return builder.toString();
	}

	// Key is the config key, value = comment and/or ignored sections
	// Parses comments, blank lines, and ignored sections
	private static Map<String, String> dumpComments(String[] lines) {
		final Map<String, String> comments = new LinkedHashMap<>();
		final StringBuilder builder = new StringBuilder();
		final StringBuilder keyBuilder = new StringBuilder();
		int lastLineIndentCount = 0;

		//outer:
		for (final String line : lines) {
			if (line != null && line.trim().startsWith("-"))
				continue;

			if (line == null || line.trim().equals("") || line.trim().startsWith("#"))
				builder.append(line).append("\n");
			else {
				lastLineIndentCount = setFullKey(keyBuilder, line, lastLineIndentCount);

				if (keyBuilder.length() > 0) {
					comments.put(keyBuilder.toString(), builder.toString());
					builder.setLength(0);
				}
			}
		}

		if (builder.length() > 0)
			comments.put(null, builder.toString());

		return comments;
	}

	// Counts spaces in front of key and divides by 2 since 1 indent = 2 spaces
	private static int countIndents(String s) {
		int spaces = 0;

		for (final char c : s.toCharArray())
			if (c == ' ')
				spaces += 1;
			else
				break;

		return spaces / 2;
	}

	// Ex. keyBuilder = key1.key2.key3 --> key1.key2
	private static void removeLastKey(StringBuilder keyBuilder) {
		String temp = keyBuilder.toString();
		final String[] keys = temp.split("\\.");

		if (keys.length == 1) {
			keyBuilder.setLength(0);
			return;
		}

		temp = temp.substring(0, temp.length() - keys[keys.length - 1].length() - 1);
		keyBuilder.setLength(temp.length());
	}

	// Updates the keyBuilder and returns configLines number of indents
	private static int setFullKey(StringBuilder keyBuilder, String configLine, int lastLineIndentCount) {
		final int currentIndents = countIndents(configLine);
		final String key = configLine.trim().split(":")[0];

		if (keyBuilder.length() == 0)
			keyBuilder.append(key);

		else if (currentIndents == lastLineIndentCount) {
			// Replace the last part of the key with current key
			removeLastKey(keyBuilder);

			if (keyBuilder.length() > 0)
				keyBuilder.append(".");

			keyBuilder.append(key);
		} else if (currentIndents > lastLineIndentCount)
			// Append current key to the keyBuilder
			keyBuilder.append(".").append(key);

		else {
			final int difference = lastLineIndentCount - currentIndents;

			for (int i = 0; i < difference + 1; i++)
				removeLastKey(keyBuilder);

			if (keyBuilder.length() > 0)
				keyBuilder.append(".");

			keyBuilder.append(key);
		}

		return currentIndents;
	}

	private static String getPrefixSpaces(int indents) {
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < indents; i++)
			builder.append("  ");

		return builder.toString();
	}

	private static void appendPrefixSpaces(StringBuilder builder, int indents) {
		builder.append(getPrefixSpaces(indents));
	}

	@Override
	final void loadFromString(@NonNull String contents) {

		Map<?, ?> input;

		try {
			input = (Map<?, ?>) this.yaml.load(contents);

		} catch (final YAMLException e) {
			throw e;

		} catch (final ClassCastException e) {
			throw new IllegalArgumentException("Top level is not a Map.");
		}

		final String header = this.parseHeader(contents);
		if (header.length() > 0)
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
			final String line = lines[i];

			if (line.startsWith(COMMENT_PREFIX)) {
				if (i > 0)
					result.append("\n");

				if (line.length() > COMMENT_PREFIX.length())
					result.append(line.substring(COMMENT_PREFIX.length()));

				foundHeader = true;
			} else if ((foundHeader) && (line.length() == 0))
				result.append("\n");
			else if (foundHeader)
				readingHeader = false;
		}

		return result.toString();
	}

	@NonNull
	private String buildHeader() {
		final String header = this.getHeader();

		if (header == null)
			return "";

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
