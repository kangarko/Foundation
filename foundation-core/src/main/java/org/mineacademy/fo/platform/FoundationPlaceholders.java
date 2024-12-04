package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.GeoAPI;
import org.mineacademy.fo.GeoAPI.GeoResponse;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Expands the functionality of {@link Variables} to include Bukkit-specific variables,
 * and also hooks into PlaceholderAPI.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FoundationPlaceholders extends SimpleExpansion {

	@Getter
	private static final FoundationPlaceholders instance = new FoundationPlaceholders();

	@Override
	protected SimpleComponent onReplace(FoundationPlayer audience, String identifier) {
		if ("server_version".equals(identifier))
			return SimpleComponent.fromPlain(MinecraftVersion.hasVersion() ? MinecraftVersion.getFullVersion() : Platform.getPlatformVersion());

		else if ("player".equals(identifier) || "player_name".equals(identifier))
			return SimpleComponent.fromPlain(audience == null ? "" : audience.getName());

		else if ("player_uuid".equals(identifier))
			return SimpleComponent.fromPlain(audience == null || !audience.isPlayer() ? "" : audience.getUniqueId().toString());

		else if ("player_server".equals(identifier))
			return SimpleComponent.fromPlain(audience == null || !audience.isPlayer() ? "" : audience.getServer().getName());

		else if ("player_ip".equals(identifier))
			return SimpleComponent.fromPlain(audience == null || !audience.isPlayer() ? "" : audience.getAddress().getAddress().toString().split("\\:")[0]);

		else if ("player_is_discord".equals(identifier) || "sender_is_discord".equals(identifier))
			return SimpleComponent.fromPlain(audience != null && audience.isDiscord() ? "true" : "false");

		else if ("player_is_console".equals(identifier) || "sender_is_console".equals(identifier))
			return SimpleComponent.fromPlain(audience != null && audience.isConsole() ? "true" : "false");

		else if ("player_is_player".equals(identifier) || "sender_is_player".equals(identifier))
			return SimpleComponent.fromPlain(audience.isPlayer() ? "true" : "false");

		else if ("player_country_code".equals(identifier) || "player_country_name".equals(identifier) || "player_region_name".equals(identifier) || "player_isp".equals(identifier)) {
			final InetSocketAddress ip = audience == null ? null : audience.getAddress();

			if (ip == null)
				return SimpleComponent.fromPlain("");

			final GeoResponse geoResponse = GeoAPI.getCountry(ip);

			if (geoResponse == null)
				return SimpleComponent.fromPlain("");

			else if ("player_country_code".equals(identifier))
				return SimpleComponent.fromPlain(geoResponse.getCountryCode());

			else if ("player_country_name".equals(identifier))
				return SimpleComponent.fromPlain(geoResponse.getCountryName());

			else if ("player_region_name".equals(identifier))
				return SimpleComponent.fromPlain(geoResponse.getRegionName());

			else if ("player_isp".equals(identifier))
				return SimpleComponent.fromPlain(geoResponse.getIsp());
		}

		else if ("prefix_plugin".equals(identifier))
			return SimpleComponent.fromMini(SimpleSettings.PREFIX);

		else if ("prefix_info".equals(identifier))
			return Messenger.getInfoPrefix();

		else if ("prefix_success".equals(identifier))
			return Messenger.getSuccessPrefix();

		else if ("prefix_warn".equals(identifier))
			return Messenger.getWarnPrefix();

		else if ("prefix_error".equals(identifier))
			return Messenger.getErrorPrefix();

		else if ("prefix_question".equals(identifier))
			return Messenger.getQuestionPrefix();

		else if ("prefix_announce".equals(identifier))
			return Messenger.getAnnouncePrefix();

		else if ("server_name".equals(identifier))
			return Platform.hasCustomServerName() ? SimpleComponent.fromPlain(Platform.getCustomServerName()) : SimpleComponent.empty();

		else if ("date".equals(identifier))
			return SimpleComponent.fromPlain(TimeUtil.getFormattedDate());

		else if ("date_short".equals(identifier))
			return SimpleComponent.fromPlain(TimeUtil.getFormattedDateShort());

		else if ("date_month".equals(identifier))
			return SimpleComponent.fromPlain(TimeUtil.getFormattedDateMonth());

		else if ("chat_line".equals(identifier))
			return SimpleComponent.fromPlain(CommonCore.chatLine());

		else if ("chat_line_smooth".equals(identifier))
			return SimpleComponent.fromSection(CommonCore.chatLineSmooth());

		else if ("label".equals(identifier)) {
			final SimpleCommandGroup defaultGroup = Platform.getPlugin().getDefaultCommandGroup();

			if (defaultGroup != null)
				return SimpleComponent.fromPlain(defaultGroup.getLabel());
		}

		return null;
	}

	@Override
	public int getPriority() {
		return 10;
	}
}