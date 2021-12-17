package org.mineacademy.fo.conversation;

import java.util.concurrent.TimeUnit;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * A simple way to communicate with the player
 * - their chat will be isolated and they chat messages processed and
 * the conversation input.
 */
public abstract class SimpleConversation implements ConversationAbandonedListener {

	/**
	 * The menu to return to, if any
	 */
	private Menu menuToReturnTo;

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
	 * @param menuToReturnTo
	 */
	protected SimpleConversation(final Menu menuToReturnTo) {
		this.menuToReturnTo = menuToReturnTo;
	}

	/**
	 * Start a conversation with the player, throwing error if {@link Player#isConversing()}
	 *
	 * @param player
	 */
	public final void start(final Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing!");

		// Do not allow open inventory since they cannot type anyways
		player.closeInventory();

		// Setup
		final CustomConversation conversation = new CustomConversation(player);
		final CustomCanceller canceller = new CustomCanceller();

		canceller.setConversation(conversation);

		conversation.getCancellers().add(canceller);
		conversation.getCancellers().add(getCanceller());

		conversation.addConversationAbandonedListener(this);

		conversation.begin();
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
	public final void conversationAbandoned(final ConversationAbandonedEvent event) {
		final ConversationContext context = event.getContext();
		final Conversable conversing = context.getForWhom();

		final Object source = event.getSource();
		final boolean timeout = (boolean) context.getAllSessionData().getOrDefault("FLP#TIMEOUT", false);

		// Remove the session data so that they are invisible to other plugnis
		context.getAllSessionData().remove("FLP#TIMEOUT");

		if (source instanceof CustomConversation) {
			final SimplePrompt lastPrompt = ((CustomConversation) source).getLastSimplePrompt();

			if (lastPrompt != null)
				lastPrompt.onConversationEnd(this, event);
		}

		onConversationEnd(event, timeout);

		if (conversing instanceof Player) {
			final Player player = (Player) conversing;

			(event.gracefulExit() ? CompSound.SUCCESSFUL_HIT : CompSound.NOTE_BASS).play(player, 1F, 1F);

			if (menuToReturnTo != null && reopenMenu()) {
				final Menu newMenu = menuToReturnTo.newInstance();

				newMenu.displayTo(player);

				final String title = this.getMenuAnimatedTitle();

				if (title != null)
					Common.runLater(2, () -> newMenu.animateTitle(title));
			}
		}
	}

	/**
	 * Fired when the user quits this conversation (see {@link #getCanceller()}, or
	 * simply quits the game)
	 *
	 *
	 * @param event
	 * @param canceledFromInactivity true if user failed to enter input in the period set in {@link #getTimeout()}
	 */
	protected void onConversationEnd(final ConversationAbandonedEvent event, boolean canceledFromInactivity) {
		this.onConversationEnd(event);
	}

	/**
	 * Fired when the user quits this conversation (see {@link #getCanceller()}, or
	 * simply quits the game)
	 *
	 * @param event
	 */
	protected void onConversationEnd(final ConversationAbandonedEvent event) {
	}

	/**
	 * Get conversation prefix before each message
	 * <p>
	 * By default we use the plugins tell prefix
	 * <p>
	 * TIP: You can use {@link SimplePrefix}
	 *
	 * @return
	 */
	protected ConversationPrefix getPrefix() {
		return new SimplePrefix(Common.ADD_TELL_PREFIX ? addLastSpace(Common.getTellPrefix()) : "");
	}

	/*
	 * Add a space to the prefix if it ends with one
	 */
	private final String addLastSpace(final String prefix) {
		return prefix.endsWith(" ") ? prefix : prefix + " ";
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
	public final void setMenuToReturnTo(final Menu menu) {
		this.menuToReturnTo = menu;
	}

	/**
	 * The message that flashes in the menu title when opening it back to player
	 *
	 * @return the menuAnimatedTitle
	 */
	public String getMenuAnimatedTitle() {
		return null;
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
	protected static final void tellBoxed(final int delayTicks, final Conversable conversable, final String... messages) {
		Common.runLater(delayTicks, () -> tellBoxed(conversable, messages));
	}

	/**
	 * Sends a boxed message to the conversable player
	 *
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(final Conversable conversable, final String... messages) {
		BoxedMessage.tell((Player) conversable, messages);
	}

	/**
	 * Shortcut method for direct message send to the player
	 *
	 * @param conversable
	 * @param message
	 */
	protected static final void tell(final Conversable conversable, final String message) {
		Common.tellConversing(conversable, Variables.replace(message, (Player) conversable));
	}

	/**
	 * Send a message to the conversable player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected static final void tellLater(final int delayTicks, final Conversable conversable, final String message) {
		Common.tellLaterConversing(delayTicks, conversable, Variables.replace(message, (Player) conversable));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private final class CustomCanceller extends InactivityConversationCanceller {

		/**
		 */
		public CustomCanceller() {
			super(SimplePlugin.getInstance(), getTimeout());
		}

		/**
		 * @see org.bukkit.conversations.InactivityConversationCanceller#cancelling(org.bukkit.conversations.Conversation)
		 */
		@Override
		protected void cancelling(Conversation conversation) {
			conversation.getContext().setSessionData("FLP#TIMEOUT", true);
		}
	}

	/**
	 * Custom conversation class used for only showing the question once per 20 seconds interval
	 */
	private final class CustomConversation extends Conversation {

		/**
		 * Holds the information about the last prompt, used to invoke onConversationEnd
		 */
		@Getter(value = AccessLevel.PRIVATE)
		private SimplePrompt lastSimplePrompt;

		private CustomConversation(final Conversable forWhom) {
			super(SimplePlugin.getInstance(), forWhom, SimpleConversation.this.getFirstPrompt());

			localEchoEnabled = false;

			if (insertPrefix() && SimpleConversation.this.getPrefix() != null)
				prefix = SimpleConversation.this.getPrefix();

		}

		@Override
		public void outputNextPrompt() {
			if (currentPrompt == null)
				try {
					abandon(new ConversationAbandonedEvent(this));

				} catch (final Throwable t) {
					tell(context.getForWhom(), (Messenger.ENABLED ? Messenger.getErrorPrefix() : "") + SimpleLocalization.Conversation.CONVERSATION_ERROR);

					t.printStackTrace();
				}

			else {
				// Save the time when we showed the question to the player
				// so that we only show it once per the given threshold
				final String promptClass = currentPrompt.getClass().getSimpleName();
				final String question = currentPrompt.getPromptText(context);

				try {
					final ExpiringMap<String, Void /*dont have expiring set class*/> askedQuestions = (ExpiringMap<String, Void>) context.getAllSessionData().getOrDefault("Asked_" + promptClass, ExpiringMap.builder().expiration(getTimeout(), TimeUnit.SECONDS).build());

					if (!askedQuestions.containsKey(question)) {
						askedQuestions.put(question, null);

						context.setSessionData("Asked_" + promptClass, askedQuestions);
						context.getForWhom().sendRawMessage(prefix.getPrefix(context) + question);
					}
				} catch (final NoSuchMethodError ex) {
					// Unfortunately, old MC version was detected
				}

				// Save last prompt if it is our class
				if (currentPrompt instanceof SimplePrompt)
					lastSimplePrompt = ((SimplePrompt) currentPrompt);

				if (!currentPrompt.blocksForInput(context)) {
					currentPrompt = currentPrompt.acceptInput(context, null);
					outputNextPrompt();
				}
			}
		}
	}
}