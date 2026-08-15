package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.time.Duration;

import org.mineacademy.fo.ReflectionUtil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
		hasClickPayloadFactory = hasClickPayload && ReflectionUtil.getMethod(ClickEvent.class, "clickEvent", ClickEvent.Action.class, ClickEvent.Payload.class) != null;

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
			return clickEvent.payload() instanceof ClickEvent.Payload.Text ? ((ClickEvent.Payload.Text) clickEvent.payload()).value() : null;

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
		return hasClickPayloadFactory
				? ClickEvent.clickEvent(action, ClickEvent.Payload.string(value))
				: (ClickEvent) ReflectionUtil.invoke(legacyClickFactoryMethod, (Object) null, action, value);
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
