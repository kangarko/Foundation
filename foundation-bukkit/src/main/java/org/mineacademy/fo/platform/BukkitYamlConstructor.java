package org.mineacademy.fo.platform;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.settings.YamlConfig;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.Tag;

/**
 * Implements custom YAML constructor to handle Bukkit's {@link ConfigurationSerialization}.
 */
final class BukkitYamlConstructor extends YamlConfig.YamlConstructor {

	public BukkitYamlConstructor(LoadSettings loadSettings) {
		super(loadSettings);

		this.tagConstructors.put(Tag.MAP, new ConstructCustomObject());
	}

	@SuppressWarnings("rawtypes")
	private class ConstructCustomObject extends ConstructYamlMap {

		private final boolean atLeast1_21 = MinecraftVersion.atLeast(V.v1_21);

		@Override
		public Object construct(Node node) {
			if (node.isRecursive())
				throw new YamlEngineException("Unexpected referential mapping structure. Node: " + node);

			final Map raw = (Map) super.construct(node);

			if (raw.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
				final String classPath = (String) raw.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY);

				if (classPath.equals("ItemMeta")) {

					// https://github.com/PaperMC/Paper/issues/11423
					if (raw.containsKey("internal") && this.atLeast1_21)
						raw.put("custom", raw.get("internal"));
				}

				final Map<String, Object> typed = new LinkedHashMap<>(raw.size());

				for (final Object entryRaw : raw.entrySet()) {
					final Map.Entry entry = (Map.Entry) entryRaw;

					typed.put(entry.getKey().toString(), entry.getValue());
				}

				try {
					return ConfigurationSerialization.deserializeObject(typed);

				} catch (final IllegalArgumentException ex) {
					throw new YamlEngineException("Could not deserialize object", ex);
				}
			}

			return raw;
		}

		@Override
		public void constructRecursive(Node node, Object object) {
			throw new YamlEngineException("Unexpected referential mapping structure. Node: " + node);
		}
	}
}
