package org.mineacademy.fo.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.FoundationEnchantmentListener;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;

/**
 * Utilizes \@AutoRegister annotation to add auto registration support for commands, events and much more.
 */
final class AutoRegisterScanner {

	/**
	 * Prevent duplicating registering of our {@link EnchantmentPacketListener}
	 */
	private static boolean enchantListenersRegistered = false;

	/**
	 * Scans your plugin and if your {@link Tool} or {@link SimpleEnchantment} class implements {@link Listener}
	 * and has "instance" method to be a singleton, your events are registered there automatically
	 * <p>
	 * If not, we only call the instance constructor in case there is any underlying registration going on
	 */
	public static void scanAndRegister() {

		// Reset
		enchantListenersRegistered = false;

		// Ignore anonymous inner classes
		final Pattern anonymousClassPattern = Pattern.compile("\\w+\\$[0-9]$");

		try (final JarFile file = new JarFile(SimplePlugin.getSource())) {

			for (final Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
				final JarEntry jar = entry.nextElement();
				final String name = jar.getName().replace("/", ".");

				// Ignore files such as settings.yml
				if (!name.endsWith(".class"))
					continue;

				try {
					final String className = name.substring(0, name.length() - 6);

					Class<?> clazz = null;

					// Look up the Java class, silently ignore if failing
					try {
						clazz = SimplePlugin.class.getClassLoader().loadClass(className);

					} catch (final NoClassDefFoundError | ClassNotFoundException | IncompatibleClassChangeError error) {
						continue;
					}

					// Ignore abstract or anonymous classes
					if (!Modifier.isAbstract(clazz.getModifiers()) && !anonymousClassPattern.matcher(className).find()) {

						// Prevent beginner programmer mistake of forgetting to implement listener
						try {
							for (final Method method : clazz.getMethods())
								if (method.isAnnotationPresent(EventHandler.class))
									Valid.checkBoolean(Listener.class.isAssignableFrom(clazz), "Detected @EventHandler in " + clazz + ", make this class 'implements Listener' before using events there");

						} catch (final Error err) {
							// Ignore, likely caused by missing plugins
						}

						// Auto register classes
						final AutoRegister autoRegister = clazz.getAnnotation(AutoRegister.class);

						// Require our annotation to be used, or support legacy classes from Foundation 5
						if (autoRegister != null || Tool.class.isAssignableFrom(clazz) || SimpleEnchantment.class.isAssignableFrom(clazz)) {
							Valid.checkBoolean(Modifier.isFinal(clazz.getModifiers()), "Please make " + clazz + " final for it to be registered automatically (or via @AutoRegister)");

							try {
								scan(clazz, autoRegister == null || !autoRegister.hideIncompatibilityWarnings());

							} catch (final NoClassDefFoundError | NoSuchFieldError ex) {
								Bukkit.getLogger().warning("Failed to auto register " + clazz + " due to it requesting missing fields/classes: " + ex.getMessage());

								// Ignore if no field is present

							} catch (final Throwable t) {
								final String error = Common.getOrEmpty(t.getMessage());

								if (t instanceof NoClassDefFoundError && error.contains("org/bukkit/entity")) {
									Bukkit.getLogger().warning("**** WARNING ****");

									if (error.contains("DragonFireball"))
										Bukkit.getLogger().warning("Your Minecraft version does not have DragonFireball class, we suggest replacing it with a Fireball instead in: " + clazz);
									else
										Bukkit.getLogger().warning("Your Minecraft version does not have " + error + " class you call in: " + clazz);
								} else
									Common.error(t, "Failed to auto register class " + clazz);
							}
						}
					}

				} catch (final Throwable t) {

					// Ignore exception in other class we loaded
					if (t instanceof VerifyError)
						continue;

					Common.error(t, "Failed to scan class '" + name + "' using Foundation!");
				}
			}

		} catch (final Throwable t) {
			Common.error(t, "Failed to scan classes to register - your classes using @AutoRegister will not function!");
		}
	}

