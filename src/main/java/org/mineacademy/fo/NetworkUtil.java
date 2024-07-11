package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
	 * Mask user agent to Chrome 126 by default, you can override this in your request properties.
	 */
	public final static String HTTP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

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
	 * @param params A map of query parameters to include in the request.
	 * @return
	 */
	public static JSONObject getJson(String endpoint, SerializedMap params) {
		return getJson(endpoint, params, null);
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params   A map of query parameters to include in the request.
	 * @param requestProperties The request properties to include in the request, null to ignore.
	 *
	 * @return A JSONObject containing the response, or null if the request or parsing fails.
	 */
	public static JSONObject getJson(String endpoint, SerializedMap params, SerializedMap requestProperties) {
		final String response = get(endpoint, params, requestProperties);

		try {
			final Object json = JSONParser.deserialize(response);

			try {
				return (JSONObject) json;

			} catch (final ClassCastException ex) {
				Common.throwError(ex,
						"Failed to cast JSON to JSONObject!",
						"",
						"Endpoint: " + endpoint,
						"Params: " + params,
						"Request Properties: " + requestProperties,
						"Response: " + response,
						"Raw JSON (" + json.getClass().getSimpleName() + "): " + json);
			}

		} catch (final JSONParseException ex) {
			Common.throwError(ex,
					"Failed to get JSON!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties,
					"Response: " + response);
		}

		return null;
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
	 * @param params A map of parameters to include in the request.
	 * @return
	 */
	public static JSONObject postJson(String endpoint, SerializedMap params) {
		return postJson(endpoint, params, null);
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params  A map of parameters to include in the request.
	 * @param requestProperties The request properties to include in the request, null to ignore.
	 *
	 * @return A JSONObject containing the response, or null if the request or parsing fails.
	 */
	public static JSONObject postJson(String endpoint, SerializedMap params, SerializedMap requestProperties) {
		final String response = post(endpoint, params, requestProperties);

		try {
			final Object json = JSONParser.deserialize(response);

			try {
				return (JSONObject) json;

			} catch (final ClassCastException ex) {
				Common.throwError(ex,
						"Failed to cast JSON to JSONObject!",
						"",
						"Endpoint: " + endpoint,
						"Params: " + params,
						"Request Properties: " + requestProperties,
						"Response: " + response,
						"Raw JSON (" + json.getClass().getSimpleName() + "): " + json);
			}

		} catch (final JSONParseException ex) {
			Common.throwError(ex,
					"Failed to post JSON!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties,
					"Response: " + response);
		}

		return null;
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
	 * @param params A map of query parameters to include in the request, null to ignore.
	 * @return
	 */
	public static String get(String endpoint, SerializedMap params) {
		return get(endpoint, params, null);
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a string.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params   A map of query parameters to include in the request, null to ignore.
	 * @param requestProperties The request properties to include in the request, null to ignore.
	 *
	 * @return A string containing the response, or an empty string if the request fails.
	 */
	public static String get(String endpoint, SerializedMap params, SerializedMap requestProperties) {

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

			connection.setRequestProperty("User-Agent", HTTP_USER_AGENT);

			if (requestProperties != null)
				for (final Map.Entry<String, Object> entry : requestProperties.entrySet())
					connection.setRequestProperty(entry.getKey(), entry.getValue().toString());

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
	 * @param params A map of parameters to include in the request, null to ignore.
	 * @return
	 */
	public static String post(String endpoint, SerializedMap params) {
		return post(endpoint, params, null);
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a string.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params   A map of parameters to include in the request, null to ignore.
	 * @param requestProperties The request properties to include in the request, null to ignore.
	 *
	 * @return A string containing the response, or an empty string if the request fails.
	 */
	public static String post(String endpoint, SerializedMap params, SerializedMap requestProperties) {

		if (params == null)
			params = new SerializedMap();

		// Bust the cache
		params.put("t", System.currentTimeMillis());

		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", HTTP_USER_AGENT);

			if (requestProperties != null)
				for (final Map.Entry<String, Object> entry : requestProperties.entrySet())
					connection.setRequestProperty(entry.getKey(), entry.getValue().toString());

			if (params != null && !params.isEmpty())
				try (OutputStream output = connection.getOutputStream()) {
					final byte[] input = params.toJson().getBytes("utf-8");

					output.write(input, 0, input.length);
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
