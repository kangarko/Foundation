package org.mineacademy.fo.settings;

import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.FoException;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.constructor.ConstructScalar;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.nodes.AnchorNode;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;

import lombok.NonNull;
import lombok.Setter;

/**
 * An implementation of configuration which saves all files in Yaml.
 * Note that this implementation is not synchronized.
 *
 * Enhanced by kangarko to use YAML 1.2 on snakeyaml-engine, comment
 * support on legacy Minecraft versions and faster than Bukkit's serialization
 * of objects into YAML without the need to register them and most importantly,
 * automatic config updater that can preserve users' comments at chosen
 * sections while updating keys or other comments from a default file.
 */
public class YamlConfig extends FileConfig {

	/**
	 * The custom constructor to convert strings to values in the config
	 */
	@Setter
	private static Function<LoadSettings, YamlConstructor> customConstructor = loadSettings -> new YamlConstructor(loadSettings);

	/**
	 * The custom representer to convert values to strings in the config
	 */
	@Setter
	private static Function<DumpSettings, YamlRepresenter> customRepresenter = dumpSettings -> new YamlRepresenter(dumpSettings);

	/**
	 * The dumper to convert the node tree to a string.
	 */
	private final Dump dumper;

	/**
	 * The composer to convert the string to a node tree.
	 */
	private final Compose composer;

	/**
	 * The constructor to convert config strings into Java classes.
	 */
	private final YamlConstructor constructor;

	/**
	 * The representer to convert Java classes into config strings.
	 */
	private final StandardRepresenter representer;

	/**
	 * The header that will be applied to the top of the saved output.
	 * Default is an empty list.
	 */
	private List<String> header = Collections.emptyList();

	/**
	 * The footer that will be applied to the bottom of the saved output.
	 * Default is an empty list.
	 */
	private List<String> footer = Collections.emptyList();

	/**
	 * The sections that are allowed to stay in the file even if they are not used
	 * and a default file is used.
	 */
	private Set<String> uncommentedSections = Collections.emptySet();

	/**
	 * Create a YamlConfig instance. To load it, call {@link #loadAndExtract(String)}
	 * or {@link #loadAndExtract(String, String)}.
	 */
	public YamlConfig() {
		final LoadSettings loadSettings = LoadSettings.builder()
				.setParseComments(true)
				.setCodePointLimit(Integer.MAX_VALUE)
				.setMaxAliasesForCollections(Integer.MAX_VALUE)
				.build();

		this.constructor = customConstructor.apply(loadSettings);
		this.composer = new Compose(loadSettings);

		final DumpSettings dumpSettings = DumpSettings.builder()
				.setDefaultFlowStyle(FlowStyle.BLOCK)
				.setDumpComments(true)
				.setWidth(4096) // Do not wrap long lines
				.setSplitLines(false)
				.build();

		this.representer = customRepresenter.apply(dumpSettings);
		this.dumper = new Dump(dumpSettings, this.representer);
	}

	@Override
	public final String saveToString() {
		final MappingNode node = this.toNodeTreeWithDefaults(this, this.getDefaults());

		node.setBlockComments(this.getCommentLines(this.saveHeader(this.getHeader()), CommentType.BLOCK));
		node.setEndComments(this.getCommentLines(this.getFooter(), CommentType.BLOCK));

		final StreamToStringWriter writer = new StreamToStringWriter();

		if (node.getBlockComments().isEmpty() && node.getEndComments().isEmpty() && node.getValue().isEmpty())
			writer.write("");

		else {
			if (node.getValue().isEmpty())
				node.setFlowStyle(FlowStyle.FLOW);

			this.dumper.dumpNode(node, writer);
		}

		return writer.toString();
	}

	@Override
	public final void loadFromString(@NonNull String contents) {

		MappingNode node;
		final Node rawNode = this.composer.composeString(contents).orElse(null);

		try {
			node = (MappingNode) rawNode;

		} catch (final ClassCastException e) {
			throw new FoException("Top level is not a Map in YAML configuration: " + contents);
		}

		this.map.clear();

		if (node != null) {
			this.adjustNodeComments(node);

			this.setHeader(this.loadHeader(this.getCommentLines(node.getBlockComments())));
			this.setFooter(this.getCommentLines(node.getEndComments()));

			this.fromNodeTree(node, this);
		}

		try {
			this.onLoad();

		} catch (final Throwable t) {
			CommonCore.error(t, "Failed to call onLoad in configuration " + this.getFile());
		}
	}

	/**
	 * Called automatically after the configuration is loaded from a string.
	 *
	 * @param contents
	 */
	protected void onLoad() {
	}

