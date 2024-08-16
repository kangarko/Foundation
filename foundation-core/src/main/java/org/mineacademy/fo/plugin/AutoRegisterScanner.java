package org.mineacademy.fo.plugin;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtilCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommandCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.settings.FileConfig;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.settings.YamlStaticConfig;

import lombok.Setter;

/**
 * Utilizes \@AutoRegister annotation to add auto registration support for commands, events and much more.
 */
final class AutoRegisterScanner {

	/**
	 * Prevents overriding {@link ProxyListener} in case of having multiple
	 */
	private static boolean proxyListenerRegistered = false;

	/**
	 * Automatically register the main command group if there is only one in the code
	 */
	private static final List<SimpleCommandGroup> registeredCommandGroups = new ArrayList<>();

	/**
	 * An extension for platform-specific implementation
	 */
	@Setter
	private static AutoRegisterHandler customRegisterHandler;

	interface AutoRegisterHandler {

		void onPreScan();

		boolean autoRegister(Class<?> clazz, boolean printWarnings, Tuple<FindInstance, Object> tuple);

		boolean canAutoRegister(Class<?> clazz);

		default void enforceModeFor(Class<?> clazz, FindInstance actual, FindInstance required) {
			AutoRegisterScanner.enforceModeFor(clazz, actual, required);
		}
	}

	/**
	 * Scans your plugin and if your Tool or SimpleEnchantment class implements Listener
	 * and has "instance" method to be a singleton, your events are registered there automatically
	 * <p>
	 * If not, we only call the instance constructor in case there is any underlying registration going on
	 */
	public static void scanAndRegister() {

		// Reset
		proxyListenerRegistered = false;
		registeredCommandGroups.clear();

		if (customRegisterHandler != null)
			customRegisterHandler.onPreScan();

		// Find all plugin classes that can be autoregistered
		final List<Class<?>> classes = findValidClasses();

		// Register settings early to be used later
		registerSettings(classes);

		for (final Class<?> clazz : classes)
			try {

				// Prevent beginner programmer mistake of forgetting to implement listener
				try {
					final Class<? extends Annotation> eventHandlerClass = ReflectionUtilCore.lookupClass("org.bukkit.event.EventHandler");
					final Class<?> listenerClass = ReflectionUtilCore.lookupClass("org.bukkit.event.Listener");

					for (final Method method : clazz.getMethods())
						if (method.isAnnotationPresent(eventHandlerClass))
							ValidCore.checkBoolean(listenerClass.isAssignableFrom(clazz), "Detected @EventHandler in " + clazz + ", make this class 'implements Listener' before using events there");

				} catch (final Error err) {
					// Ignore, likely caused by a non-Bukkit platform or missing plugins
				}

				// Handled above
				if (YamlStaticConfig.class.isAssignableFrom(clazz))
					continue;

				// Auto register classes
				final AutoRegister autoRegister = clazz.getAnnotation(AutoRegister.class);

				// Require our annotation to be used
				if (autoRegister != null
						|| ProxyListener.class.isAssignableFrom(clazz)
						|| SimpleExpansion.class.isAssignableFrom(clazz)
						|| (customRegisterHandler != null && customRegisterHandler.canAutoRegister(clazz))) {

					ValidCore.checkBoolean(!SimpleSubCommandCore.class.isAssignableFrom(clazz), "@AutoRegister cannot be used on sub command class: " + clazz + "! Rather write registerSubcommand(Class) in registerSubcommands()"
							+ " method where Class is your own middle-men abstract class extending SimpleSubCommand that all of your subcommands extend.");

					ValidCore.checkBoolean(Modifier.isFinal(clazz.getModifiers()), "Please make " + clazz + " final for it to be registered automatically (or via @AutoRegister)");

					try {
						autoRegister(clazz, autoRegister == null || !autoRegister.hideIncompatibilityWarnings());

					} catch (final NoClassDefFoundError | NoSuchFieldError ex) {
						CommonCore.warning("Failed to auto register " + clazz + " due to it requesting missing fields/classes: " + ex.getMessage());

					} catch (final Throwable t) {
						final String error = CommonCore.getOrEmpty(t.getMessage());

						if (t instanceof NoClassDefFoundError && error.contains("org/bukkit/entity")) {
							CommonCore.warning("**** WARNING ****");

							if (error.contains("DragonFireball"))
								CommonCore.warning("Your Minecraft version does not have DragonFireball class, we suggest replacing it with a Fireball instead in: " + clazz);
							else
								CommonCore.warning("Your Minecraft version does not have " + error + " class you call in: " + clazz);
						} else
							CommonCore.error(t, "Failed to auto register class " + clazz);
					}
				}

			} catch (final Throwable t) {

				// Ignore exception in other class we loaded
				if (t instanceof VerifyError)
					continue;

				CommonCore.error(t, "Failed to scan class '" + clazz + "' using Foundation!");
			}

		// Register command groups later
		registerCommandGroups();
	}

