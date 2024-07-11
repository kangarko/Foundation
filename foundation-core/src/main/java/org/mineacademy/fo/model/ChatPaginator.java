package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.command.PermsCommand;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

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
	 * The header included on every page.
	 */
	private final List<SimpleComponent> header = new ArrayList<>();

	/**
	 * The pages with their content.
	 */
	private final Map<Integer, List<SimpleComponent>> pages = new LinkedHashMap<>();

	/**
	 * The footer included on every page.
	 */
	private final List<SimpleComponent> footer = new ArrayList<>();

	/**
	 * Construct chat pagination taking the entire visible chat portion when chat is maximized.
	 */
	public ChatPaginator() {
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
	public ChatPaginator setFoundationHeader(String title) {
		return this.setHeader("&8&m" + ChatUtil.center("&r " + title + " &8&m", Lang.legacy("command-header-center-letter").charAt(0), Integer.parseInt(Lang.legacy("command-header-center-padding"))));
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
			this.header.add(SimpleComponent.fromMini(message));

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
			pages.add(SimpleComponent.fromMini(message));

		return this.setPages(pages.toArray(new SimpleComponent[pages.size()]));
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(SimpleComponent... components) {
		this.pages.clear();
		this.pages.putAll(CommonCore.fillPages(this.linesPerPage, Arrays.asList(components)));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(List<SimpleComponent> components) {
		this.pages.clear();
		this.pages.putAll(CommonCore.fillPages(this.linesPerPage, components));

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
			this.footer.add(SimpleComponent.fromMini(message));

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
	 * Start showing the first page to the sender
	 *
	 * @param audience
	 */
	public void send(FoundationPlayer audience) {
		this.send(audience, 1);
	}

	/**
	 * Show the given page to the sender, either paginated or a full dumb when this is a console
	 *
	 * @param audience
	 * @param page
	 */
	public void send(FoundationPlayer audience, int page) {
		if (audience.isPlayer()) {
			audience.setTempMetadata(Platform.getPlugin().getName() + "_Pages", this);
			audience.dispatchCommand("/#flp " + page);

		} else {
			for (final SimpleComponent component : this.header)
				audience.sendMessage(component);

			int amount = 1;

			for (final List<? extends SimpleComponent> components : this.pages.values())
				for (final SimpleComponent component : components)
					audience.sendMessage(component.replaceBracket("count", String.valueOf(amount++)));

			for (final SimpleComponent component : this.footer)
				audience.sendMessage(component);
		}
	}
}
