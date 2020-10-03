package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.PermsCommand;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A draft API for enumerating chat messages into pages.
 *
 * See {@link PermsCommand} for an early implementation.
 */
@Getter
@RequiredArgsConstructor
public final class ChatPages {

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
	 * is used and there is no footer. We use {@link #FOUNDATION_HEIGHT} for height.
	 */
	public ChatPages() {
		this(FOUNDATION_HEIGHT);
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
	public ChatPages setFoundationHeader(String title) {
		return this.setHeader("&8" + Common.chatLine(),
				"<center>&6" + title,
				"&8" + Common.chatLine());
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPages setHeader(SimpleComponent... components) {
		for (final SimpleComponent component : components)
			this.header.add(component);

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPages setHeader(String... messages) {
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
	public ChatPages setPages(SimpleComponent... components) {
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
	public ChatPages setPages(String... messages) {
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
	public ChatPages setPages(Collection<SimpleComponent> components) {
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
	public ChatPages setFooter(SimpleComponent... components) {
		for (final SimpleComponent component : components)
			this.footer.add(component);

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPages setFooter(String... messages) {
		for (final String message : messages)
			this.footer.add(SimpleComponent.of(message));

		return this;
	}

	/**
	 * Show this page to the sender, either paginated or a full dumb when this is a console
	 *
	 * @param sender
	 */
	public void send(CommandSender sender) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;

			player.setMetadata("FoPages", new FixedMetadataValue(SimplePlugin.getInstance(), this));
			player.chat("/#flp 0");
		}

		else {
			for (final SimpleComponent component : this.header)
				component.send(sender);

			for (final List<SimpleComponent> components : this.pages.values())
				for (final SimpleComponent component : components)
					component.send(sender);

			for (final SimpleComponent component : this.footer)
				component.send(sender);
		}
	}
}
