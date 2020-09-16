package org.mineacademy.fo.bungee;

import java.lang.reflect.Constructor;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * A unified way of combining Bungee channel, listener and action
 */
@Getter
public final class SimpleBungee {

	/**
	 * The channel
	 */
	private final String channel;

	/**
	 * The listener
	 */
	private final BungeeListener listener;

	/**
	 * The actions
	 */
	private final BungeeAction[] actions;

	/**
	 * Create a new simple bungee suite with the given channel, the given listener class and the given action as enum
	 *
	 * @param channel
	 * @param listenerClass
	 * @param actionEnum
	 */
	public SimpleBungee(String channel, Class<? extends BungeeListener> listenerClass, Class<? extends BungeeAction> actionEnum) {
		this(channel, toListener(listenerClass), toAction(actionEnum));
	}

	private static BungeeListener toListener(Class<? extends BungeeListener> listenerClass) {
		Valid.checkNotNull(listenerClass);

		try {
			final Constructor<?> con = listenerClass.getConstructor();
			con.setAccessible(true);

			return (BungeeListener) con.newInstance();
		} catch (final ReflectiveOperationException ex) {
			Common.log("Unable to create new instance of " + listenerClass + ", ensure constructor is public without parameters!");
			ex.printStackTrace();

			return null;
		}
	}

	private static BungeeAction[] toAction(Class<? extends BungeeAction> actionEnum) {
		Valid.checkNotNull(actionEnum);
		Valid.checkBoolean(actionEnum.isEnum(), "Enum expected, given: " + actionEnum);

		try {
			return (BungeeAction[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.log("Unable to get values() of " + actionEnum + ", ensure it is an enum!");
			ex.printStackTrace();

			return null;
		}
	}

	/**
	 * Create a new bungee suite with the given params
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	public SimpleBungee(String channel, BungeeListener listener, BungeeAction... actions) {
		Valid.checkNotNull(channel, "Channel cannot be null!");

		this.channel = channel;
		this.listener = listener;

		Valid.checkNotNull(actions, "Actions cannot be null!");
		this.actions = actions;
	}
}
