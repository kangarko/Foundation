package org.mineacademy.fo.platform;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.settings.YamlConfig;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.nodes.Node;

/**
 * Implements custom YAML representer to handle Bukkit's {@link ConfigurationSerializable}.
 */
class BukkitYamlRepresenter extends YamlConfig.YamlRepresenter {

	public BukkitYamlRepresenter(final DumpSettings settings) {
		super(settings);

		this.parentClassRepresenters.put(ConfigurationSection.class, new RepresentConfigurationSection());
		this.parentClassRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
	}

	private class RepresentConfigurationSection extends RepresentMap {

		@Override
		public Node representData(final Object data) {
			return super.representData(((ConfigurationSection) data).getValues(false));
		}
	}

	private class RepresentConfigurationSerializable extends RepresentMap {

		@Override
		public Node representData(final Object data) {
			final ConfigurationSerializable serializable = (ConfigurationSerializable) data;
			final Map<String, Object> values = new LinkedHashMap<>();

			try {
				values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
				values.putAll(serializable.serialize());

			} catch (final IllegalStateException ex) {

				// Bukkit components with custom (non-vanilla) registry entries such as custom
				// resource pack sounds in EquippableComponent throw IllegalStateException on
				// serialize(). Skip the component and warn instead of crashing.
				CommonCore.warning("Unable to serialize Bukkit's " + serializable.getClass().getSimpleName()
						+ " because it contains a non-vanilla registry entry (" + ex.getMessage()
						+ "). This component will be skipped. Data: " + data);

				return super.representData(new LinkedHashMap<>());

			} catch (final Throwable ex) {
				throw new FoException(ex, "Error serializing Bukkit's " + serializable.getClass().getSimpleName() + " from data " + data);
			}

			return super.representData(values);
		}
	}
}