	public static void reloadSettings() {
		// Find all plugin classes that can be autoregistered
		final List<Class<?>> classes = findValidClasses();

		// Reset settings and localization
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		FileConfig.clearLoadedSections();

		// Register settings early to be used later
		registerSettings(classes);

		for (final Class<?> clazz : classes)
			try {
				final Tuple<FindInstance, Object> tuple = findInstance(clazz);

				if (YamlConfig.class.isAssignableFrom(clazz)) {
					final YamlConfig config = (YamlConfig) tuple.getValue();

					if (Platform.getPlugin().isReloading()) {
						config.save();
						config.reload();
					}
				}

			} catch (final Throwable t) {

				// Ignore exception in other class we loaded
				if (t instanceof VerifyError)
					continue;

				CommonCore.error(t, "Failed to reload class '" + clazz + "' using Foundation!");
			}
	}

	/*
	 * Registers settings and localization classes, either automatically if
	 * a class is detected, or forced if settings/localization files are found
	 */
	private static void registerSettings(List<Class<?>> classes) {
		final List<Class<?>> staticSettingsFound = new ArrayList<>();
		final List<Class<?>> staticLocalizations = new ArrayList<>();
		final List<Class<?>> staticCustom = new ArrayList<>();

		for (final Class<?> clazz : classes) {
			boolean load = false;

			if (clazz == SimpleLocalization.class || clazz == SimpleSettings.class || clazz == YamlStaticConfig.class)
				continue;

			if (SimpleSettings.class.isAssignableFrom(clazz)) {
				staticSettingsFound.add(clazz);

				load = true;
			}

			if (SimpleLocalization.class.isAssignableFrom(clazz)) {
				staticLocalizations.add(clazz);

				load = true;
			}

			if (load || !load && YamlStaticConfig.class.isAssignableFrom(clazz))
				staticCustom.add(clazz);
		}

		boolean staticSettingsFileExist = false;
		boolean staticLocalizationFileExist = false;

		try (final JarFile jarFile = new JarFile(Platform.getPlugin().getFile())) {
			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry type = it.nextElement();
				final String name = type.getName();

				if (name.matches("settings\\.yml"))
					staticSettingsFileExist = true;
				else if (name.matches("localization\\/messages\\_(.*)\\.yml"))
					staticLocalizationFileExist = true;
			}
		} catch (final IOException ex) {
		}

		ValidCore.checkBoolean(staticSettingsFound.size() < 2, "Cannot have more than one class extend SimpleSettings: " + staticSettingsFound);
		ValidCore.checkBoolean(staticLocalizations.size() < 2, "Cannot have more than one class extend SimpleLocalization: " + staticLocalizations);

		if (staticSettingsFound.isEmpty() && staticSettingsFileExist)
			YamlStaticConfig.load(SimpleSettings.class);

		// A dirty solution to prioritize loading settings and then localization
		final List<Class<?>> delayedLoading = new ArrayList<>();

		for (final Class<?> customSettings : staticCustom)
			if (SimpleSettings.class.isAssignableFrom(customSettings))
				YamlStaticConfig.load((Class<? extends YamlStaticConfig>) customSettings);
			else
				delayedLoading.add(customSettings);

		for (final Class<?> delayedSettings : delayedLoading)
			YamlStaticConfig.load((Class<? extends YamlStaticConfig>) delayedSettings);

