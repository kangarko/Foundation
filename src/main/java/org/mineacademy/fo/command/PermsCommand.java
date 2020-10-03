package org.mineacademy.fo.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.command.annotation.Permission;
import org.mineacademy.fo.command.annotation.PermissionGroup;
import org.mineacademy.fo.constants.FoPermissions;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ChatPages;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.NonNull;

/**
 * A simple predefined command for quickly listing all permissions
 * the plugin uses, given they are stored in a {@link FoPermissions} class.
 */
public final class PermsCommand extends SimpleSubCommand {

	private final Class<? extends FoPermissions> classToList;
	private final SerializedMap variables;

	public PermsCommand(@NonNull Class<? extends FoPermissions> classToList) {
		this(classToList, new SerializedMap());
	}

	public PermsCommand(@NonNull Class<? extends FoPermissions> classToList, SerializedMap variables) {
		super("permissions|perms");

		this.classToList = classToList;
		this.variables = variables;

		setDescription("List all permissions the plugin has.");

		// Invoke to check for errors early
		list();
	}

	@Override
	protected void onCommand() {

		new ChatPages(15)
				.setFoundationHeader("Listing All " + SimplePlugin.getNamed() + " Permissions")
				.setPages(list())
				.send(sender);
	}

	private List<SimpleComponent> list() {
		final List<SimpleComponent> messages = new ArrayList<>();
		Class<?> iteratedClass = classToList;

		try {
			do {
				listIn(iteratedClass, messages);

			} while (!(iteratedClass = iteratedClass.getSuperclass()).isAssignableFrom(Object.class));

		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		return messages;
	}

	private void listIn(Class<?> clazz, List<SimpleComponent> messages) throws ReflectiveOperationException {

		if (!clazz.isAssignableFrom(FoPermissions.class)) {
			final PermissionGroup group = clazz.getAnnotation(PermissionGroup.class);

			if (!messages.isEmpty() && !clazz.isAnnotationPresent(PermissionGroup.class))
				throw new FoException("Please place @PermissionGroup over " + clazz);

			messages.add(SimpleComponent.of("&7- " + (messages.isEmpty() ? "Main" : group.value()) + " Permissions:"));
		}

		for (final Field field : clazz.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Permission.class))
				continue;

			final Permission annotation = field.getAnnotation(Permission.class);

			final String info = Replacer.replaceVariables(annotation.value(), variables);
			final boolean def = annotation.def();

			if (info.contains("{") && info.contains("}"))
				throw new FoException("Forgotten unreplaced variable in " + info + " for field " + field + " in " + clazz);

			final String node = Replacer.replaceVariables((String) field.get(null), variables);
			final boolean has = sender == null ? false : hasPerm(node);

			messages.add(SimpleComponent
					.of("  " + (has ? "&a" : "&7") + node + (def ? " &7[true by default]" : ""))
					.onHover("&7Info: &f" + info,
							"&7Default? " + (def ? "&2yes" : "&cno"),
							"&7Do you have it? " + (has ? "&2yes" : "&cno")));
		}

		for (final Class<?> inner : clazz.getDeclaredClasses()) {
			messages.add(SimpleComponent.of("&r "));

			listIn(inner, messages);
		}
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}