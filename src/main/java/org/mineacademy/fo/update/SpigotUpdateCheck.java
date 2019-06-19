package org.mineacademy.fo.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;

/**
 * A simple class performing an update check for Spigot free and premium resources
 */
public class SpigotUpdateCheck implements Runnable {

	/**
	 * The Spigot resource ID for the given plugin
	 */
	@Getter
	private final int resourceId;

	/**
	 * Did we found that a newer version is available?
	 */
	@Getter
	private boolean newerVersionAvailable = false;

	/**
	 * The newer version
	 */
	@Getter
	private String newVersion = "";

	/**
	 * Initializes a new instance.
	 *
	 * @param resourceId the id of the plugin at Spigot's page.
	 */
	protected SpigotUpdateCheck(int resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * The main method in which the update check takes place.
	 */
	@Override
	public void run() {
		if (resourceId == -1)
			return;

		final String currentVersion = SimplePlugin.getVersion();

		if (currentVersion.contains("SNAPSHOT") || currentVersion.contains("DEV"))
			return;

		try {
			final HttpURLConnection con = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openConnection();
			con.setRequestMethod("GET");

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				final String line = reader.readLine();

				newVersion = line;
			}

			if (newVersion.isEmpty())
				return;

			if (isNewerVersion(currentVersion, newVersion)) {
				newerVersionAvailable = true;

				Common.log(getUpdateMessage());
			}

		} catch (final UnknownHostException ex) {
			Common.log("Could not check for update from " + ex.getMessage() + ".");

		} catch (final IOException ex) {
			if (ex.getMessage().startsWith("Server returned HTTP response code: 403")) {
				// no permission
			} else if (ex.getMessage().startsWith("Server returned HTTP response code:"))
				Common.log("Could not check for update, SpigotMC site appears to be down (or unaccessible): " + ex.getMessage());

			else
				ex.printStackTrace();

		} catch (final Exception ex) {
			ex.printStackTrace();

		}
	}

	/**
	 * Returns true if the remote version is greater than the current version
	 *
	 * @param current
	 * @param remote
	 * @return
	 */
	private boolean isNewerVersion(String current, String remote) {
		if (remote.contains("-LEGACY"))
			return false;

		String[] currParts = removeTagsInNumber(current).split("\\.");
		String[] remoteParts = removeTagsInNumber(remote).split("\\.");

		if (currParts.length != remoteParts.length) {
			final boolean olderIsLonger = currParts.length > remoteParts.length;
			final String[] modifiedParts = new String[olderIsLonger ? currParts.length : remoteParts.length];

			for (int i = 0; i < (olderIsLonger ? currParts.length : remoteParts.length); i++)
				modifiedParts[i] = olderIsLonger ? remoteParts.length > i ? remoteParts[i] : "0" : currParts.length > i ? currParts[i] : "0";

			if (olderIsLonger)
				remoteParts = modifiedParts;
			else
				currParts = modifiedParts;
		}

		for (int i = 0; i < currParts.length; i++) {
			if (Integer.parseInt(currParts[i]) > Integer.parseInt(remoteParts[i]))
				return false;

			if (Integer.parseInt(remoteParts[i]) > Integer.parseInt(currParts[i]))
				return true;
		}

		return false;
	}

	/**
	 * Removes some tags such as -BETA in the version number
	 *
	 * @param raw
	 * @return
	 */
	private final String removeTagsInNumber(String raw) {
		return raw.replace("-BETA", "").replace("-ALPHA", "").replace("-DEV", "").replace("-RC", "");
	}

	/**
	 * Returns the update message, by default {@link SimpleLocalization#UPDATE_AVAILABLE}
	 *
	 * @return
	 */
	public String getUpdateMessage() {
		return SimpleLocalization.UPDATE_AVAILABLE
				.replace("{resourceId}", resourceId + "")
				.replace("{plugin.name}", SimplePlugin.getNamed())
				.replace("{new}", newVersion)
				.replace("{curr}", SimplePlugin.getVersion())
				.replace("{current}", SimplePlugin.getVersion())
				.replace("{user_id}", "%%__USER__%%");
	}
}
