package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;

/**
 * Represents a period between two points,
 * used in minigame arenas so you can specify a phase "between",
 * "from", "on" a certain number etc.
 */
@Getter
@RequiredArgsConstructor
public final class ActivePeriod implements ConfigSerializable {

	/**
	 * The active mode for this period
	 */
	private final ActiveMode mode;

	/**
	 * The starting limit
	 */
	private final int startLimit;

	/**
	 * The ending limit
	 */
	private final int endLimit;

	/**
	 * Create a new active period given with the same start and end limit
	 *
	 * @param mode
	 * @param limit
	 */
	public ActivePeriod(ActiveMode mode, int limit) {
		this(mode, limit, limit);
	}

	/**
	 * Return true if this active period is within the given period
	 * for example "between" mode with limits 1 and 4 will return true for period 1,2,3 and 4
	 *
	 * @param period
	 * @return
	 */
	public boolean mayExecute(int period) {
		switch (mode) {
			case FROM:
				return period >= startLimit;

			case ON:
				return period == startLimit;

			case BETWEEN:
				return period >= startLimit && period <= endLimit;

			case TILL:
				return period <= startLimit;

			default:
				throw new FoException("Unhandled activation mode " + mode);
		}
	}

	/**
	 * Return the active period formatted, e.g.:
	 * <p>
	 * "from 5 - 4" or "on 5"
	 *
	 * @return
	 */
	public String formatPeriod() {
		return mode.localizedName + " " + formatLimits();
	}

	/**
	 * Return the two points formatted, e.g.:
	 * <p>
	 * "4 - 5" or only singular: "7"
	 *
	 * @return
	 */
	public String formatLimits() {
		return startLimit + (startLimit == endLimit ? "" : " - " + endLimit);
	}

	/**
	 * Put this active period data into a serializable map which you
	 * can save in your settings file
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("mode", mode.localizedName);
		map.put("limit", startLimit);
		map.put("limit2", endLimit);

		return map;
	}

	/**
	 * Parses a serialized map from your settings file
	 * into an active period class
	 *
	 * @param map
	 * @return
	 */
	public static ActivePeriod deserialize(SerializedMap map) {
		final ActiveMode mode = ActiveMode.fromName(map.getString("mode"));
		final int startLimit = map.getInteger("limit");
		final int endLimit = map.getInteger("limit2", startLimit);

		return new ActivePeriod(mode, startLimit, endLimit);
	}

	/**
	 * Represents how the start and end limit should work for {@link ActivePeriod}
	 */
	@RequiredArgsConstructor
	public enum ActiveMode {

		/**
		 * Shall the period be active from the starting limit indefinitelly?
		 */
		FROM("from"),

		/**
		 * Shall the period be active only on the starting limit and nowhere else?
		 */
		ON("on"),

		/**
		 * Shall the period be active from between the start and end limit?
		 */
		BETWEEN("between"),

		/**
		 * Shall the period be active from 0 to the ending limit?
		 */
		TILL("till");

		/**
		 * The localized unobfuscated name
		 */
		private final String localizedName;

		@Override
		public String toString() {
			return localizedName;
		}

		/**
		 * Finds an active mode by its {@link #localizedName}
		 *
		 * @param name
		 * @return
		 */
		public static ActiveMode fromName(String name) {
			for (final ActiveMode other : values())
				if (other.localizedName.equals(name))
					return other;

			throw new FoException("Invalid activation mode: " + name);
		}
	}
}
