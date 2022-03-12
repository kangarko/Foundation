package org.mineacademy.fo.settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.FileConfig.AccusativeHelper;
import org.mineacademy.fo.settings.FileConfig.TitleHelper;

/**
 * A special case {@link YamlConfig} that allows static access to config.
 * <p>
 * You can only load or set values during initialization. Write "private static void init()"
 * methods in your class (and inner classes), we will invoke it automatically!
 * <p>
 * You cannot set values after the class has been loaded!
 */
public abstract class YamlStaticConfig {

	/**
	 * Represents "null" which you can use as convenience shortcut in loading config
	 * that has no internal from path.
	 */
	public static final String NO_DEFAULT = null;

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
			protected void onLoad() {
				loadViaReflection();
			}
		};
	}

	// -----------------------------------------------------------------------------------------------------
	// Main
	// -----------------------------------------------------------------------------------------------------

	/**
	 * Load the given static config class
	 *
	 * @param clazz
	 */
	public static final void load(Class<? extends YamlStaticConfig> clazz) {
		try {
			final YamlStaticConfig config = clazz.newInstance();

			config.onLoad();

			TEMPORARY_INSTANCE = null;

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load static settings " + clazz);
		}
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
	protected abstract void onLoad() throws Exception;

	/**
	 * Return true if you have a default file and want to save comments from it
	 *
	 * Any user-generated comments will be lost, any user-written values will be lost.
	 *
	 * Please see {@link #getUncommentedSections()} to write sections containing maps users
	 * can create to prevent losing them.
	 *
	 * @return
	 */
	protected boolean saveComments() {
		return true;
	}

	/**
	 * See {@link #saveComments()}
	 *
	 * @return
	 */
	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	/*
	 * Loads the class via reflection, scanning for "private static void init()" methods to run
	 */
	private void loadViaReflection() {
		Valid.checkNotNull(TEMPORARY_INSTANCE, "Instance cannot be null " + getFileName());
		Valid.checkNotNull(TEMPORARY_INSTANCE.defaults, "Default config cannot be null for " + getFileName());

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

	/*
	 * Invoke all "private static void init()" methods in the class and its subclasses
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

	/*
	 * Invoke all "private static void init()" methods in the class
	 */
	private void invokeMethodsIn(final Class<?> clazz) throws Exception {
		for (final Method method : clazz.getDeclaredMethods()) {

			if (!SimplePlugin.getInstance().isEnabled())
				return;

			final int mod = method.getModifiers();

			if (method.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) &&
						Modifier.isStatic(mod) &&
						method.getReturnType() == Void.TYPE &&
						method.getParameterTypes().length == 0,
						"Method '" + method.getName() + "' in " + clazz + " must be 'private static void init()'");

				method.setAccessible(true);
				method.invoke(null);
			}
		}

		checkFields(clazz);
	}

	/*
	 * Safety check whether all fields have been set
	 */
	private void checkFields(final Class<?> clazz) throws Exception {

		if (clazz == YamlStaticConfig.class)
			return;

		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			if (Modifier.isPublic(field.getModifiers()))
				Valid.checkBoolean(!field.getType().isPrimitive(), "Field '" + field.getName() + "' in " + clazz + " must not be primitive!");

			Object result = null;
			try {
				result = field.get(null);
			} catch (final NullPointerException ex) {
			}
			Valid.checkNotNull(result, "Null " + field.getType().getSimpleName() + " field '" + field.getName() + "' in " + clazz);
		}
	}

	// -----------------------------------------------------------------------------------------------------
	// Delegate methods
	// -----------------------------------------------------------------------------------------------------

	protected final void loadConfiguration(String internalPath) {
		TEMPORARY_INSTANCE.loadConfiguration(internalPath, internalPath);
	}

	protected final void loadConfiguration(String from, String to) {
		TEMPORARY_INSTANCE.loadConfiguration(from, to);
	}

	protected static final void set(final String path, final Object value) {
		TEMPORARY_INSTANCE.set(path, value);
	}

	protected static final boolean isSet(final String path) {
		return TEMPORARY_INSTANCE.isSet(path);
	}

	protected static final boolean isSetDefault(final String path) {
		return TEMPORARY_INSTANCE.isSetDefault(path);
	}

	protected static final void move(final String fromRelative, final String toAbsolute) {
		TEMPORARY_INSTANCE.move(fromRelative, toAbsolute);
	}

	protected static final void setPathPrefix(final String pathPrefix) {
		TEMPORARY_INSTANCE.setPathPrefix(pathPrefix);
	}

	protected static final String getPathPrefix() {
		return TEMPORARY_INSTANCE.getPathPrefix();
	}

	protected static final String getFileName() {
		return TEMPORARY_INSTANCE.getFileName();
	}

	/**
	 * @deprecated ugly workaround for some of our older plugins, do not use
	 *
	 * @return
	 */
	@Deprecated
	protected static YamlConfig getInstance() {
		return TEMPORARY_INSTANCE;
	}

	// -----------------------------------------------------------------------------------------------------
	// Config manipulators
	// -----------------------------------------------------------------------------------------------------

	protected static final List<CompMaterial> getMaterialList(final String path) {
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

	protected static final <E> IsInList<E> getIsInList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getIsInList(path, listType);
	}

	protected static final boolean getBoolean(final String path) {
		return TEMPORARY_INSTANCE.getBoolean(path);
	}

	protected static final String getString(final String path) {
		return TEMPORARY_INSTANCE.getString(path);
	}

	protected static final int getInteger(final String path) {
		return TEMPORARY_INSTANCE.getInteger(path);
	}

	protected static final int getInteger(final String path, int def) {
		return TEMPORARY_INSTANCE.getInteger(path, def);
	}

	protected static final double getDouble(final String path) {
		return TEMPORARY_INSTANCE.getDouble(path);
	}

	protected static final SimpleSound getSound(final String path) {
		return TEMPORARY_INSTANCE.getSound(path);
	}

	protected static final AccusativeHelper getCasus(final String path) {
		return TEMPORARY_INSTANCE.getAccusativePeriod(path);
	}

	protected static final TitleHelper getTitle(final String path) {
		return TEMPORARY_INSTANCE.getTitle(path);
	}

	protected static final SimpleTime getTime(final String path) {
		return TEMPORARY_INSTANCE.getTime(path);
	}

	protected static final double getPercentage(String path) {
		return TEMPORARY_INSTANCE.getPercentage(path);
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

	protected static final Object getObject(final String path) {
		return TEMPORARY_INSTANCE.getObject(path);
	}

	protected static final SerializedMap getMap(final String path) {
		return TEMPORARY_INSTANCE.getMap(path);
	}

	protected static final <Key, Value> LinkedHashMap<Key, Value> getMap(final String path, final Class<Key> keyType, final Class<Value> valueType) {
		return TEMPORARY_INSTANCE.getMap(path, keyType, valueType);
	}
}