	/*
	 * This method splits the header on the last empty line, and sets the
	 * comments below this line as comments for the first key on the map object.
	 *
	 * @param node The root node of the yaml object
	 */
	private void adjustNodeComments(final MappingNode node) {
		if (node.getBlockComments() == null && !node.getValue().isEmpty()) {
			final Node firstNode = node.getValue().get(0).getKeyNode();
			final List<CommentLine> lines = firstNode.getBlockComments();

			if (lines != null) {
				int index = -1;

				for (int i = 0; i < lines.size(); i++)
					if (lines.get(i).getCommentType() == CommentType.BLANK_LINE)
						index = i;

				if (index != -1) {
					node.setBlockComments(lines.subList(0, index + 1));

					firstNode.setBlockComments(lines.subList(index + 1, lines.size()));
				}
			}
		}
	}

	private void fromNodeTree(MappingNode input, ConfigSection section) {
		this.constructor.flattenMapping(input);

		for (final NodeTuple nodeTuple : input.getValue()) {
			final Node key = nodeTuple.getKeyNode();
			final String keyString = String.valueOf(this.constructor.construct(key));

			Node value = nodeTuple.getValueNode();

			while (value instanceof AnchorNode)
				value = ((AnchorNode) value).getRealNode();

			if (value instanceof MappingNode && !this.hasSerializedTypeKey((MappingNode) value))
				this.fromNodeTree((MappingNode) value, section.createSection(keyString));

			else
				section.store(keyString, this.constructor.construct(value));

			section.setComments(keyString, this.getCommentLines(key.getBlockComments()));

			if (value instanceof MappingNode || value instanceof SequenceNode)
				section.setInlineComments(keyString, this.getCommentLines(key.getInLineComments()));

			else
				section.setInlineComments(keyString, this.getCommentLines(value.getInLineComments()));
		}
	}

	private boolean hasSerializedTypeKey(MappingNode node) {
		for (final NodeTuple nodeTuple : node.getValue()) {
			final Node keyNode = nodeTuple.getKeyNode();

			if (!(keyNode instanceof ScalarNode))
				continue;

			final String key = ((ScalarNode) keyNode).getValue();

			if (key.equals("==")) // From Bukkit
				return true;
		}
		return false;
	}

	private MappingNode toNodeTreeWithDefaults(ConfigSection section, ConfigSection defaults) {

		// Move settings which are NOT in the default file to the unused folder.
		// You can configure which sections are allowed to stay using uncommentedSections field above.
		final Map<String, Object> unusedKeys = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
			final String path = entry.getKey();

			if (defaults != null && !defaults.isStored(path)) {
				boolean isUncommentedSection = false;

				for (final String uncommented : this.uncommentedSections) {
					if (path.startsWith(uncommented)) {

						isUncommentedSection = true;
						break;
					}
				}

				if (!isUncommentedSection) {
					unusedKeys.put(path, entry.getValue());

					section.store(path, null);
				}
			}
		}

		if (!unusedKeys.isEmpty()) {
			final File unusedFile = FileUtil.createIfNotExists("unused/" + this.getFile().getName());
			final YamlConfig unusedConfig = YamlConfig.fromFile(unusedFile);

			for (final Map.Entry<String, Object> entry : unusedKeys.entrySet())
				unusedConfig.store(entry.getKey(), entry.getValue());

			unusedConfig.save();
			CommonCore.warning("The following entries in " + this.getFile().getName() + " are unused and were moved to " + unusedFile + ": " + unusedKeys.keySet());
		}

