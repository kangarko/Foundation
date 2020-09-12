package org.mineacademy.fo.settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlConfig.CasusHelper;
import org.mineacademy.fo.settings.YamlConfig.TitleHelper;

/**
 * A special case {@link YamlConfig} that allows static access to this config. This is unsafe
 * however this is only to be used in two config instances - the main settings.yml file and
 * localization file, which allow static access from anywhere for convenience.
 * <p>
 * Keep in mind you can only access values during initialization and you must write "private static
 * void init()" method in your class so that we can invoke it automatically!
 * <p>
 * Also keep in mind that all static fields must be set after the class has finished loading!
 */
public abstract class YamlStaticConfig {

	/**
	 * The temporary {@link YamlConfig} instance we store here to get values from
	 */
	private static YamlConfig TEMPORARY_INSTANCE;

	/**
	 * Internal use only: Create a new {@link YamlConfig} instance and link it to load fields via
	 * reflection.
	 */
	protected YamlStaticConfig() {
		TEMPORARY_INSTANCE = new YamlConfig() {

			{
				beforeLoad();
			}

			@Override
			protected boolean saveComments() {
				return YamlStaticConfig.this.saveComments();
			}

			@Override
			protected List<String> getUncommentedSections() {
				return YamlStaticConfig.this.getUncommentedSections();
			}

			@Override
			protected void onLoadFinish() {
				loadViaReflection();
			}
		};

		TEMPORARY_INSTANCE.setHeader(getHeader());
	}

	// -----------------------------------------------------------------------------------------------------
	// Main
	// -----------------------------------------------------------------------------------------------------

	/**
	 * Load all given static config classes
	 *
	 * @param classes
	 * @throws Exception
	 */
	public static final void load(final List<Class<? extends YamlStaticConfig>> classes) throws Exception {
		if (classes == null)
			return;

		for (final Class<? extends YamlStaticConfig> clazz : classes) {
			final YamlStaticConfig config = clazz.newInstance();

			config.load();

			TEMPORARY_INSTANCE = null;
		}
	}

	/**
	 * Return the default header used when the file is being written to and saved. YAML files do not
	 * remember # comments. All of them will be lost and the file will be "crunched" together when you
	 * save it, with the only exception being the header. Use the header to display important
	 * information such as where your users can find the documented version of your file (such as on
	 * GitHub).
	 * <p>
	 * Set to null to disable, defaults to {@link FoConstants.Header#UPDATED_FILE}
	 *
	 * @return the header
	 */
	protected String[] getHeader() {
		return FoConstants.Header.UPDATED_FILE;
	}

	/**
	 * @see YamlConfig#saveComments()
	 */
	protected boolean saveComments() {
		return false;
	}

	/**
	 * @see YamlConfig#getUncommentedSections()
	 */
	protected List<String> getUncommentedSections() {
		return null;
	}

	/**
	 * Call this method if you need to make and changes to the settings file BEFORE it is actually
	 * loaded.
	 */
	protected void beforeLoad() {
	}

	/**
	 * Invoke code before this class is being scanned and invoked using reflection
	 * <p>
	 * This method if called AFTER we load our file
	 */
	protected void preLoad() {
	}

	/**
	 * Called automatically in {@link #load(List)}, you should call the standard load method from
	 * {@link YamlConfig} here
	 *
	 * @throws Exception
	 */
	protected abstract void load() throws Exception;

	/**
	 * Loads the class via reflection, scanning for "private static void init()" methods to run
	 */
	private final void loadViaReflection() {
		Valid.checkNotNull(TEMPORARY_INSTANCE, "Instance cannot be null " + getFileName());
		Valid.checkNotNull(TEMPORARY_INSTANCE.getConfig(), "Config cannot be null for " + getFileName());
		Valid.checkNotNull(TEMPORARY_INSTANCE.getDefaults(), "Default config cannot be null for " + getFileName());

		try {
			preLoad();

			// Parent class if applicable.
			if (YamlStaticConfig.class.isAssignableFrom(getClass().getSuperclass())) {
				final Class<?> superClass = getClass().getSuperclass();

				invokeAll(superClass);
			}

			// The class itself.
			invokeAll(getClass());

		} catch (Throwable t) {
			if (t instanceof InvocationTargetException && t.getCause() != null)
				t = t.getCause();

			Remain.sneaky(t);
		}
	}

