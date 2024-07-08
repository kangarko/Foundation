package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.jsonsimple.JSONObject;
import org.mineacademy.fo.jsonsimple.JSONParseException;
import org.mineacademy.fo.jsonsimple.JSONParser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for network operations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkUtil {

	/**
	 * Makes a GET request to the specified endpoint and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @return A JSONObject containing the response, or null if the request or parsing fails.
	 */
	public static JSONObject getJson(String endpoint) {
		return getJson(endpoint, null);
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params   A map of query parameters to include in the request.
	 * @return A JSONObject containing the response, or null if the request or parsing fails.
	 */
	public static JSONObject getJson(String endpoint, SerializedMap params) {
		final String response = get(endpoint, params);

		try {
			return (JSONObject) JSONParser.deserialize(response);

		} catch (final JSONParseException ex) {
			Common.throwError(ex,
					"Failed to get JSON! ",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Response: " + response);

			return null;
		}
	}

	/**
	 * Makes a POST request to the specified endpoint and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @return A JSONObject containing the response, or null if the request or parsing fails.
	 */
	public static JSONObject postJson(String endpoint) {
		return postJson(endpoint, null);
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params  A map of parameters to include in the request.
	 * @return A JSONObject containing the response, or null if the request or parsing fails.
	 */
	public static JSONObject postJson(String endpoint, SerializedMap params) {
		final String response = post(endpoint, params);

		try {
			return (JSONObject) JSONParser.deserialize(response);

		} catch (final JSONParseException ex) {
			Common.throwError(ex,
					"Failed to post JSON! ",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Response: " + response);

			return null;
		}
	}

	/**
	 * Makes a GET request to the specified endpoint and returns the response as a string.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @return A string containing the response, or an empty string if the request fails.
	 */
	public static String get(String endpoint) {
		return get(endpoint, null);
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a string.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params   A map of query parameters to include in the request.
	 * @return A string containing the response, or an empty string if the request fails.
	 */
	public static String get(String endpoint, SerializedMap params) {

		if (params == null)
			params = new SerializedMap();

		// Bust the cache
		params.put("t", System.currentTimeMillis());

		try {
			if (params != null && !params.isEmpty()) {
				final StringBuilder endpointBuilder = new StringBuilder(endpoint).append("?");

				for (final Map.Entry<String, Object> entry : params.entrySet())
					endpointBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue().toString(), "UTF-8")).append("&");

				endpoint = endpointBuilder.toString();
			}

			final URL url = new URL(endpoint);
			final URLConnection connection = url.openConnection();

			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				final StringBuilder responseBuilder = new StringBuilder();
				String input;

				while ((input = reader.readLine()) != null)
					responseBuilder.append(input);

				return responseBuilder.toString();
			}

		} catch (final Exception ex) {
			Common.throwError(ex, "Failed to read response from " + endpoint + " with params " + params);

			return "";
		}
	}

	/**
	 * Makes a POST request to the specified endpoint and returns the response as a string.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @return A string containing the response, or an empty string if the request fails.
	 */
	public static String post(String endpoint) {
		return post(endpoint, null);
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a string.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params   A map of parameters to include in the request.
	 * @return A string containing the response, or an empty string if the request fails.
	 */
	public static String post(String endpoint, SerializedMap params) {

		if (params == null)
			params = new SerializedMap();

		// Bust the cache
		params.put("t", System.currentTimeMillis());

		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

			if (params != null && !params.isEmpty()) {
				final byte[] postDataBytes = params.toJson().getBytes("UTF-8");

				try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
					wr.write(postDataBytes);
				}
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				final StringBuilder responseBuilder = new StringBuilder();
				String input;

				while ((input = reader.readLine()) != null)
					responseBuilder.append(input);

				return responseBuilder.toString();
			}

		} catch (final Exception ex) {
			Common.throwError(ex, "Failed to read response from " + endpoint + " with params " + params);

			return "";
		}
	}
}
