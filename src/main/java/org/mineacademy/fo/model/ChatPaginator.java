package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A draft API for enumerating chat messages into pages.
 *
 * See {@link PermsCommand} for an early implementation.
 */
@Getter
@RequiredArgsConstructor
public final class ChatPaginator {

	/**
	 * This is the height that will fill all chat lines (20)
	 * if you use {@link #setFoundationHeader(String)}.
	 *
	 * It is 17 because our header is 3 lines wide.
	 */
	public static final int FOUNDATION_HEIGHT = 15;

	/**
	 * How many lines per page? Maximum on screen is 20 minus header and footer.
	 */
	private final int linesPerPage;

	/**
	 * The color used in header and footer
	 */
	private final ChatColor themeColor;

	/**
	 * The header included on every page.
	 */
	private final List<SimpleComponent> header = new ArrayList<>();

	/**
	 * The pages with their content.
	 */
	private final Map<Integer, List<SimpleComponent>> pages = new HashMap<>();

	/**
	 * The footer included on every page.
	 */
	private final List<SimpleComponent> footer = new ArrayList<>();

	/**
	 * Construct chat pages taking the entire visible
	 * chat portion when chat is maximize given {@link #setFoundationHeader(String)}
	 * is used and there is no footer. We use {@link #FOUNDATION_HEIGHT} for height
	 * and {@link org.mineacademy.fo.settings.SimpleLocalization.Commands#HEADER_COLOR} for color.
	 */
	public ChatPaginator() {
		this(FOUNDATION_HEIGHT, SimpleLocalization.Commands.HEADER_COLOR);
	}

	/**
	 * Construct chat pages taking the entire visible
	 * chat portion when chat is maximize given {@link #setFoundationHeader(String)}
	 * is used and there is no footer. We use {@link #FOUNDATION_HEIGHT} for height.
	 *
	 * @param themeColor
	 */
	public ChatPaginator(ChatColor themeColor) {
		this(FOUNDATION_HEIGHT, themeColor);
	}

	/**
	 * Creates a paginator with the given lines per page. Maximum on screen is 20 minus header and footer.
	 * The {@link org.mineacademy.fo.settings.SimpleLocalization.Commands#HEADER_COLOR} color is used.
	 *
	 * @param linesPerPage
	 */
	public ChatPaginator(int linesPerPage) {
		this(linesPerPage, SimpleLocalization.Commands.HEADER_COLOR);
	}

	/**
	 * Sets the standard Foundation header used across plugins.
	 * ----------------
	 * \<center\>title
	 * ---------------
	 *
	 * @param title
	 * @return
	 */
	public ChatPaginator setFoundationHeader(String title) {
		return this.setHeader("&r", this.themeColor + "&m" + ChatUtil.center("&r" + this.themeColor + " " + title + " &m", '-', 150), "&r");
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setHeader(SimpleComponent... components) {
		Collections.addAll(this.header, components);

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setHeader(String... messages) {
		for (final String message : messages)
			this.header.add(SimpleComponent.of(message));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(SimpleComponent... components) {
		this.pages.clear();
		this.pages.putAll(Common.fillPages(this.linesPerPage, Arrays.asList(components)));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setPages(String... messages) {
		final List<SimpleComponent> pages = new ArrayList<>();

		for (final String message : messages)
			pages.add(SimpleComponent.of(message));

		return this.setPages(pages);
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(Collection<SimpleComponent> components) {
		this.pages.clear();
		this.pages.putAll(Common.fillPages(this.linesPerPage, components));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setFooter(SimpleComponent... components) {
		Collections.addAll(this.footer, components);

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setFooter(String... messages) {
		for (final String message : messages)
			this.footer.add(SimpleComponent.of(message));

		return this;
	}

	/**
	 * Start showing the first page to the sender
	 *
	 * @param sender
	 */
	public void send(CommandSender sender) {
		this.send(sender, 1);
	}

	/**
	 * Show the given page to the sender, either paginated or a full dumb when this is a console
	 *
	 * @param sender
	 * @param page
	 */
	public void send(CommandSender sender, int page) {
		if (Bukkit.isPrimaryThread())
			this.send0(sender, page);
		else
			Common.runLater(() -> this.send0(sender, page));
	}

	private void send0(CommandSender sender, int page) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;

			// Remove old FoPages to prevent conflicts when two or more plugins use Foundation shaded
			if (player.hasMetadata("FoPages")) {
				final Plugin owningPlugin = player.getMetadata("FoPages").get(0).getOwningPlugin();

				player.removeMetadata("FoPages", owningPlugin);
			}

			player.setMetadata("FoPages", new FixedMetadataValue(SimplePlugin.getInstance(), SimplePlugin.getNamed()));
			player.setMetadata(getPageNbtTag(), new FixedMetadataValue(SimplePlugin.getInstance(), this));

			player.chat("/#flp " + page);
		}

		else {
			for (final SimpleComponent component : this.header)
				component.send(sender);

			int amount = 1;

			for (final List<SimpleComponent> components : this.pages.values())
				for (final SimpleComponent component : components)
					component.replace("{count}", amount++).send(sender);

			for (final SimpleComponent component : this.footer)
				component.send(sender);
		}
	}

	public static String getPageNbtTag() {
		return "FoPages_" + SimplePlugin.getNamed();
	}
}
