package org.mineacademy.fo.settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlConfig.CasusHelper;
import org.mineacademy.fo.settings.YamlConfig.TimeHelper;
import org.mineacademy.fo.settings.YamlConfig.TitleHelper;

/**
 * A special case {@link YamlConfig} that allows static access to
 * this config. This is unsafe however this is only to be used
 * in two config instances - the main settings.yml file and localization file,
 * which allow static access from anywhere for convenience.
 *
 * Keep in mind you can only access values during initialization
 * and you must write "private static void init()" method in your
 * class so that we can invoke it automatically!
 *
 * Also keep in mind that all static fields must be set after the class
 * has finished loading!
 */
public abstract class YamlStaticConfig {

	/**
	 * The temporary {@link YamlConfig} instance we store here
	 * to get values from
	 */
	private static YamlConfig TEMPORARY_INSTANCE;

	/**
	 * Internal use only: Create a new {@link YamlConfig} instance and link it to load fields
	 * via reflection.
	 */
	protected YamlStaticConfig() {
		TEMPORARY_INSTANCE = new YamlConfig() {

			@Override
			protected void onLoadFinish() {
				YamlStaticConfig.this.loadViaReflection();
			}
		};
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
	public static final void load(List<Class<? extends YamlStaticConfig>> classes) throws Exception {
		if (classes == null)
			return;

		for (final Class<? extends YamlStaticConfig> clazz : classes) {
			final YamlStaticConfig config = clazz.newInstance();

			config.load();

			TEMPORARY_INSTANCE = null;
		}
	}

	/**
	 * Invoke code before this class is being scanned and invoked using reflection
	 */
	protected void beforeLoad() {
	}

	/**
	 * Called automatically in {@link #load(List)}, you should call the standard load method
	 * from {@link YamlConfig} here
	 *
	 * @throws Exception
	 */
	protected abstract void load() throws Exception;

	/**
	 * Loads the class via reflection, scanning for "private static void init()" methods to run
	 */
	protected final void loadViaReflection() {
		Valid.checkNotNull(TEMPORARY_INSTANCE, "Instance cannot be null " + getFileName());
		Valid.checkNotNull(TEMPORARY_INSTANCE.getConfig(), "Config cannot be null for " + getFileName());
		Valid.checkNotNull(TEMPORARY_INSTANCE.getDefaults(), "Default config cannot be null for " + getFileName());

		try {
			beforeLoad();

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
	private final void invokeAll(Class<?> clazz) throws Exception {
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
	private final void invokeMethodsIn(Class<?> clazz) throws Exception {
		for (final Method m : clazz.getDeclaredMethods()) {
			final int mod = m.getModifiers();

			if (m.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) && Modifier.isStatic(mod) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0,
						"Method '" + m.getName() + "' in " + clazz + " must be 'private static void init()'");

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
	private final void checkFields(Class<?> clazz) throws Exception {
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

	protected final void createLocalizationFile(String localePrefix) throws Exception {
		TEMPORARY_INSTANCE.loadLocalization(localePrefix);
	}

	protected final void createFileAndLoad(String path) throws Exception {
		TEMPORARY_INSTANCE.loadConfiguration(path, path);
	}

	/**
	 * This set method sets the path-value pair and also saves the file
	 *
	 * @param path
	 * @param value
	 */
	protected static final void set(String path, Object value) {
		TEMPORARY_INSTANCE.setNoSave(path, value);
	}

	protected static final boolean isSetAbsolute(String path) {
		return TEMPORARY_INSTANCE.isSetAbsolute(path);
	}

	protected static final boolean isSet(String path) {
		return TEMPORARY_INSTANCE.isSet(path);
	}

	protected static final void move(String fromRelative, String toAbsolute) {
		TEMPORARY_INSTANCE.move(fromRelative, toAbsolute);
	}

	protected static final void move(Object value, String fromPath, String toPath) {
		TEMPORARY_INSTANCE.move(value, fromPath, toPath);
	}

	protected static final void pathPrefix(String pathPrefix) {
		TEMPORARY_INSTANCE.pathPrefix(pathPrefix);
	}

	protected static final String getPathPrefix() {
		return TEMPORARY_INSTANCE.getPathPrefix();
	}

	protected static final void addDefaultIfNotExist(String path) {
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

	protected static final StrictList<Enchantment> getEnchantments(String path) {
		return TEMPORARY_INSTANCE.getEnchants(path);
	}

	protected static final StrictList<Material> getMaterialList(String path) {
		return TEMPORARY_INSTANCE.getMaterialList(path);
	}

	protected static final StrictList<String> getCommandList(String path) {
		return TEMPORARY_INSTANCE.getCommandList(path);
	}

	protected static final List<String> getStringList(String path) {
		return TEMPORARY_INSTANCE.getStringList(path);
	}

	protected static final <E extends Enum<E>> StrictList<E> getEnumList(String path, Class<E> listType) {
		return TEMPORARY_INSTANCE.getEnumList(path, listType);
	}

	protected static final boolean getBoolean(String path) {
		return TEMPORARY_INSTANCE.getBoolean(path);
	}

	protected static final String[] getStringArray(String path) {
		return TEMPORARY_INSTANCE.getStringArray(path);
	}

	protected static final String getString(String path) {
		return TEMPORARY_INSTANCE.getString(path);
	}

	protected static final int getInteger(String path) {
		return TEMPORARY_INSTANCE.getInteger(path);
	}

	protected static final double getDoubleSafe(String path) {
		return TEMPORARY_INSTANCE.getDoubleSafe(path);
	}

	protected static final double getDouble(String path) {
		return TEMPORARY_INSTANCE.getDouble(path);
	}

	protected static final SimpleSound getSound(String path) {
		return TEMPORARY_INSTANCE.getSound(path);
	}

	protected static final CasusHelper getCasus(String path) {
		return TEMPORARY_INSTANCE.getCasus(path);
	}

	protected static final TitleHelper getTitle(String path) {
		return TEMPORARY_INSTANCE.getTitle(path);
	}

	protected static final TimeHelper getTime(String path) {
		return TEMPORARY_INSTANCE.getTime(path);
	}

	protected static final CompMaterial getMaterial(String path) {
		return TEMPORARY_INSTANCE.getMaterial(path);
	}

	protected static final <E extends Enum<E>> E getEnum(String path, Class<E> typeOf) {
		return TEMPORARY_INSTANCE.getEnum(path, typeOf);
	}

	protected static final BoxedMessage getBoxedMessage(String path) {
		return TEMPORARY_INSTANCE.getBoxedMessage(path);
	}

	protected static final Object getGodKnowsWhat(String path) {
		return TEMPORARY_INSTANCE.getObject(path);
	}

	protected static final <Key, Value> LinkedHashMap<Key, Value> getMap(String path, Class<Key> keyType, Value valueType) {
		return TEMPORARY_INSTANCE.getMap(path, keyType, valueType);
	}

	protected static final LinkedHashMap<String, LinkedHashMap<String, Object>> getValuesAndKeys(String path) {
		return TEMPORARY_INSTANCE.getValuesAndKeys(path);
	}
}