		return this.toNodeTreeWithDefaults0(section, defaults, true);
	}

	private MappingNode toNodeTreeWithDefaults0(ConfigSection section, ConfigSection defaults, boolean pullFromDefaults) {
		final List<NodeTuple> nodeTuples = new ArrayList<>();

		for (final Map.Entry<String, Object> entry : (pullFromDefaults && defaults != null ? defaults : section).getValues(false).entrySet()) {
			final Node key = this.representer.represent(entry.getKey());
			Node value;

			final boolean hasDiskValue = section.isStored(entry.getKey());
			final Object diskValue = section.retrieve(entry.getKey());

			boolean isUncommentedSection = !pullFromDefaults;
			String deepPath = entry.getKey();

			if (entry.getValue() instanceof ConfigSection) {
				final ConfigSection innerSection = (ConfigSection) entry.getValue();

				deepPath = innerSection.getFullPath();

				for (final String uncommented : this.uncommentedSections) {
					if (deepPath.startsWith(uncommented)) {
						isUncommentedSection = true;

						break;
					}
				}

				if (hasDiskValue)
					ValidCore.checkBoolean(diskValue instanceof ConfigSection, "Expected " + entry.getKey() + " in " + this.getFile() + " to be a Map, got " + diskValue.getClass().getSimpleName());

				value = this.toNodeTreeWithDefaults0((ConfigSection) (hasDiskValue ? diskValue : entry.getValue()), defaults != null ? defaults.retrieveMemorySection(entry.getKey()) : null, !isUncommentedSection);

			} else
				value = this.representer.represent(SerializeUtilCore.serialize(Language.YAML, hasDiskValue ? diskValue : entry.getValue()));

			key.setBlockComments(this.getCommentLines((pullFromDefaults && defaults != null ? defaults : section).getComments(entry.getKey()), CommentType.BLOCK));

			if (value instanceof MappingNode || value instanceof SequenceNode)
				key.setInLineComments(this.getCommentLines((pullFromDefaults && defaults != null ? defaults : section).getInlineComments(entry.getKey()), CommentType.IN_LINE));
			else
				value.setInLineComments(this.getCommentLines((pullFromDefaults && defaults != null ? defaults : section).getInlineComments(entry.getKey()), CommentType.IN_LINE));

			nodeTuples.add(new NodeTuple(key, value));
		}

		return new MappingNode(Tag.MAP, nodeTuples, FlowStyle.BLOCK);
	}

	private List<String> getCommentLines(List<CommentLine> comments) {
		final List<String> lines = new ArrayList<>();

		if (comments != null)
			for (final CommentLine comment : comments)
				if (comment.getCommentType() == CommentType.BLANK_LINE)
					lines.add(null);

				else {
					String line = comment.getValue();

					line = line.startsWith(" ") ? line.substring(1) : line;
					lines.add(line);
				}

		return lines;
	}

	private List<CommentLine> getCommentLines(List<String> comments, CommentType commentType) {
		final List<CommentLine> lines = new ArrayList<>();

		for (final String comment : comments)
			if (comment == null)
				lines.add(new CommentLine(Optional.empty(), Optional.empty(), "", CommentType.BLANK_LINE));

			else {
				String line = comment;

				line = line.isEmpty() ? line : " " + line;
				lines.add(new CommentLine(Optional.empty(), Optional.empty(), line, commentType));
			}
		return lines;
	}

	/*
	 * Removes the empty line at the end of the header that separates the header
	 * from further comments. Also removes all empty header starts (backwards
	 * compat).
	 *
	 * @param header The list of heading comments
	 * @return The modified list
	 */
	private List<String> loadHeader(List<String> header) {
		final LinkedList<String> list = new LinkedList<>(header);

		if (!list.isEmpty())
			list.removeLast();

		while (!list.isEmpty() && list.peek() == null)
			list.remove();

		return list;
	}

	/*
	 * Adds the empty line at the end of the header that separates the header
	 * from further comments.
	 *
	 * @param header The list of heading comments
	 * @return The modified list
	 */
	private List<String> saveHeader(List<String> header) {
		final LinkedList<String> list = new LinkedList<>(header);

		if (!list.isEmpty())
			list.add(null);

		return list;
	}

	/**
	 * Gets the header that will be applied to the top of the saved output.
	 * <p>
	 * This header will be commented out and applied directly at the top of
	 * the generated output of the {@link FileConfig}. It is not
	 * required to include a newline at the end of the header as it will
	 * automatically be applied, but you may include one if you wish for extra
	 * spacing.
	 * <p>
	 * If no comments exist, an empty list will be returned. A null entry
	 * represents an empty line and an empty String represents an empty comment
	 * line.
	 *
	 * @return Unmodifiable header, every entry represents one line.
	 */
	public final List<String> getHeader() {
		return this.header;
	}

	/**
	 * Sets the header that will be applied to the top of the saved output.
	 * <p>
	 * This header will be commented out and applied directly at the top of
	 * the generated output of the {@link FileConfig}. It is not
	 * required to include a newline at the end of the header as it will
	 * automatically be applied, but you may include one if you wish for extra
	 * spacing.
	 * <p>
	 * If no comments exist, an empty list will be returned. A null entry
	 * represents an empty line and an empty String represents an empty comment
	 * line.
	 *
	 * @param lines New header, every entry represents one line.
	 */
	public final void setHeader(String... lines) {
		this.setHeader(CommonCore.newList(lines));
	}

	/**
	 * Sets the header that will be applied to the top of the saved output.
	 * <p>
	 * This header will be commented out and applied directly at the top of
	 * the generated output of the {@link FileConfig}. It is not
	 * required to include a newline at the end of the header as it will
	 * automatically be applied, but you may include one if you wish for extra
	 * spacing.
	 * <p>
	 * If no comments exist, an empty list will be returned. A null entry
	 * represents an empty line and an empty String represents an empty comment
	 * line.
	 *
	 * @param value New header, every entry represents one line.
	 */
	public final void setHeader(List<String> value) {
		final List<String> actualLines = new ArrayList<>();

		if (value != null)
			for (String line : value) {
				if (line == null)
					line = "";

				for (final String subline : line.split("\n"))
					actualLines.add(subline);
			}

		this.header = actualLines;
	}

	/**
	 * Gets the footer that will be applied to the bottom of the saved output.
	 * <p>
	 * This footer will be commented out and applied directly at the bottom of
	 * the generated output of the {@link FileConfig}. It is not required
	 * to include a newline at the beginning of the footer as it will
	 * automatically be applied, but you may include one if you wish for extra
	 * spacing.
	 * <p>
	 * If no comments exist, an empty list will be returned. A null entry
	 * represents an empty line and an empty String represents an empty comment
	 * line.
	 *
	 * @return Unmodifiable footer, every entry represents one line.
	 */
	public final List<String> getFooter() {
		return this.footer;
	}

	/**
	 * Sets the footer that will be applied to the bottom of the saved output.
	 * <p>
	 * This footer will be commented out and applied directly at the bottom of
	 * the generated output of the {@link FileConfig}. It is not required
	 * to include a newline at the beginning of the footer as it will
	 * automatically be applied, but you may include one if you wish for extra
	 * spacing.
	 * <p>
	 * If no comments exist, an empty list will be returned. A null entry
	 * represents an empty line and an empty String represents an empty comment
	 * line.
	 *
	 * @param value New footer, every entry represents one line.
	 */
	public final void setFooter(List<String> value) {
		this.footer = value;
	}

	/**
	 * Return a list of sections which won't inherit comments from the {@link #getDefaults()}
	 * configuration. 
	 * 
	 * @see #setUncommentedSections(Collection)
	 * 
	 * @return
	 */
	public final Set<String> getUncommentedSections() {
		return uncommentedSections;
	}

	/**
	 * Return a list of sections which won't inherit comments from the {@link #getDefaults()}
	 * configuration. 
	 * 
	 * This is useful when you let the use set a map inside the config and you want to 
	 * support custom user comments in that map.
	 * 
	 * @param uncommentedSections
	 */
	public final void setUncommentedSections(Collection<String> uncommentedSections) {
		this.uncommentedSections = new HashSet<>(uncommentedSections);
	}

	/**
	 * Creates a new {@link YamlConfig}, loading from the given internal path.
	 *
	 * @param path the path in the plugin's jar
	 * @return Resulting configuration
	 */
	public static YamlConfig fromInternalPath(@NonNull String path) {
		final YamlConfig config = new YamlConfig();
		config.loadFromInternal(path);

		return config;
	}

	/**
	 * Creates a new {@link YamlConfig}, loading from the given file.
	 *
	 * @param file Input file
	 * @return Resulting configuration
	 */
	public static YamlConfig fromFile(@NonNull File file) {
		final YamlConfig config = new YamlConfig();
		config.loadFromFile(file);

		return config;
	}

	/**
	 * Creates a new {@link YamlConfig}, loading from the given file.
	 *
	 * @param reader Input file
	 * @return Resulting configuration
	 */
	public static YamlConfig fromReader(@NonNull Reader reader) {
		final YamlConfig config = new YamlConfig();
		config.loadFromReader(reader);

		return config;
	}

	/**
	 * A helper class to convert yaml sections into Java objects.
	 */
	public static class YamlConstructor extends StandardConstructor {

		public YamlConstructor(LoadSettings loadSettings) {
			super(loadSettings);

			this.tagConstructors.put(Tag.COMMENT, new ConstructComment());
		}

		@Override
		public void flattenMapping(final MappingNode node) {
			super.flattenMapping(node);
		}

		@Override
		public Object construct(Node node) {
			return constructObject(node);
		}

		private static class ConstructComment extends ConstructScalar {
			@Override
			public Object construct(Node node) {

				// Handle the comment node - For now, we'll just return null.
				return null;
			}
		}
	}

	/**
	 * A helper class to convert Java objects into yaml sections.
	 */
	public static class YamlRepresenter extends StandardRepresenter {

		public YamlRepresenter(DumpSettings settings) {
			super(settings);

			// We use our own custom enum serializer
			this.parentClassRepresenters.remove(Enum.class);
		}
	}

	/*
	 * Internal helper class to support dumping to String
	 */
	private class StreamToStringWriter extends StringWriter implements StreamDataWriter {
	}

}
