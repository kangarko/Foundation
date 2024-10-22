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

					if (isNewerVersion(plugin.getVersion(), versionString) && SimpleSettings.NOTIFY_NEW_VERSIONS) {
						BuiltByBitUpdateCheck.newVersionAvailable = true;
						BuiltByBitUpdateCheck.newVersionString = versionString;
						BuiltByBitUpdateCheck.newVersionReleaseDate = releaseDate * 1000;

						CommonCore.log(getUpdateMessage());
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

			components.add(SimpleComponent.fromMini(line));
		}

		// trick to replace {url} in click minimessage tag
		return components.toArray(new SimpleComponent[components.size()]);
	}

	/*
	 * Helper method to check if the new version is newer than the current version.
	 */
	private static boolean isNewerVersion(String currentVersion, String newVersion) {
		currentVersion = currentVersion.replaceAll("[^\\d.]", "");
		newVersion = newVersion.replaceAll("[^\\d.]", "");

		final String[] currentParts = currentVersion.split("\\.");
		final String[] newParts = newVersion.split("\\.");

		// To handle missing minor or patch, we assign 0 to missing parts.
		final int currentMajor = Integer.parseInt(currentParts[0]);
		final int currentMinor = currentParts.length > 1 ? Integer.parseInt(currentParts[1]) : 0;
		final int currentPatch = currentParts.length > 2 ? Integer.parseInt(currentParts[2]) : 0;

		final int newMajor = Integer.parseInt(newParts[0]);
		final int newMinor = newParts.length > 1 ? Integer.parseInt(newParts[1]) : 0;
		final int newPatch = newParts.length > 2 ? Integer.parseInt(newParts[2]) : 0;

		if (newMajor > currentMajor)
			return true;
		if (newMajor < currentMajor)
			return false;

		if (newMinor > currentMinor)
			return true;
		if (newMinor < currentMinor)
			return false;

		return newPatch > currentPatch;
	}
}
