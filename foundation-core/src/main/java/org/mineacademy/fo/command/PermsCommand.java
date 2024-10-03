package org.mineacademy.fo.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.command.annotation.Permission;
import org.mineacademy.fo.command.annotation.PermissionGroup;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

import lombok.NonNull;

/**
 * A simple predefined command for quickly listing all permissions
 * the plugin uses, given they are stored in a class.
 */
public final class PermsCommand extends SimpleSubCommandCore {

	/**
	 * Classes with permissions listed as fields
	 */
	private final Class<?> classToList;

	/**
	 * Create a new subcommand using the {@link FoundationPlugin#getDefaultCommandGroup()} command group and
	 * "permissions" and "perms" aliases.
	 *
	 * The class must have the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param classToList
	 */
	public PermsCommand(Class<?> classToList) {
		this("permissions|perms", classToList);
	}

	/**
	 * Create a new subcommand using the {@link FoundationPlugin#getDefaultCommandGroup()} command group and label.
	 * The class must have the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param label
	 * @param classToList
	 */
	private PermsCommand(String label, @NonNull Class<?> classToList) {
		super(label);

		this.classToList = classToList;

		this.setProperties();
	}

	/**
	 * Create a new subcommand using the given command group and "permissions" and "perms" aliases. The class must have
	 * the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param group
	 * @param classToList
	 */
	public PermsCommand(SimpleCommandGroup group, Class<?> classToList) {
		this(group, "permissions|perms", classToList);
	}

	/**
	 * Create a new subcommand using the given command group and label. The class must have
	 * the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param group
	 * @param label
	 * @param classToList
	 * @param variableReplacer
	 */
	private PermsCommand(SimpleCommandGroup group, String label, @NonNull Class<?> classToList) {
		super(group, label);

		this.classToList = classToList;

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setDescription(Lang.component("command-perms-description"));
		this.setUsage(Lang.component("command-perms-usage"));
	}

	@Override
	protected void onCommand() {
		final String phrase = this.args.length > 0 ? this.joinArgs(0) : null;

		new ChatPaginator(15)
				.setFoundationHeader(Lang.legacy("command-perms-header"))
				.setPages(this.list(phrase))
				.send(this.audience);
	}

	/*
	 * Iterate through all classes and superclasses in the given classes and fill their permissions
	 * that match the given phrase
	 */
	private List<SimpleComponent> list(String phrase) {
		final List<SimpleComponent> messages = new ArrayList<>();
		Class<?> iteratedClass = this.classToList;

		try {
			do
				this.listIn(iteratedClass, messages, phrase);
			while (!(iteratedClass = iteratedClass.getSuperclass()).isAssignableFrom(Object.class));

		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		return messages;
	}

	/*
	 * Find annotations and compile permissions list from the given class and given existing
	 * permissions that match the given phrase
	 */
	private void listIn(Class<?> clazz, List<SimpleComponent> messages, String phrase) throws ReflectiveOperationException {
		final PermissionGroup group = clazz.getAnnotation(PermissionGroup.class);

		if (!messages.isEmpty() && !clazz.isAnnotationPresent(PermissionGroup.class))
			throw new FoException("Please place @PermissionGroup over " + clazz);

		final List<SimpleComponent> subsectionMessages = new ArrayList<>();

		for (final Field field : clazz.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Permission.class))
				continue;

			final Permission annotation = field.getAnnotation(Permission.class);

			String info = annotation.value();

			if (info.contains("{label}")) {
				final SimpleCommandGroup defaultGroup = Platform.getPlugin().getDefaultCommandGroup();

				ValidCore.checkNotNull(defaultGroup, "Found {label} in @Permission under " + field + " while no default command group is set!");
			}

			info = Variables.replace(info, null);

			final boolean def = annotation.def();

			if (info.contains("{plugin_name}") || info.contains("{plugin}"))
				throw new FoException("Forgotten unsupported variable in " + info + " for field " + field + " in " + clazz);

			final String node = (String) field.get(null);

			if (node.contains("{plugin_name}") || node.contains("{plugin}"))
				throw new FoException("Forgotten unsupported variable in " + info + " for field " + field + " in " + clazz);

			final boolean has = this.audience == null ? false : this.hasPerm(node.replaceAll("\\.\\{.*?\\}", ""));

			if (phrase == null || node.contains(phrase)) {
				subsectionMessages.add(SimpleComponent
						.fromMini("  " + (has ? "&a" : "&7") + node + (def ? " " + Lang.legacy("command-perms-true-by-default") : ""))
						.onClickOpenUrl("")
						.onClickSuggestCmd(node)
						.onHover(Lang.componentArrayVars("command-perms-info",
								"info", info,
								"default", def ? Lang.component("command-perms-yes") : Lang.component("command-perms-no"),
								"state", has ? Lang.component("command-perms-yes") : Lang.component("command-perms-no"))));
			}
		}

		if (!subsectionMessages.isEmpty()) {
			messages.add(SimpleComponent
					.fromMini("&7- ").append(messages.isEmpty() ? Lang.component("command-perms-main") : SimpleComponent.fromPlain(group.value()))
					.onClickOpenUrl(""));

			messages.addAll(subsectionMessages);
		}

		for (final Class<?> inner : clazz.getDeclaredClasses()) {
			messages.add(SimpleComponent.fromMini("&r "));

			this.listIn(inner, messages, phrase);
		}
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}