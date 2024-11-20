package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;

import org.mineacademy.fo.GeoAPI;
import org.mineacademy.fo.GeoAPI.GeoResponse;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;

import com.velocitypowered.api.proxy.Player;

/**
 * Expands the functionality of {@link Variables} to include Bukkit-specific variables,
 * and also hooks into PlaceholderAPI.
 */
final class VelocityVariableCollector implements Variables.Collector {

	@Override
	public SimpleComponent replaceVariable(String pluginIdentifier, String params, String variable, FoundationPlayer audience) {
		final Player player = audience != null && audience.isPlayer() ? ((VelocityPlayer) audience).getPlayer() : null;

		if ("server_version".equals(variable))
			return SimpleComponent.fromPlain(Platform.getPlatformVersion());

		else if ("player".equals(variable) || "player_name".equals(variable))
			return SimpleComponent.fromPlain(audience == null ? "" : audience.getName());

		else if ("player_uuid".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : player.getUniqueId().toString());

		else if ("player_server".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : player.getCurrentServer().get().getServerInfo().getName());

		else if ("player_ip".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : player.getRemoteAddress().getAddress().toString().split("\\:")[0]);

		else if ("country_code".equals(variable) || "country_name".equals(variable) || "region_name".equals(variable) || "isp".equals(variable)) {
			final InetSocketAddress ip = audience == null ? null : audience.getAddress();

			if (ip == null)
				return SimpleComponent.fromPlain("");

			final GeoResponse geoResponse = GeoAPI.getCountry(ip);

			if (geoResponse == null)
				return SimpleComponent.fromPlain("");

			else if ("country_code".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getCountryCode());

			else if ("country_name".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getCountryName());

			else if ("region_name".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getRegionName());

			else if ("isp".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getIsp());
		}

		return null;
	}
}
