package org.mineacademy.fo.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.remain.Remain;
import org.yaml.snakeyaml.Yaml;

import lombok.NonNull;

/**
 * A class to update/add new sections/keys to your config while keeping your current values and keeping your comments
 * Algorithm:
 * Read the new file and scan for comments and ignored sections, if ignored section is found it is treated as a comment.
 * Read and write each line of the new config, if the old config has value for the given key it writes that value in the new config.
 * If a key has an attached comment above it, it is written first.
 *
 * @author tchristofferson
 *
 * Source: https://github.com/tchristofferson/Config-Updater
 */
class ConfigUpdater {

	/**
	 * Update a yaml file from a resource inside your plugin jar
	 *
	 * @param internalPath The yaml file name to update from, typically config.yml
	 * @param outerPath The yaml file to update
	 *
	 * @throws IOException If an IOException occurs
	 */
	public static void update(@NonNull String internalPath, File outerPath) throws IOException {
		final BufferedReader newReader = new BufferedReader(new InputStreamReader(FileUtil.getInternalResource(internalPath), StandardCharsets.UTF_8));
		final List<String> newLines = newReader.lines().collect(Collectors.toList());
		newReader.close();

		final FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(outerPath);
		final FileConfiguration newConfig = Remain.loadConfiguration(FileUtil.getInternalResource(internalPath));
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outerPath), StandardCharsets.UTF_8));

		final List<String> ignoredSectionsArrayList = new ArrayList<>();
		//ignoredSections can ONLY contain configurations sections
		ignoredSectionsArrayList.removeIf(ignoredSection -> !newConfig.isConfigurationSection(ignoredSection));

		final Yaml yaml = new Yaml();
		final Map<String, String> comments = parseComments(newLines, ignoredSectionsArrayList, oldConfig, yaml);

		write(newConfig, oldConfig, comments, ignoredSectionsArrayList, writer, yaml);
	}

	//Write method doing the work.
	//It checks if key has a comment associated with it and writes comment then the key and value
	private static void write(FileConfiguration newConfig, FileConfiguration oldConfig, Map<String, String> comments, List<String> ignoredSections, BufferedWriter writer, Yaml yaml) throws IOException {
		outer:
		for (final String key : newConfig.getKeys(true)) {
			final String[] keys = key.split("\\.");
			final String actualKey = keys[keys.length - 1];
			final String comment = comments.remove(key);

			final StringBuilder prefixBuilder = new StringBuilder();
			final int indents = keys.length - 1;
			appendPrefixSpaces(prefixBuilder, indents);
			final String prefixSpaces = prefixBuilder.toString();

			if (comment != null) {
				writer.write(comment);//No \n character necessary, new line is automatically at end of comment
			}

			for (final String ignoredSection : ignoredSections) {
				if (key.startsWith(ignoredSection)) {
					continue outer;
				}
			}

			final Object newObj = newConfig.get(key);
			final Object oldObj = oldConfig.get(key);

			if (newObj instanceof ConfigurationSection && oldObj instanceof ConfigurationSection) {
				//write the old section
				writeSection(writer, actualKey, prefixSpaces, (ConfigurationSection) oldObj);
			} else if (newObj instanceof ConfigurationSection) {
				//write the new section, old value is no more
				writeSection(writer, actualKey, prefixSpaces, (ConfigurationSection) newObj);
			} else if (oldObj != null) {
				//write the old object
				write(oldObj, actualKey, prefixSpaces, yaml, writer);
			} else {
				//write new object
				write(newObj, actualKey, prefixSpaces, yaml, writer);
			}
		}

		final String danglingComments = comments.get(null);

		if (danglingComments != null) {
			writer.write(danglingComments);
		}

		writer.close();
	}

	//Doesn't work with configuration sections, must be an actual object
	//Auto checks if it is serializable and writes to file
	private static void write(Object obj, String actualKey, String prefixSpaces, Yaml yaml, BufferedWriter writer) throws IOException {
		if (obj instanceof ConfigurationSerializable) {
			writer.write(prefixSpaces + actualKey + ": " + yaml.dump(((ConfigurationSerializable) obj).serialize()));
		} else if (obj instanceof String || obj instanceof Character) {
			if (obj instanceof String) {
				final String s = (String) obj;
				obj = s.replace("\n", "\\n");
			}

			writer.write(prefixSpaces + actualKey + ": " + yaml.dump(obj));
		} else if (obj instanceof List) {
			writeList((List<?>) obj, actualKey, prefixSpaces, yaml, writer);
		} else {
			writer.write(prefixSpaces + actualKey + ": " + yaml.dump(obj));
		}
	}

	//Writes a configuration section
	private static void writeSection(BufferedWriter writer, String actualKey, String prefixSpaces, ConfigurationSection section) throws IOException {
		if (section.getKeys(false).isEmpty()) {
			writer.write(prefixSpaces + actualKey + ": {}");
		} else {
			writer.write(prefixSpaces + actualKey + ":");
		}

		writer.write("\n");
	}

	//Writes a list of any object
	private static void writeList(List<?> list, String actualKey, String prefixSpaces, Yaml yaml, BufferedWriter writer) throws IOException {
		writer.write(getListAsString(list, actualKey, prefixSpaces, yaml));
	}

	private static String getListAsString(List<?> list, String actualKey, String prefixSpaces, Yaml yaml) {
		final StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

		if (list.isEmpty()) {
			builder.append(" []\n");
			return builder.toString();
		}

		builder.append("\n");

		for (int i = 0; i < list.size(); i++) {
			final Object o = list.get(i);

			if (o instanceof String || o instanceof Character) {
				builder.append(prefixSpaces).append("- '").append(o).append("'");
			} else if (o instanceof List) {
				builder.append(prefixSpaces).append("- ").append(yaml.dump(o));
			} else {
				builder.append(prefixSpaces).append("- ").append(o);
			}

			if (i != list.size()) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	//Key is the config key, value = comment and/or ignored sections
	//Parses comments, blank lines, and ignored sections
	private static Map<String, String> parseComments(List<String> lines, List<String> ignoredSections, FileConfiguration oldConfig, Yaml yaml) {
		final Map<String, String> comments = new HashMap<>();
		final StringBuilder builder = new StringBuilder();
		final StringBuilder keyBuilder = new StringBuilder();
		int lastLineIndentCount = 0;

		outer:
		for (final String line : lines) {
			if (line != null && line.trim().startsWith("-"))
				continue;

			if (line == null || line.trim().equals("") || line.trim().startsWith("#")) {
				builder.append(line).append("\n");
			} else {
				lastLineIndentCount = setFullKey(keyBuilder, line, lastLineIndentCount);

				for (final String ignoredSection : ignoredSections) {
					if (keyBuilder.toString().equals(ignoredSection)) {
						final Object value = oldConfig.get(keyBuilder.toString());

						if (value instanceof ConfigurationSection)
							appendSection(builder, (ConfigurationSection) value, new StringBuilder(getPrefixSpaces(lastLineIndentCount)), yaml);

						continue outer;
					}
				}

				if (keyBuilder.length() > 0) {
					comments.put(keyBuilder.toString(), builder.toString());
					builder.setLength(0);
				}
			}
		}

		if (builder.length() > 0) {
			comments.put(null, builder.toString());
		}

		return comments;
	}

	private static void appendSection(StringBuilder builder, ConfigurationSection section, StringBuilder prefixSpaces, Yaml yaml) {
		builder.append(prefixSpaces).append(getKeyFromFullKey(section.getCurrentPath())).append(":");
		final Set<String> keys = section.getKeys(false);

		if (keys.isEmpty()) {
			builder.append(" {}\n");
			return;
		}

		builder.append("\n");
		prefixSpaces.append("  ");

		for (final String key : keys) {
			final Object value = section.get(key);
			final String actualKey = getKeyFromFullKey(key);

			if (value instanceof ConfigurationSection) {
				appendSection(builder, (ConfigurationSection) value, prefixSpaces, yaml);
				prefixSpaces.setLength(prefixSpaces.length() - 2);
			} else if (value instanceof List) {
				builder.append(getListAsString((List<?>) value, actualKey, prefixSpaces.toString(), yaml));
			} else {
				builder.append(prefixSpaces.toString()).append(actualKey).append(": ").append(yaml.dump(value));
			}
		}
	}

	//Counts spaces in front of key and divides by 2 since 1 indent = 2 spaces
	private static int countIndents(String s) {
		int spaces = 0;

		for (final char c : s.toCharArray()) {
			if (c == ' ') {
				spaces += 1;
			} else {
				break;
			}
		}

		return spaces / 2;
	}

	//Ex. keyBuilder = key1.key2.key3 --> key1.key2
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

	private static String getKeyFromFullKey(String fullKey) {
		final String[] keys = fullKey.split("\\.");
		return keys[keys.length - 1];
	}

	//Updates the keyBuilder and returns configLines number of indents
	private static int setFullKey(StringBuilder keyBuilder, String configLine, int lastLineIndentCount) {
		final int currentIndents = countIndents(configLine);
		final String key = configLine.trim().split(":")[0];

		if (keyBuilder.length() == 0) {
			keyBuilder.append(key);
		} else if (currentIndents == lastLineIndentCount) {
			//Replace the last part of the key with current key
			removeLastKey(keyBuilder);

			if (keyBuilder.length() > 0) {
				keyBuilder.append(".");
			}

			keyBuilder.append(key);
		} else if (currentIndents > lastLineIndentCount) {
			//Append current key to the keyBuilder
			keyBuilder.append(".").append(key);
		} else {
			final int difference = lastLineIndentCount - currentIndents;

			for (int i = 0; i < difference + 1; i++) {
				removeLastKey(keyBuilder);
			}

			if (keyBuilder.length() > 0) {
				keyBuilder.append(".");
			}

			keyBuilder.append(key);
		}

		return currentIndents;
	}

	private static String getPrefixSpaces(int indents) {
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < indents; i++) {
			builder.append("  ");
		}

		return builder.toString();
	}

	private static void appendPrefixSpaces(StringBuilder builder, int indents) {
		builder.append(getPrefixSpaces(indents));
	}
}