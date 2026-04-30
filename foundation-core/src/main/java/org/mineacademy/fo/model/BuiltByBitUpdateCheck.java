package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.NetworkUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.Getter;

/**
 * A class that checks for plugin updates on BuiltByBit.
 */
public final class BuiltByBitUpdateCheck implements Runnable {

	/**
	 * Is a new version available? Run this class first to check.
	 */
	@Getter
	private static boolean newVersionAvailable = false;

	/**
	 * The new version string, null if not available.
	 */
	@Getter
	private static String newVersionString = null;

	/**
	 * The new version release date, 0 if not available.
	 */
	@Getter
	private static long newVersionReleaseDate = 0;

	@Override
	public void run() {
		final FoundationPlugin plugin = Platform.getPlugin();

		if (plugin.getBuiltByBitSharedToken() != null && plugin.getBuiltByBitId() != -1) {
			JsonObject json;

			try {
				json = NetworkUtil.getJson("https://api.builtbybit.com/v1/resources/" + plugin.getBuiltByBitId() + "/versions/latest", new HashMap<>(), CommonCore.newHashMap("Authorization", "Shared " + plugin.getBuiltByBitSharedToken()));

			} catch (final Throwable t) {
				CommonCore.log("Error checking for plugin update. Got: " + t.getMessage());

				return;
			}

			if (json.has("result")) {
				if (json.get("result").getAsString().equals("success") && json.has("data")) {
					final JsonObject data = json.get("data").getAsJsonObject();

					final String versionString = data.get("name").getAsString();
					final long releaseDate = data.get("release_date").getAsLong();

					if (isNewerVersion(plugin.getVersion(), versionString)) {
						BuiltByBitUpdateCheck.newVersionAvailable = true;
						BuiltByBitUpdateCheck.newVersionString = versionString;
						BuiltByBitUpdateCheck.newVersionReleaseDate = releaseDate * 1000;

						if (SimpleSettings.NOTIFY_NEW_VERSIONS)
							for (final SimpleComponent component : getUpdateMessage())
								CommonCore.log(component.toLegacySection(null));
					}

				} else
					CommonCore.warning("Got failed result connecting to BuiltByBit to check for " + plugin.getName() + " update. Expected result to be 'success' and data field, got: " + json);

			} else
				CommonCore.warning("Unexpected BuiltByBit response format while checking for " + plugin.getName() + " plugin updates. Expected 'result' field, got: " + json);
		}
	}

	/**
	 * Return the update message.
	 *
	 * @see Lang
	 * @return
	 */
	public static SimpleComponent[] getUpdateMessage() {
		ValidCore.checkBoolean(newVersionAvailable, "Cannot call getUpdateMessage() when no new version is available!");

		final List<SimpleComponent> components = new ArrayList<>();

		// Need to be loaded manually to replace {url} in mini click tag
		for (final JsonElement element : Lang.dictionary().getAsJsonArray("plugin-update-notification")) {
			String line = element.getAsString();

			line = line.replace("{plugin}", Platform.getPlugin().getName());
			line = line.replace("{plugin_name}", Platform.getPlugin().getName());
			line = line.replace("{version}", Platform.getPlugin().getVersion());
			line = line.replace("{plugin_version}", Platform.getPlugin().getVersion());
			line = line.replace("{new_version}", newVersionString);
			line = line.replace("{release_date}", TimeUtil.getFormattedDateShort(newVersionReleaseDate));
			line = line.replace("{url}", "https://builtbybit.com/resources/" + Platform.getPlugin().getBuiltByBitId() + "/updates");

			components.add(SimpleComponent.fromMiniAmpersand(line));
		}

		// trick to replace {url} in click minimessage tag
		return components.toArray(new SimpleComponent[components.size()]);
	}

	/*
	 * Helper method to check if the new version is newer than the current version.
	 */
	private static boolean isNewerVersion(String currentVersion, String newVersion) {
		final int[] current = parseVersion(currentVersion);
		final int[] latest = parseVersion(newVersion);

		if (latest[0] > current[0])
			return true;
		if (latest[0] < current[0])
			return false;

		if (latest[1] > current[1])
			return true;
		if (latest[1] < current[1])
			return false;

		return latest[2] > current[2];
	}

	/*
	 * Parse a version string into an int[3] of {major, minor, patch}, treating any
	 * missing or non-numeric segment as 0. Tolerates labels, prefixes, suffixes,
	 * duplicate or edge dots (e.g. "v1.0", "1.0.0-RC1", "Update .1.2", "1..0", "1.").
	 */
	private static int[] parseVersion(String version) {
		String stripped = version.replaceAll("[^\\d.]", "");
		stripped = stripped.replaceAll("\\.+", ".");
		stripped = stripped.replaceAll("^\\.|\\.$", "");

		final int[] parts = { 0, 0, 0 };

		if (stripped.isEmpty())
			return parts;

		final String[] split = stripped.split("\\.");
		final int limit = Math.min(split.length, parts.length);

		for (int i = 0; i < limit; i++)
			parts[i] = Integer.parseInt(split[i]);

		return parts;
	}
}
