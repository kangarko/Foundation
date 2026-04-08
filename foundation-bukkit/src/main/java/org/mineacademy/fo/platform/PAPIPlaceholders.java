package org.mineacademy.fo.platform;

import org.bukkit.OfflinePlayer;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.model.Variables;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Expands the functionality of {@link Variables} to include PlaceholderAPI-specific variables.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PAPIPlaceholders extends SimpleExpansion {

	@Getter
	private final static PAPIPlaceholders instance = new PAPIPlaceholders();

	@Override
	protected String onReplace(final FoundationPlayer audience, final String identifier) {
		OfflinePlayer player = null;

		if (audience != null)
			if (audience.isPlayer())
				player = audience.getPlayer();

			else if (audience.isDiscord())
				player = ((DiscordSender) audience.getSender()).getOfflinePlayer();

		return HookManager.getPlaceholderAPIValue(player, identifier);
	}

	@Override
	public int getPriority() {
		return 7;
	}
}