	private static void scan(Class<?> clazz, boolean printWarnings) {

		// Special case: Prevent class init error
		if (SimpleEnchantment.class.isAssignableFrom(clazz) && MinecraftVersion.olderThan(V.v1_13)) {
			if (printWarnings) {
				Bukkit.getLogger().warning("**** WARNING ****");
				Bukkit.getLogger().warning("SimpleEnchantment requires Minecraft 1.13.2 or greater. The following class will not be registered: " + clazz.getName()
						+ ". To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
			}

			return;
		}

		if (DiscordListener.class.isAssignableFrom(clazz) && !HookManager.isDiscordSRVLoaded()) {
			if (printWarnings) {
				Bukkit.getLogger().warning("**** WARNING ****");
				Bukkit.getLogger().warning("DiscordListener requires DiscordSRV. The following class will not be registered: " + clazz.getName()
						+ ". To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
			}

			return;
		}

		if (PacketListener.class.isAssignableFrom(clazz) && !HookManager.isProtocolLibLoaded()) {
			if (printWarnings) {
				Bukkit.getLogger().warning("**** WARNING ****");
				Bukkit.getLogger().warning("PacketListener requires ProtocolLib. The following class will not be registered: " + clazz.getName()
						+ ". To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
			}

			return;
		}

		final SimplePlugin plugin = SimplePlugin.getInstance();
		final Tuple<RegisterMode, Object> tuple = findInstance(clazz);

		final RegisterMode mode = tuple.getKey();
		final Object instance = tuple.getValue();

		boolean eventsRegistered = false;

		if (SimpleListener.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);

			plugin.registerEvents((SimpleListener<?>) instance);
			eventsRegistered = true;
		}

		else if (BungeeListener.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);

			plugin.registerBungeeCord((BungeeListener) instance);
			eventsRegistered = true;
		}

		else if (SimpleCommand.class.isAssignableFrom(clazz)) {
			plugin.registerCommand((SimpleCommand) instance);
		}

		else if (SimpleCommandGroup.class.isAssignableFrom(clazz)) {

			final SimpleCommandGroup group = (SimpleCommandGroup) instance;
			final boolean isMainCommand = group.getLabel().equals(SimpleSettings.MAIN_COMMAND_ALIASES.get(0));

			if (isMainCommand)
				SimplePlugin.getInstance().setMainCommand(group);

			plugin.registerCommands(group);
		}

		else if (SimpleExpansion.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);

			Variables.addExpansion((SimpleExpansion) instance);
		}

		else if (YamlConfig.class.isAssignableFrom(clazz)) {

			// Automatically called onLoadFinish when getting instance
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);

			if (SimplePlugin.isReloading()) {
				((YamlConfig) instance).save();
				((YamlConfig) instance).reload();
			}
		}

		else if (PacketListener.class.isAssignableFrom(clazz)) {

			// Automatically registered by means of adding packet adapters
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);

			((PacketListener) instance).onRegister();
		}

		else if (DiscordListener.class.isAssignableFrom(clazz)) {

			// Automatically registered in its constructor
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);
		}

		else if (SimpleEnchantment.class.isAssignableFrom(clazz)) {

			// Automatically registered in its constructor
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);

			if (!enchantListenersRegistered) {
				plugin.registerEvents(FoundationEnchantmentListener.getInstance());

				EnchantmentPacketListener.getInstance().onRegister();
			}

			enchantListenersRegistered = true;
		}

		else if (Tool.class.isAssignableFrom(clazz)) {

			// Automatically registered in its constructor
			enforceModeFor(clazz, mode, RegisterMode.SINGLETON);
		}

		else if (instance instanceof Listener) {
			// Pass-through to register events later
		}

		else
			throw new FoException("@AutoRegister cannot be used on " + clazz);

		// Register events if needed
		if (!eventsRegistered && instance instanceof Listener)
			plugin.registerEvents((Listener) instance);

		Debugger.debug("auto-register", "Automatically registered " + clazz);
	}

	private static Tuple<RegisterMode, Object> findInstance(Class<?> clazz) {
		final Constructor<?>[] constructors = clazz.getDeclaredConstructors();

		Object instance = null;
		RegisterMode mode = null;

		// Strictly limit the class to one no args constructor
		if (constructors.length == 1) {
			final Constructor<?> constructor = constructors[0];

			if (constructor.getParameterCount() == 0) {
				final int modifiers = constructor.getModifiers();

				// Case 1: Public constructor
				if (Modifier.isPublic(modifiers)) {
					instance = ReflectionUtil.instantiate(constructor);
					mode = RegisterMode.NO_ARGS_CONSTRUCTOR;
				}

				// Case 2: Singleton
				else if (Modifier.isPrivate(modifiers)) {
					Field instanceField = null;

					for (final Field field : clazz.getDeclaredFields()) {
						final int fieldMods = field.getModifiers();

						if (Modifier.isPrivate(fieldMods) && Modifier.isStatic(fieldMods) && (Modifier.isFinal(fieldMods) || Modifier.isVolatile(fieldMods)))
							instanceField = field;
					}

					if (instanceField != null) {
						instance = ReflectionUtil.getFieldContent(instanceField, (Object) null);
						mode = RegisterMode.SINGLETON;
					}
				}
			}

		}

		Valid.checkNotNull(instance, "Your class " + clazz + " using @AutoRegister must EITHER have 1) one public no arguments constructor,"
				+ " OR 2) one private no arguments constructor plus a 'private static final " + clazz.getSimpleName() + " instance' instance field.");

		return new Tuple<>(mode, instance);
	}

	private static void enforceModeFor(Class<?> clazz, RegisterMode actual, RegisterMode required) {
		Valid.checkBoolean(required == actual, clazz + " using @AutoRegister must have " + (required == RegisterMode.NO_ARGS_CONSTRUCTOR ? "a single public no args constructor"
				: "one private no args constructor plus a 'private static final " + clazz.getSimpleName() + " instance' field to be a singleton'"));
	}

	enum RegisterMode {
		NO_ARGS_CONSTRUCTOR,
		SINGLETON
	}
}
