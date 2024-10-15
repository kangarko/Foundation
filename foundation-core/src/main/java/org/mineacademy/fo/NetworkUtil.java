package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * Utility class for network operations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkUtil {

	/**
	 * The user agent to use when connecting to remote URLs. Helps with domains blocking Java connections.
	 */
	@Getter
	@Setter
	private static String remoteUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36";

	/**
	 * Makes a GET request to the specified endpoint and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @return A JsonObject containing the response, or null if the request or parsing fails.
	 */
	public static JsonObject getJson(String endpoint) {
		return getJson(endpoint, new HashMap<>());
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params A map of query parameters to include in the request.
	 * @return
	 */
	public static JsonObject getJson(String endpoint, Map<String, String> params) {
		return getJson(endpoint, params, new HashMap<>());
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params   A map of query parameters to include in the request.
	 * @param requestProperties The request properties to include in the request, null to ignore.
	 *
	 * @return A JsonObject containing the response, or null if the request or parsing fails.
	 */
	public static JsonObject getJson(String endpoint, Map<String, String> params, Map<String, String> requestProperties) {
		requestProperties.put("Content-Type", "application/json");

		final String response = get(endpoint, params, requestProperties);

		try {
			return CommonCore.GSON.fromJson(response, JsonObject.class);

		} catch (final Throwable ex) {
			CommonCore.throwError(ex,
					"Error converting 'get' request to JSON!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties);
		}

		return null;
	}

	/**
	 * Makes a POST request to the specified endpoint and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @return A JsonObject containing the response, or null if the request or parsing fails.
	 */
	public static JsonObject postJson(String endpoint) {
		return postJson(endpoint, new HashMap<>());
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params A map of parameters to include in the request.
	 * @return
	 */
	public static JsonObject postJson(String endpoint, Map<String, Object> params) {
		return postJson(endpoint, params, new HashMap<>());
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a JSON object.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params  A map of parameters to include in the request.
	 * @param requestProperties The request properties to include in the request, null to ignore.
	 *
	 * @return A JsonObject containing the response, or null if the request or parsing fails.
	 */
	public static JsonObject postJson(String endpoint, Map<String, Object> params, Map<String, String> requestProperties) {
		requestProperties.put("Content-Type", "application/json");

		final String response = post(endpoint, params, requestProperties);

		try {
			return CommonCore.GSON.fromJson(response, JsonObject.class);

		} catch (final Throwable ex) {
			CommonCore.throwError(ex,
					"Error converting 'post' request to JSON!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties);
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
		return get(endpoint, new HashMap<>());
	}

	/**
	 * Makes a GET request to the specified endpoint with the given parameters and returns the response as a string.
	 *
	 * @param endpoint The URL to send the GET request to.
	 * @param params A map of query parameters to include in the request, null to ignore.
	 * @return
	 */
	public static String get(String endpoint, Map<String, String> params) {
		return get(endpoint, params, new HashMap<>());
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
	public static String get(@NonNull String endpoint, @NonNull Map<String, String> params, @NonNull Map<String, String> requestProperties) {

		// Bust cache
		params.put("t", String.valueOf(System.currentTimeMillis()));

		try {
			if (params != null && !params.isEmpty()) {
				final StringBuilder endpointBuilder = new StringBuilder(endpoint).append("?");

				for (final Map.Entry<String, String> entry : params.entrySet()) {
					final String value = entry.getValue();

					endpointBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
				}

				endpoint = endpointBuilder.toString();
			}

			if (endpoint.endsWith("&"))
				endpoint = endpoint.substring(0, endpoint.length() - 1);

			final URL url = new URL(endpoint);
			final URLConnection connection = url.openConnection();

			setUserAgentAndRequestProperties(connection, params, requestProperties);

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				final StringBuilder responseBuilder = new StringBuilder();
				String input;

				while ((input = reader.readLine()) != null)
					responseBuilder.append(input);

				return responseBuilder.toString();
			}

		} catch (final Exception ex) {
			CommonCore.throwError(ex,
					"Error issuing a 'get' request!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties);

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
		return post(endpoint, new HashMap<>());
	}

	/**
	 * Makes a POST request to the specified endpoint with the given parameters and returns the response as a string.
	 *
	 * @param endpoint The URL to send the POST request to.
	 * @param params A map of parameters to include in the request, null to ignore.
	 * @return
	 */
	public static String post(String endpoint, Map<String, Object> params) {
		return post(endpoint, params, new HashMap<>());
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
	public static String post(@NonNull String endpoint, @NonNull Map<String, Object> params, @NonNull Map<String, String> requestProperties) {

		// Bust cache
		params.put("t", String.valueOf(System.currentTimeMillis()));

		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			setUserAgentAndRequestProperties(connection, params, requestProperties);

			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			try (OutputStream output = connection.getOutputStream()) {
				final byte[] input = CommonCore.GSON.toJson(params).getBytes("utf-8");

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
			CommonCore.throwError(ex,
					"Error issuing a 'post' request!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties);

			return "";
		}
	}

	/*
	 * Set user agent and request properties
	 */
	private static void setUserAgentAndRequestProperties(URLConnection connection, Map<String, ?> params, Map<String, ?> requestProperties) {

		// Set user agent (some webhosts blocks Java agent)
		if (remoteUserAgent != null)
			connection.setRequestProperty("User-Agent", remoteUserAgent);

		for (final Map.Entry<String, ?> entry : requestProperties.entrySet()) {
			ValidCore.checkBoolean(entry.getValue() instanceof String, "Request property key can only be a String, found " + entry.getValue().getClass().getSimpleName() + " in: " + requestProperties);

			connection.setRequestProperty(entry.getKey(), entry.getValue().toString());
		}
	}
}
