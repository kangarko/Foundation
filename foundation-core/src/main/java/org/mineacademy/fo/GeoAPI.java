package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import org.mineacademy.fo.collection.expiringmap.ExpiringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Utility class for resolving geographical information about players.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeoAPI {

	/**
	 * The cached responses per IP addresses, records removed after 1 hour to prevent them stacking up in memory.
	 */
	private static final ExpiringMap<String, GeoResponse> cache = ExpiringMap.builder().expiration(1, TimeUnit.HOURS).build();

	/**
	 * Returns a {@link GeoResponse} with geographic data for the given IP address
	 * THIS IS A BLOCKING OPERATION THAT SHOULD BE RUN ASYNC. We will cache the response
	 * if it has been looked up for the given IP for maximum performance.
	 *
	 * @param ip
	 * @return
	 */
	public static GeoResponse getCountry(InetSocketAddress ip) {
		GeoResponse response = new GeoResponse("", "", "", "");

		if (ip == null)
			return response;

		if (ip.getHostString().equals("127.0.0.1") || ip.getHostString().equals("0.0.0.0"))
			return new GeoResponse("local", "-", "local", "-");

		if (cache.containsKey(ip.toString()) || cache.containsValue(response))
			return cache.get(ip.toString());

		try {
			final URL url = new URL("http://ip-api.com/json/" + ip.getHostName());
			final URLConnection con = url.openConnection();
			con.setConnectTimeout(3000);
			con.setReadTimeout(3000);

			try (final BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String page = "";
				String input;

				while ((input = r.readLine()) != null)
					page += input;

				response = new GeoResponse(getJson(page, "country"), getJson(page, "countryCode"), getJson(page, "regionName"), getJson(page, "isp"));
				cache.put(ip.toString(), response);
			}

		} catch (final NoRouteToHostException ex) {
			// Firewall or internet access denied

		} catch (final SocketTimeoutException ex) { // hide
		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		return response;
	}

	private static String getJson(String page, String element) {
		return page.contains("\"" + element + "\":\"") ? page.split("\"" + element + "\":\"")[1].split("\",")[0] : "";
	}

	/**
	 * The response we get from an external server, cached since the country does not change for the IP does it? :)
	 */
	@RequiredArgsConstructor
	@Getter
	public static final class GeoResponse {
		private final String countryName, countryCode, regionName, isp;
	}
}