	/**
	 * Invoke all "private static void init()" methods in the class and its subclasses
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private void invokeAll(final Class<?> clazz) throws Exception {
		invokeMethodsIn(clazz);

		// All sub-classes in superclass.
		for (final Class<?> subClazz : clazz.getDeclaredClasses()) {
			invokeMethodsIn(subClazz);

			// And classes in sub-classes in superclass.
			for (final Class<?> subSubClazz : subClazz.getDeclaredClasses())
				invokeMethodsIn(subSubClazz);
		}
	}

	/**
	 * Invoke all "private static void init()" methods in the class
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private void invokeMethodsIn(final Class<?> clazz) throws Exception {
		for (final Method m : clazz.getDeclaredMethods()) {

			if (!SimplePlugin.getInstance().isEnabled())
				return;

			final int mod = m.getModifiers();

			if (m.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) && Modifier.isStatic(mod) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0, "Method '" + m.getName() + "' in " + clazz + " must be 'private static void init()'");

				m.setAccessible(true);
				m.invoke(null);
			}
		}

		checkFields(clazz);
	}

	/**
	 * Safety check whether all fields have been set
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private void checkFields(final Class<?> clazz) throws Exception {
		for (final Field f : clazz.getDeclaredFields()) {
			f.setAccessible(true);

			if (Modifier.isPublic(f.getModifiers()))
				Valid.checkBoolean(!f.getType().isPrimitive(), "Field '" + f.getName() + "' in " + clazz + " must not be primitive!");

			Object result = null;
			try {
				result = f.get(null);
			} catch (final NullPointerException ex) {
			}
			Valid.checkNotNull(result, "Null " + f.getType().getSimpleName() + " field '" + f.getName() + "' in " + clazz);
		}
	}

	// -----------------------------------------------------------------------------------------------------
	// Delegate methods
	// -----------------------------------------------------------------------------------------------------

	protected final void createLocalizationFile(final String localePrefix) throws Exception {
		TEMPORARY_INSTANCE.loadLocalization(localePrefix);
	}

	protected final void createFileAndLoad(final String path) throws Exception {
		TEMPORARY_INSTANCE.loadConfiguration(path, path);
	}

	/**
	 * This set method sets the path-value pair and also saves the file
	 * <p>
	 * Pathprefix is added
	 *
	 * @param path
	 * @param value
	 */
	protected static final void set(final String path, final Object value) {
		TEMPORARY_INSTANCE.setNoSave(path, value);
	}

	protected static final boolean isSetAbsolute(final String path) {
		return TEMPORARY_INSTANCE.isSetAbsolute(path);
	}

	protected static final boolean isSet(final String path) {
		return TEMPORARY_INSTANCE.isSet(path);
	}

	protected static final boolean isSetDefault(final String path) {
		return TEMPORARY_INSTANCE.isSetDefault(path);
	}

	protected static final boolean isSetDefaultAbsolute(final String path) {
		return TEMPORARY_INSTANCE.isSetDefaultAbsolute(path);
	}

	protected static final void move(final String fromRelative, final String toAbsolute) {
		TEMPORARY_INSTANCE.move(fromRelative, toAbsolute);
	}

	protected static final void move(final Object value, final String fromPath, final String toPath) {
		TEMPORARY_INSTANCE.move(value, fromPath, toPath);
	}

	protected static final String formPathPrefix(final String path) {
		return TEMPORARY_INSTANCE.formPathPrefix(path);
	}

	protected static final void pathPrefix(final String pathPrefix) {
		TEMPORARY_INSTANCE.pathPrefix(pathPrefix);
	}

	protected static final String getPathPrefix() {
		return TEMPORARY_INSTANCE.getPathPrefix();
	}

	protected static final void addDefaultIfNotExist(final String path) {
		TEMPORARY_INSTANCE.addDefaultIfNotExist(path);
	}

	protected static final String getFileName() {
		return TEMPORARY_INSTANCE.getFileName();
	}

	protected static final YamlConfiguration getConfig() {
		return TEMPORARY_INSTANCE.getConfig();
	}

	protected static final YamlConfiguration getDefaults() {
		return TEMPORARY_INSTANCE.getDefaults();
	}

	// -----------------------------------------------------------------------------------------------------
	// Config manipulators
	// -----------------------------------------------------------------------------------------------------

	protected static final StrictList<Enchantment> getEnchantments(final String path) {
		return TEMPORARY_INSTANCE.getEnchants(path);
	}

	protected static final StrictList<CompMaterial> getMaterialList(final String path) {
		return TEMPORARY_INSTANCE.getMaterialList(path);
	}

	protected static final StrictList<String> getCommandList(final String path) {
		return TEMPORARY_INSTANCE.getCommandList(path);
	}

	protected static final List<String> getStringList(final String path) {
		return TEMPORARY_INSTANCE.getStringList(path);
	}

