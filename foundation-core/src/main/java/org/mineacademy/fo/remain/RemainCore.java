package org.mineacademy.fo.remain;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import org.mineacademy.fo.ReflectionUtilCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.Platform;

import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class RemainCore {

	/**
	 * The Google Json instance
	 */
	private final static Gson GSON = new Gson();

	// ----------------------------------------------------------------------------------------------------
	// Set manually
	// ----------------------------------------------------------------------------------------------------

	/**
	 * The server-name from server.properties (is lacking on new Minecraft version so we have to readd it back)
	 */
	private static String serverName;

	/**
	 * The internal private section path data class
	 */
	private static Class<?> sectionPathDataClass = null;

	/**
	 * Initialize all fields and methods automatically when we set the plugin
	 */
	static void init() {
		try {
			sectionPathDataClass = ReflectionUtilCore.lookupClass("org.bukkit.configuration.SectionPathData");
		} catch (final Throwable ex) {
			// Unavailable
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Server name
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return the server name identifier
	 *
	 * @return
	 */
	public static String getServerName() {
		if (!hasServerName())
			throw new IllegalArgumentException("Please instruct developer of " + Platform.getPlugin().getName() + " to call Remain#setServerName");

		return serverName;
	}

	/**
	 * Return true if the server-name property in server.properties got modified
	 *
	 * @return
	 */
	public static boolean hasServerName() {
		return serverName != null && !serverName.isEmpty() && !serverName.contains("mineacademy.org/server-properties") && !"undefined".equals(serverName) && !"Unknown Server".equals(serverName);
	}

	/**
	 * Set the server name identifier
	 *
	 * @param serverName
	 */
	public static void setServerName(String serverName) {
		RemainCore.serverName = serverName;
	}

	// ----------------------------------------------------------------------------------------------------
	// Section path data
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Converts the given object that may be a SectionPathData for MC 1.18 back into its root data
	 * such as MemorySection
	 *
	 * @param objectOrSectionPathData
	 * @return
	 *
	 * @deprecated legacy code, will be removed
	 */
	@Deprecated // TODO remove
	public static Object getRootOfSectionPathData(Object objectOrSectionPathData) {
		if (objectOrSectionPathData != null && objectOrSectionPathData.getClass() == sectionPathDataClass)
			objectOrSectionPathData = ReflectionUtilCore.invoke("getData", objectOrSectionPathData);

		return objectOrSectionPathData;
	}

	/**
	 * Return true if the given object is a memory section
	 *
	 * @param obj
	 * @return
	 */
	public static boolean isMemorySection(Object obj) {
		return obj != null && sectionPathDataClass == obj.getClass();
	}

	// ----------------------------------------------------------------------------------------------------
	// Misc
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Converts a component to JSON
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToJson(Component component) {
		return GsonComponentSerializer.gson().serialize(component);
	}

	/**
	 * Serializes the component into legacy text
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToLegacy(Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
	}

	/**
	 * Serializes the component into mini message
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToMini(Component component) {
		return MiniMessage.miniMessage().serialize(component);
	}

	/**
	 * Serializes the component into plain text
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToPlain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component).trim();
	}

	/**
	 * Converts a json string to Adventure component
	 *
	 * @param json
	 * @return
	 */
	public static Component convertJsonToAdventure(String json) {
		return GsonComponentSerializer.gson().deserialize(json);
	}

	/**
	 *
	 * @param componentJson
	 * @return
	 */
	public static String convertJsonToLegacy(String componentJson) {
		return convertAdventureToLegacy(convertJsonToAdventure(componentJson));
	}

	/**
	 * Creates a new adventure component from legacy text with {@link CompChatColor#COLOR_CHAR} colors replaced
	 *
	 * @param legacyText
	 * @return
	 */
	public static Component convertLegacyToAdventure(String legacyText) {
		return LegacyComponentSerializer.legacySection().deserialize(legacyText);
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 *
	 * @param message
	 * @return
	 */
	public static String convertLegacyToJson(final String message) {
		return GsonComponentSerializer.gson().serialize(convertLegacyToAdventure(message));
	}

	/**
	 * Convert the given json into list
	 *
	 * @param json
	 * @return
	 */
	public static List<String> fromJsonList(String json) {
		return GSON.fromJson(json, List.class);
	}

	/**
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param audience
	 * @param message
	 * @param progress
	 */
	public static void sendBossbarPercent(final Audience audience, final Component message, final float progress) {
		sendBossbarPercent(audience, message, progress, null, null);
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param audience
	 * @param message
	 * @param progress
	 * @param color
	 * @param overlay
	 */
	public static void sendBossbarPercent(final Audience audience, final Component message, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		Platform.sendBossbarPercent(audience, message, progress, color, overlay);
	}

	/**
	 * Send boss bar only for limited time
	 *
	 * @param audience
	 * @param message
	 * @param seconds
	 */
	public static void sendBossbarTimed(final Audience audience, final Component message, final int seconds, final float progress) {
		sendBossbarTimed(audience, message, seconds, progress, null, null);
	}

	/**
	 * Send boss bar only for limited time
	 *
	 * @param audience
	 * @param message
	 * @param seconds
	 * @param progress
	 * @param color
	 * @param overlay
	 */
	public static void sendBossbarTimed(final Audience audience, final Component message, final int seconds, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		Platform.sendBossbarTimed(audience, message, seconds, progress, color, overlay);
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final Audience sender, final String json) {
		sender.sendMessage(convertJsonToAdventure(json));
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null. (1.8+)
	 * Texts will be colorized.
	 *
	 * @param audience the player
	 * @param header the header
	 * @param footer the footer
	 */
	public static void sendTablist(final Audience audience, final Component header, final Component footer) {
		audience.sendPlayerListHeaderAndFooter(header, footer);
	}

	/**
	 * Sends a title to the player (1.8+) for three seconds
	 *
	 * @param audience
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitle(final Audience audience, final Component title, final Component subtitle) {
		sendTitle(audience, 20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player (1.8+) Texts will be colorized.
	 *
	 * @param audience   the player
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public static void sendTitle(final Audience audience, final int fadeIn, final int stay, final int fadeOut, final Component title, final Component subtitle) {
		audience.showTitle(Title.title(title, subtitle, Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
	}

	/**
	 * Resets the title that is being displayed to the player (1.8+)
	 *
	 * @param audience the player
	 */
	public static void resetTitle(final Audience audience) {
		audience.resetTitle();
	}

	/**
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrows.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
	}

	/**
	 * Some legacy MC versions cannot properly handle 8-char hex colors
	 * so we convert them to 6-char hex colors
	 *
	 * @param component
	 * @return
	 */
	/*private static Component fixHexColors(Component component) {
		final Pattern pattern = Pattern.compile("#[0-9a-fA-F]{8}");
		final Matcher matcher = pattern.matcher(RemainCore.convertAdventureToJson(component));
		final StringBuffer builder = new StringBuffer();

		while (matcher.find()) {
			final String hex = matcher.group();
			final String editedHex = "#" + hex.substring(3);

			matcher.appendReplacement(builder, editedHex);
		}

		matcher.appendTail(builder);

		return RemainCore.convertJsonToAdventure(builder.toString());
	}*/

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String convertListToJson(final Collection<String> list) {
		return GSON.toJson(list);
	}
}

/**
 * A wrapper for Spigot
 */
final class SneakyThrows {

	public static void sneaky(final Throwable t) {
		throw SneakyThrows.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}