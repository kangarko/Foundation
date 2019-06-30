package org.mineacademy.fo.conversation;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * A simple way to communicate with the player
 * - their chat will be isolated and they chat messages processed and
 * the conversation input.
 */
public abstract class SimpleConversation implements ConversationAbandonedListener {

	/**
	 * The menu to return to, if any
	 */
	private Menu menu;

	/**
	 * Creates a simple conversation
	 */
	protected SimpleConversation() {
		this(null);
	}

	/**
	 * Creates a simple conversation that opens the
	 * menu when finished
	 *
	 * @param menu
	 */
	protected SimpleConversation(Menu menu) {
		this.menu = menu;
	}

	/**
	 * Start a conversation with the player, throwing error if {@link Player#isConversing()}
	 *
	 * @param player
	 */
	public final void start(Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing!");

		// Do not allow open inventory since they cannot type anyways
		player.closeInventory();

		// Setup
		final ConversationFactory factory = new ConversationFactory(SimplePlugin.getInstance())
				.withModality(true)
				.withLocalEcho(false)

				.withTimeout(getTimeout())

				.thatExcludesNonPlayersWithMessage(SimpleLocalization.CONVERSATION_REQUIRES_PLAYER)

				.withFirstPrompt(getFirstPrompt())
				.withConversationCanceller(getCanceller())

				.addConversationAbandonedListener(this);

		if (insertPrefix() && getPrefix() != null)
			factory.withPrefix(getPrefix());

		factory.buildConversation(player).begin();
	}

	/**
	 * Get the first prompt in this conversation for the player
	 *
	 * @return
	 */
	protected abstract Prompt getFirstPrompt();

	/**
	 * Listen for and handle existing the conversation
	 */
	@Override
	public final void conversationAbandoned(ConversationAbandonedEvent event) {
		final Conversable conversing = event.getContext().getForWhom();
		final Object source = event.getSource();

		if (source instanceof Conversation) {
			final Conversation convo = (Conversation) source;
			final Prompt currentPrompt = ReflectionUtil.getField(convo, "currentPrompt", Prompt.class);

			if (currentPrompt != null && currentPrompt instanceof SimplePrompt)
				((SimplePrompt) currentPrompt).onConversationEnd(this, event);
		}

		onConversationEnd(event);

		if (conversing instanceof Player) {
			final Player player = (Player) conversing;

			(event.gracefulExit() ? CompSound.SUCCESSFUL_HIT : CompSound.NOTE_BASS).play(player, 1F, 1F);

			if (menu != null && reopenMenu())
				menu.newInstance().displayTo(player);
		}
	}

	/**
	 * Fired when the user quits this conversation (see {@link #getCanceller()}, or
	 * simply quits the game)
	 *
	 * @param event
	 */
	protected void onConversationEnd(ConversationAbandonedEvent event) {
	}

	/**
	 * Get conversation prefix before each message
	 *
	 * By default we use the plugins tell prefix
	 *
	 * TIP: You can use {@link SimplePrefix}
	 *
	 * @return
	 */
	protected SimplePrefix getPrefix() {
		return new SimplePrefix(Common.ADD_TELL_PREFIX && Common.ADD_TELL_PREFIX_IN_CONVERSATION ? Common.getTellPrefix() : "");
	}

	/**
	 * Return the canceller that listens for certain words to exit the convo,
	 * by default we use {@link SimpleCanceller} that listens to quit|cancel|exit
	 *
	 * @return
	 */
	protected ConversationCanceller getCanceller() {
		return new SimpleCanceller("quit", "cancel", "exit");
	}

	/**
	 * Return true if we should insert a prefix before each message, see {@link #getPrefix()}
	 *
	 * @return
	 */
	protected boolean insertPrefix() {
		return true;
	}

	/**
	 * If we detect the player has a menu opened should we reopen it?
	 *
	 * @return
	 */
	protected boolean reopenMenu() {
		return true;
	}

	/**
	 * Get the timeout in seconds before automatically exiting the convo
	 *
	 * @return
	 */
	protected int getTimeout() {
		return 60;
	}

	/**
	 * Sets the menu to return to after the end of this conversation
	 *
	 * @param menu
	 */
	public void setMenuToReturnTo(Menu menu) {
		this.menu = menu;
	}
	// ------------------------------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Sends a boxed message to the conversable player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(int delayTicks, Conversable conversable, String... messages) {
		Common.runLater(delayTicks, () -> tellBoxed(conversable, messages));
	}

	/**
	 * Sends a boxed message to the conversable player
	 *
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(Conversable conversable, String... messages) {
		BoxedMessage.tell((Player) conversable, messages);
	}

	/**
	 * Shortcut method for direct message send to the player
	 *
	 * @param conversable
	 * @param message
	 */
	protected static final void tell(Conversable conversable, String message) {
		Common.tellConversing(conversable, message);
	}

	/**
	 * Send a message to the conversable player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected static final void tellLater(int delayTicks, Conversable conversable, String message) {
		Common.tellLaterConversing(delayTicks, conversable, message);
	}
}