		if (staticLocalizations.isEmpty() && staticLocalizationFileExist)
			YamlStaticConfig.load(SimpleLocalization.class);

	}

	/*
	 * Registers command groups, automatically assuming the main command group from the main command label
	 */
	private static void registerCommandGroups() {
		boolean mainCommandGroupFound = false;

		for (final SimpleCommandGroup group : registeredCommandGroups) {

			// Register if main command or there is only one command group, then assume main
			if (group.getLabel().equals(SimpleSettings.MAIN_COMMAND_ALIASES.first()) || registeredCommandGroups.size() == 1) {
				ValidCore.checkBoolean(!mainCommandGroupFound, "Found 2 or more command groups that do not specify label in their constructor."
						+ " (We can only automatically use one of such groups as the main one using Command_Aliases as command label(s)"
						+ " from settings.yml but not more.");

				Platform.getPlugin().setDefaultCommandGroup(group);
				mainCommandGroupFound = true;
			}

			Platform.getPlugin().registerCommands(group);
		}
	}

	/*
	 * Automatically registers the given class, printing console warnings
	 */
	private static void autoRegister(Class<?> clazz, boolean printWarnings) {

		final FoundationPlugin plugin = Platform.getPlugin();
		final Tuple<FindInstance, Object> tuple = findInstance(clazz);

		final FindInstance mode = tuple.getKey();
		final Object instance = tuple.getValue();

		if (customRegisterHandler != null && customRegisterHandler.autoRegister(clazz, printWarnings, tuple))
			return;

		if (ProxyListener.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, FindInstance.SINGLETON);

			if (!proxyListenerRegistered) {
				proxyListenerRegistered = true;

				plugin.setDefaultProxyListener((ProxyListener) instance);
			}

		} else if (SimpleCommandCore.class.isAssignableFrom(clazz))
			plugin.registerCommand((SimpleCommandCore) instance);

		else if (SimpleCommandGroup.class.isAssignableFrom(clazz)) {
			final SimpleCommandGroup group = (SimpleCommandGroup) instance;

			// Special case, do it at the end
			registeredCommandGroups.add(group);
		}

		else if (SimpleExpansion.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, FindInstance.SINGLETON);

			Variables.addExpansion((SimpleExpansion) instance);
		}

		else if (YamlConfig.class.isAssignableFrom(clazz)) {

			// Automatically called onLoadFinish when getting instance
			enforceModeFor(clazz, mode, FindInstance.SINGLETON);

		} else
			throw new FoException("@AutoRegister cannot be used on " + clazz);
	}

	/*
	 * Compiles valid classes from our plugin that can be autoregistered
	 */
	private static List<Class<?>> findValidClasses() {
		final List<Class<?>> classes = new ArrayList<>();

		// Ignore anonymous inner classes
		final Pattern anonymousClassPattern = Pattern.compile("\\w+\\$[0-9]$");

		try (final JarFile file = new JarFile(Platform.getPlugin().getFile())) {
			for (final Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
				final JarEntry jar = entry.nextElement();
				final String name = jar.getName().replace("/", ".");

				// Ignore files such as settings.yml
				if (!name.endsWith(".class"))
					continue;

				final String className = name.substring(0, name.length() - 6);
				Class<?> clazz = null;

				// Look up the Java class, silently ignore if failing
				try {
					clazz = Platform.getPlugin().getPluginClassLoader().loadClass(className);

				} catch (final ClassFormatError | VerifyError | NoClassDefFoundError | ClassNotFoundException | IncompatibleClassChangeError error) {
					continue;
				}

				// Ignore abstract or anonymous classes
				if (!Modifier.isAbstract(clazz.getModifiers()) && !anonymousClassPattern.matcher(className).find())
					classes.add(clazz);
			}

		} catch (final Throwable t) {
			RemainCore.sneaky(t);
		}

		return classes;
	}

	/*
	 * Tries to return instance of the given class, either by returning its singleon
	 * or creating a new instance from constructor if valid
	 */
	private static Tuple<FindInstance, Object> findInstance(Class<?> clazz) {
		final Constructor<?>[] constructors = clazz.getDeclaredConstructors();

		Object instance = null;
		FindInstance mode = null;

		// Strictly limit the class to one no args constructor
		if (constructors.length == 1) {
			final Constructor<?> constructor = constructors[0];

			if (constructor.getParameterCount() == 0) {
				final int modifiers = constructor.getModifiers();

				// Case 1: Public constructor
				if (Modifier.isPublic(modifiers)) {
					instance = ReflectionUtilCore.instantiate(constructor);
					mode = FindInstance.NEW_FROM_CONSTRUCTOR;
				}

				// Case 2: Singleton
				else if (Modifier.isPrivate(modifiers)) {
					Field instanceField = null;

					for (final Field field : clazz.getDeclaredFields()) {
						final int fieldMods = field.getModifiers();

						if (!field.getType().isAssignableFrom(clazz) || field.getType() == Object.class)
							continue;

						if (Modifier.isPrivate(fieldMods) && Modifier.isStatic(fieldMods) && (Modifier.isFinal(fieldMods) || Modifier.isVolatile(fieldMods)))
							instanceField = field;
					}

					if (instanceField != null) {
						instance = ReflectionUtilCore.getFieldContent(instanceField, (Object) null);
						mode = FindInstance.SINGLETON;
					}
				}
			}

		}

		ValidCore.checkBoolean(!(instance instanceof Boolean), "Used " + mode + " to find instance of " + clazz.getSimpleName() + " but got a boolean instead!");

		ValidCore.checkNotNull(instance, "Your class " + clazz + " using @AutoRegister must EITHER have 1) one public no arguments constructor,"
				+ " OR 2) one private no arguments constructor plus a 'private static final " + clazz.getSimpleName() + " instance' instance field.");

		return new Tuple<>(mode, instance);
	}

	/*
	 * Checks if the way the given class can be made a new instance of, correspond with the required way
	 */
	private static void enforceModeFor(Class<?> clazz, FindInstance actual, FindInstance required) {
		ValidCore.checkBoolean(required == actual, clazz + " using @AutoRegister must have " + (required == FindInstance.NEW_FROM_CONSTRUCTOR ? "a single public no args constructor"
				: "one private no args constructor plus a 'private static final " + clazz.getSimpleName() + " instance' field to be a singleton'"));
	}

	/*
	 * How a new instance can be made to autoregister
	 */
	public enum FindInstance {
		NEW_FROM_CONSTRUCTOR,
		SINGLETON
	}
}