	protected static final <E> Set<E> getSet(final String path, Class<E> typeOf) {
		return TEMPORARY_INSTANCE.getSet(path, typeOf);
	}

	protected static final <E> List<E> getList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getList(path, listType);
	}

	protected static final <E extends Enum<E>> List<E> getCompatibleEnumList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getCompatibleEnumList(path, listType);
	}

	protected static final boolean getBoolean(final String path) {
		return TEMPORARY_INSTANCE.getBoolean(path);
	}

	protected static final String[] getStringArray(final String path) {
		return TEMPORARY_INSTANCE.getStringArray(path);
	}

	protected static final String getString(final String path) {
		return TEMPORARY_INSTANCE.getString(path);
	}

	protected static final Replacer getReplacer(final String path) {
		return TEMPORARY_INSTANCE.getReplacer(path);
	}

	protected static final int getInteger(final String path) {
		return TEMPORARY_INSTANCE.getInteger(path);
	}

	/**
	 * @deprecated use {@link #getDouble(String)}
	 *
	 * @param path
	 * @return
	 */
	@Deprecated
	protected static final double getDoubleSafe(final String path) {
		return TEMPORARY_INSTANCE.getDoubleSafe(path);
	}

	protected static final double getDouble(final String path) {
		return TEMPORARY_INSTANCE.getDouble(path);
	}

	protected static final SimpleSound getSound(final String path) {
		return TEMPORARY_INSTANCE.getSound(path);
	}

	protected static final CasusHelper getCasus(final String path) {
		return TEMPORARY_INSTANCE.getCasus(path);
	}

	protected static final TitleHelper getTitle(final String path) {
		return TEMPORARY_INSTANCE.getTitle(path);
	}

	protected static final SimpleTime getTime(final String path) {
		return TEMPORARY_INSTANCE.getTime(path);
	}

	protected static final CompMaterial getMaterial(final String path) {
		return TEMPORARY_INSTANCE.getMaterial(path);
	}

	protected static final BoxedMessage getBoxedMessage(final String path) {
		return TEMPORARY_INSTANCE.getBoxedMessage(path);
	}

	protected static final <E> E get(final String path, final Class<E> typeOf) {
		return TEMPORARY_INSTANCE.get(path, typeOf);
	}

	protected static final <E> E getWithData(final String path, final Class<E> typeOf, Object... deserializeArguments) {
		return TEMPORARY_INSTANCE.getWithData(path, typeOf, deserializeArguments);
	}

	protected static final Object getObject(final String path) {
		return TEMPORARY_INSTANCE.getObject(path);
	}

	protected static final <T> T getOrSetDefault(final String path, final T defaultValue) {
		return TEMPORARY_INSTANCE.getOrSetDefault(path, defaultValue);
	}

	protected static final SerializedMap getMap(final String path) {
		return TEMPORARY_INSTANCE.getMap(path);
	}

	protected static final <Key, Value> LinkedHashMap<Key, Value> getMap(final String path, final Class<Key> keyType, final Class<Value> valueType) {
		return TEMPORARY_INSTANCE.getMap(path, keyType, valueType);
	}

	protected static LinkedHashMap<String, LinkedHashMap<String, Object>> getValuesAndKeys(final String path) {
		Valid.checkNotNull(path, "Path cannot be null");

		// add default
		if (getDefaults() != null && !getConfig().isSet(path)) {
			Valid.checkBoolean(getDefaults().isSet(path), "Default '" + getFileName() + "' lacks a section at " + path);

			for (final String name : getDefaults().getConfigurationSection(path).getKeys(false))
				for (final String setting : getDefaults().getConfigurationSection(path + "." + name).getKeys(false))
					TEMPORARY_INSTANCE.addDefaultIfNotExist(path + "." + name + "." + setting, Object.class);
		}

		Valid.checkBoolean(getConfig().isSet(path), "Malfunction copying default section to " + path);

		// key, values assigned to the key
		final TreeMap<String, LinkedHashMap<String, Object>> groups = new TreeMap<>();

		final String old = TEMPORARY_INSTANCE.getPathPrefix();
		TEMPORARY_INSTANCE.pathPrefix(null);
		for (final String name : getConfig().getConfigurationSection(path).getKeys(false)) {
			// type, value (UNPARSED)

			final LinkedHashMap<String, Object> valuesRaw = getMap(path + "." + name, String.class, Object.class);

			groups.put(name, valuesRaw);
		}

		TEMPORARY_INSTANCE.pathPrefix(old);

		return new LinkedHashMap<>(groups);
	}
}