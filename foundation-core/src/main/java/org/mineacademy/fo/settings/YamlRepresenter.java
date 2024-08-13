package org.mineacademy.fo.settings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mineacademy.fo.model.ConfigSerializable;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.RepresentToNode;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;

public final class YamlRepresenter extends StandardRepresenter {

	public YamlRepresenter(DumpSettings settings) {
		super(settings);

		this.representers.put(ConfigSerializable.class, new RepresentConfigurationSerializable());
		this.representers.put(ConfigSection.class, new RepresentConfigurationSection());
		this.representers.remove(Enum.class);
	}

	private class RepresentConfigurationSection implements RepresentToNode {
		@Override
		public Node representData(Object data) {
			final ConfigSection section = (ConfigSection) data;
			final Map<String, Object> values = section.getValues(false);

			return representMapping(Tag.MAP, values, FlowStyle.BLOCK);
		}
	}

	private class RepresentConfigurationSerializable implements RepresentToNode {
		@Override
		public Node representData(Object data) {
			final ConfigSerializable serializable = (ConfigSerializable) data;
			final Map<String, Object> values = new LinkedHashMap<>();

			values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, serializable.getClass().getName());
			values.putAll(serializable.serialize().asMap());

			return representMapping(Tag.MAP, values, FlowStyle.BLOCK);
		}
	}
}