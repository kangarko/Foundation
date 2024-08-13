package org.mineacademy.fo.model;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.RequiredArgsConstructor;

/**
 * Utility class for connecting to Mojang servers to get the players name from a
 * given UUID
 */
@RequiredArgsConstructor
public class UUIDToNameConverter implements Callable<String> {

	/**
	 * The URL to connect to
	 */
	private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

	/**
	 * The JSON parser library
	 */
	private final Gson gson = new Gson();

	/**
	 * The UUID to convert to name
	 */
	private final UUID uuid;

	/**
	 * Attempts to connect to Mojangs servers to retrieve the current player
	 * username from his unique id
	 * <p>
	 * Runs on the main thread
	 */
	@Override
	public String call() throws Exception {

		final HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL + this.uuid.toString().replace("-", "")).openConnection();
		final JsonObject response = this.gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
		final String name = response.get("name").getAsString();

		if (name == null)
			return "";

		final String cause = response.get("cause").getAsString();
		final String errorMessage = response.get("errorMessage").getAsString();

		if (cause != null && cause.length() > 0)
			throw new IllegalStateException(errorMessage);

		return name;
	}
}
