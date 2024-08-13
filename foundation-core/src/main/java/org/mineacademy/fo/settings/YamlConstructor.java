package org.mineacademy.fo.settings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.Tag;

public final class YamlConstructor extends StandardConstructor {

	public YamlConstructor(LoadSettings settings) {
		super(settings);

		this.tagConstructors.put(Tag.MAP, new ConstructCustomObject());
	}

	private class ConstructCustomObject implements ConstructNode {

		@Override
		public Object construct(Node node) {
			if (node instanceof MappingNode && ((MappingNode) node).getValue().isEmpty())
				throw new YamlEngineException("Unexpected referential mapping structure. Node: " + node);

			final Map<Object, Object> raw = constructMapping((MappingNode) node);

			if (raw.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
				final Map<String, Object> typed = new LinkedHashMap<>(raw.size());

				for (final Map.Entry<Object, Object> entry : raw.entrySet())
					typed.put(entry.getKey().toString(), entry.getValue());

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