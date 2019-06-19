package org.mineacademy.fo.model;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.region.RegionCuboid;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A region holding the area and its name.
 */
@Getter
@RequiredArgsConstructor
public final class NamedRegion implements ConfigSerializable {

	/**
	 * The name of the region.
	 */
	private final String name;

	/**
	 * The cuboid area.
	 */
	private final Region region;

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Name", name);
		map.put("Region", region);

		return map;
	}

	/**
	 * Deserialize a {@link NamedRegion} from the given map
	 *
	 * @param map
	 * @return
	 */
	public static NamedRegion deserialize(SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Region") && map.containsKey("Name"), "Malformed region map in data file! - " + map);

		final String name = map.getString("Name");
		final Region region = map.get("Region", RegionCuboid.class);

		return new NamedRegion(name, region);
	}
}