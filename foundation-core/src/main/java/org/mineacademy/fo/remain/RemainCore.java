package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.time.Duration;

import org.mineacademy.fo.ReflectionUtil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.title.Title.Times;

/**
 * Our cross-version compatibility class for the Adventure library, shared by all platforms.
 *
 * The runtime Adventure varies wildly: servers bundling it natively range from years old
 * 4.x lines (Paper 1.16.5) through 4.22 to 4.24 (PandaSpigot, Paper 1.21.6 to 1.21.8) up to
 * 5.x (Paper 1.21.9+), while servers without it get 4.26.1 downloaded by us. We compile
 * against 5.x, so members removed in 5.x are invoked reflectively here and members added
 * after the oldest 4.x lines still found in the wild are probed and gated here.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RemainCore {

	/**
	 * Does the runtime Adventure have the ClickEvent.Payload API? Added in 4.22.0.
	 */
	private static boolean hasClickPayload = true;

	/**
	 * Does the runtime Adventure have ClickEvent.clickEvent(Action, Payload)? Added in 4.25.0,
	 * so 4.22 to 4.24 bundles carry the Payload API without this factory.
	 */
	private static boolean hasClickPayloadFactory = true;

	/**
	 * Does the runtime Adventure have ClickEvent.custom(Key, BinaryTagHolder)? Added in 4.23.0,
	 * one release after custom click events themselves, so a server bundling 4.22.x supports the
	 * click action but not this factory.
	 */
	private static boolean hasCustomClickEvent = true;

	/**
	 * ClickEvent#value(), the pre-4.22 payload getter. Removed in Adventure 5.x which we
	 * compile against, hence reflective.
	 */
	private static Method legacyClickValueMethod;

	/**
	 * ClickEvent.clickEvent(Action, String), the pre-4.25 factory. Removed in Adventure 5.x
	 * which we compile against, hence reflective.
	 */
	private static Method legacyClickFactoryMethod;

	/**
	 * Title.Times.of(Duration, Duration, Duration), the factory before the times() rename in
	 * 4.14.0, found on servers bundling old Adventure natively such as Paper 1.16.5. Removed
	 * in Adventure 5.x which we compile against, hence reflective. Null when times() exists.
	 */
	private static Method legacyTimesOfMethod;

	static {
		hasClickPayload = ReflectionUtil.isClassAvailable("net.kyori.adventure.text.event.ClickEvent$Payload");
		hasClickPayloadFactory = hasClickPayload && ClickPayloadAccessor.hasFactory();
		hasCustomClickEvent = ReflectionUtil.getMethod(ClickEvent.class, "custom", Key.class, BinaryTagHolder.class) != null;

		if (!hasClickPayload)
			legacyClickValueMethod = ReflectionUtil.getMethod(ClickEvent.class, "value");

		if (!hasClickPayloadFactory)
			legacyClickFactoryMethod = ReflectionUtil.getMethod(ClickEvent.class, "clickEvent", ClickEvent.Action.class, String.class);

		if (ReflectionUtil.getMethod(Times.class, "times", Duration.class, Duration.class, Duration.class) == null)
			legacyTimesOfMethod = ReflectionUtil.getMethod(Times.class, "of", Duration.class, Duration.class, Duration.class);
	}

	/**
	 * Return the click event's text payload, or null when the payload is not text
	 * (change_page, show_dialog and custom click events cannot be read as a string).
	 *
	 * @param clickEvent
	 * @return the text payload, or null
	 */
	@SuppressWarnings("rawtypes")
	public static String getClickEventValue(final ClickEvent clickEvent) {
		if (hasClickPayload)
			return ClickPayloadAccessor.getValue(clickEvent);

		return ReflectionUtil.invoke(legacyClickValueMethod, clickEvent);
	}

	/**
	 * Create a new click event with the given text payload.
	 *
	 * @param action
	 * @param value
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static ClickEvent newClickEvent(final ClickEvent.Action action, final String value) {
		if (hasClickPayloadFactory)
			return ClickPayloadAccessor.newClickEvent(action, value);

		return ReflectionUtil.invoke(legacyClickFactoryMethod, (Object) null, action, value);
	}

	/**
	 * Return true if custom click events can carry an NBT payload, that is Adventure 4.23.0
	 * or newer. Callers must degrade to a regular run_command click when this is false.
	 *
	 * @return
	 */
	public static boolean hasCustomClickEvent() {
		return hasCustomClickEvent;
	}

	/**
	 * Create new title times from the given durations.
	 *
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 * @return
	 */
	public static Times newTitleTimes(final Duration fadeIn, final Duration stay, final Duration fadeOut) {
		return legacyTimesOfMethod == null
				? Times.times(fadeIn, stay, fadeOut)
				: (Times) ReflectionUtil.invoke(legacyTimesOfMethod, (Object) null, fadeIn, stay, fadeOut);
	}
}

/**
 * Holds every direct ClickEvent.Payload call, kept out of {@link RemainCore} on purpose.
 *
 * Loading a class resolves the types its own methods name, so a single Payload reference
 * anywhere in RemainCore made RemainCore itself unloadable on the many servers bundling
 * Adventure older than 4.22, and since every platform Remain extends it, the whole plugin
 * died at enable time with NoClassDefFoundError. A runtime flag cannot guard that, only a
 * separate class can: this one is loaded the first time a method below runs, which happens
 * only behind RemainCore's payload checks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ClickPayloadAccessor {

	/**
	 * Return true if Adventure carries the 4.25.0 clickEvent(Action, Payload) factory.
	 */
	static boolean hasFactory() {
		return ReflectionUtil.getMethod(ClickEvent.class, "clickEvent", ClickEvent.Action.class, ClickEvent.Payload.class) != null;
	}

	/**
	 * Return the click event's text payload, or null when the payload carries something else.
	 */
	@SuppressWarnings("rawtypes")
	static String getValue(final ClickEvent clickEvent) {
		return clickEvent.payload() instanceof ClickEvent.Payload.Text ? ((ClickEvent.Payload.Text) clickEvent.payload()).value() : null;
	}

	/**
	 * Create a click event carrying the given text payload.
	 */
	@SuppressWarnings("rawtypes")
	static ClickEvent newClickEvent(final ClickEvent.Action action, final String value) {
		return ClickEvent.clickEvent(action, ClickEvent.Payload.string(value));
	}
}
