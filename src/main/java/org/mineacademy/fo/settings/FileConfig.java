package org.mineacademy.fo.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;

public abstract class FileConfig {

	private static final Map<String, ConfigSection> loadedSections = new HashMap<>();

	public static final String NO_DEFAULT = null;

	private File file;
	private String header = null;

	ConfigSection section = new ConfigSection(); // overridden in load(File)
	ConfigSection defaults;
	String defaultsPath;

	private String pathPrefix = null;

	protected FileConfig() {
	}

	// ------------------------------------------------------------------------------------
	// Getting fields
	// ------------------------------------------------------------------------------------

	@NonNull
	public final Set<String> getKeys(boolean deep) {
		return this.section.getKeys(deep);
	}

	@NonNull
	public final Map<String, Object> getValues(boolean deep) {
		return this.section.getValues(deep);
	}

	public final <T> T get(final String path, final Class<T> type, Object... deserializeParams) {
		return this.get(path, type, null, deserializeParams);
	}

	public final <T> T get(@NonNull String path, Class<T> type, T def, Object... deserializeParams) {

		path = this.buildPathPrefix(path);

		// Copy defaults if not set and log about this change
		this.copyDefault(path, type);

		Object raw = this.section.retrieve(path);

		if (this.defaults != null && def == null)
			Valid.checkNotNull(raw, "Failed to set '" + path + "' to " + type.getSimpleName() + " from default config's value: " + this.defaults.retrieve(path) + ", has values: " + this.section.map.keySet());

		if (raw != null) {

			// Workaround for empty lists
			if (raw.equals("[]") && type == List.class)
				raw = new ArrayList<>();

			// Retype manually
			if (type == Long.class && raw instanceof Integer)
				raw = ((Integer) raw).longValue();

			raw = SerializeUtil.deserialize(type, raw, deserializeParams);
			this.checkAssignable(false, path, raw, type);

			return (T) raw;
		}

		return def;
	}

	private void copyDefault(final String path, final Class<?> type) {
		if (this.defaults != null && !this.section.isStored(path)) {
			Object object = this.defaults.retrieve(path);

			Valid.checkNotNull(object, "Inbuilt config " + this.getFileName() + " lacks " + (object == null ? "key" : object.getClass().getSimpleName()) + " at \"" + path + "\". Is it outdated?");
			this.checkAssignable(true, path, object, type);

			object = SerializeUtil.serialize(object);

			Common.log("&7Updating " + this.getFileName() + " at &b\'&f" + path + "&b\' &7-> " + (object == null ? "&ckey removed" : "&b\'&f" + object + "&b\'") + "&r");
			this.section.store(path, object);
		}
	}

	private void checkAssignable(final boolean fromDefault, final String path, final Object object, final Class<?> type) {
		if (!type.isAssignableFrom(object.getClass()) && !type.getSimpleName().equals(object.getClass().getSimpleName())) {

			// Exception
			if (ConfigSerializable.class.isAssignableFrom(type) && object instanceof ConfigSection)
				return;

			throw new FoException("Malformed configuration! Key '" + path + "' in " + (fromDefault ? "inbuilt " : "") + this.getFileName() + " must be " + type.getSimpleName() + " but got " + object.getClass().getSimpleName() + ": '" + object + "'");
		}
	}

	// ------------------------------------------------------------------------------------
	// Getting values helpers
	// ------------------------------------------------------------------------------------

	public final String getString(final String path) {
		return this.getString(path, null);
	}

	public final String getString(final String path, final String def) {
		final Object object = this.getObject(path, def);

		if (object == null)
			return null;

		else if (object instanceof List)
			return Common.join((List<?>) object, "\n");

		else if (object instanceof String[])
			return Common.join(Arrays.asList((String[]) object), "\n");

		else if (object.getClass().isArray())
			return Common.join((Object[]) object);

		else if (object instanceof Boolean
				|| object instanceof Integer
				|| object instanceof Long
				|| object instanceof Double
				|| object instanceof Float)
			return Objects.toString(object);

		else if (object instanceof Number)
			return ((Number) object).toString();

		else if (object instanceof String)
			return (String) object;

		throw new FoException("Excepted string at '" + path + "' in " + this.getFileName() + ", got (" + object.getClass() + "): " + object);
	}

	public final Boolean getBoolean(final String path) {
		return this.getBoolean(path, null);
	}

	public final Boolean getBoolean(final String path, final Boolean def) {
		return this.get(path, Boolean.class, def);
	}

	public final Integer getInteger(final String path) {
		return this.getInteger(path, null);
	}

	public final Integer getInteger(final String path, final Integer def) {
		return this.get(path, Integer.class, def);
	}

	public final Long getLong(final String path) {
		return this.getLong(path, null);
	}

	public final Long getLong(final String path, final Long def) {
		return this.get(path, Long.class, def);
	}

	public final Double getDouble(final String path) {
		return this.getDouble(path, null);
	}

	public final Double getDouble(final String path, final Double def) {
		final Object raw = this.getObject(path, def);

		if (raw != null)
			Valid.checkBoolean(raw instanceof Number, "Expected a number at '" + path + "', got " + raw.getClass().getSimpleName() + ": " + raw);

		return raw != null ? ((Number) raw).doubleValue() : null;
	}

	public final Location getLocation(final String path) {
		return this.getLocation(path, null);
	}

	public final Location getLocation(final String path, final Location def) {
		return this.get(path, Location.class, def);
	}

	public final OfflinePlayer getOfflinePlayer(final String path) {
		return this.getOfflinePlayer(path, null);
	}

	public final OfflinePlayer getOfflinePlayer(final String path, final OfflinePlayer def) {
		return this.get(path, OfflinePlayer.class, def);
	}

	public final SimpleSound getSound(final String path) {
		return this.getSound(path, null);
	}

	public final SimpleSound getSound(final String path, final SimpleSound def) {
		return this.get(path, SimpleSound.class, def);
	}

	public final AccusativeHelper getAccusativePeriod(final String path) {
		return this.getAccusativePeriod(path, null);
	}

	public final AccusativeHelper getAccusativePeriod(final String path, final String def) {
		final String rawLine = this.getString(path, def);

		return rawLine != null ? new AccusativeHelper(rawLine) : null;
	}

	public final TitleHelper getTitle(final String path) {
		return this.getTitle(path, null, null);
	}

	public final TitleHelper getTitle(final String path, final String defTitle, final String defSubtitle) {
		final String title = this.getString(path + ".Title", defTitle);
		final String subtitle = this.getString(path + ".Subtitle", defSubtitle);

		return title != null ? new TitleHelper(title, subtitle) : null;
	}

	public final SimpleTime getTime(final String path) {
		return this.getTime(path, null);
	}

	public final SimpleTime getTime(final String path, final SimpleTime def) {
		return this.get(path, SimpleTime.class, def);
	}

	public final Double getPercentage(String path) {
		return this.getPercentage(path, null);
	}

	public final Double getPercentage(String path, Double def) {

		final Object object = this.getObject(path, def);

		if (object != null) {
			final String raw = object.toString();
			Valid.checkBoolean(raw.endsWith("%"), "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must end with %! Got: " + raw);

			final String rawNumber = raw.substring(0, raw.length() - 1);
			Valid.checkInteger(rawNumber, "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must be a whole number! Got: " + raw);

			return Integer.parseInt(rawNumber) / 100D;
		}

		return null;
	}

	public final BoxedMessage getBoxedMessage(final String path) {
		return this.getBoxedMessage(path, null);
	}

	public final BoxedMessage getBoxedMessage(final String path, final BoxedMessage def) {
		return this.get(path, BoxedMessage.class, def);
	}

	public final CompMaterial getMaterial(final String path) {
		return this.getMaterial(path, null);
	}

	public final CompMaterial getMaterial(final String path, CompMaterial def) {
		return this.get(path, CompMaterial.class, def);
	}

	public final ItemStack getItemStack(@NonNull String path) {
		return this.getItemStack(path, null);
	}

	public final ItemStack getItemStack(@NonNull String path, ItemStack def) {
		return this.get(path, ItemStack.class, def);
	}

	public final <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return this.getTuple(key, null, keyType, valueType);
	}

	public final <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		return this.get(key, Tuple.class, def, keyType, valueType);
	}

	public final Object getObject(final String path) {
		return this.getObject(path, null);
	}

	public final Object getObject(final String path, final Object def) {
		return this.get(path, Object.class, def);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting lists
	// ------------------------------------------------------------------------------------------------------------

	public final LocationList getLocationList(final String path) {
		return new LocationList(this, this.getList(path, Location.class));
	}

	public final List<Object> getList(final String path) {
		final Object obj = this.getObject(path);

		// Allow one values instead of lists, such as
		// "Apply_On: timed" instead of "Apply_On: [timed]" for convenience
		return obj instanceof List ? (List<Object>) obj : obj != null ? Arrays.asList(obj) : new ArrayList<>();
	}

	public final List<SerializedMap> getMapList(final String path) {
		return this.getList(path, SerializedMap.class);
	}

	public final <T> Set<T> getSet(final String key, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = this.getList(key, type);

		return list == null ? new HashSet<>() : new HashSet<>(list);
	}

	public final List<CompMaterial> getMaterialList(final String path) {
		return this.getList(path, CompMaterial.class);
	}

	public final <T> List<T> getList(final String path, final Class<T> type) {
		return this.getList(path, type, (Object[]) null);
	}

	public final <T> List<T> getList(final String path, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = this.getList(path);

		if (type == Map.class && deserializeParameters != null & deserializeParameters.length > 0 && deserializeParameters[0] != String.class)
			throw new FoException("getList('" + path + "') that returns Map must have String.class as key, not " + deserializeParameters[0]);

		if (objects != null)
			for (Object object : objects) {
				object = object != null ? SerializeUtil.deserialize(type, object, deserializeParameters) : null;

				if (object != null)
					list.add((T) object);
			}

		return list;
	}

	public final <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = this.getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(this.getList(path, type));
	}

	public final List<String> getStringList(final String path) {
		final Object raw = this.getObject(path);

		if (raw == null)
			return new ArrayList<>();

		if (raw instanceof String) {
			final String output = (String) raw;

			return "'[]'".equals(output) || "[]".equals(output) ? new ArrayList<>() : Arrays.asList(output);
		}

		if (raw instanceof List)
			return this.fixYamlBooleansInList((List<Object>) raw);

		throw new FoException("Excepted a list at '" + path + "' in " + this.getFileName() + ", got (" + raw.getClass() + "): " + raw);
	}

	/*
	 * Attempts to convert objects into strings, since SnakeYAML parser interprets
	 * "true" and "yes" as boolean types
	 */
	private List<String> fixYamlBooleansInList(@NonNull final Iterable<Object> list) {
		final List<String> newList = new ArrayList<>();

		for (final Object obj : list)
			if (obj != null)
				newList.add(obj.toString());

		return newList;
	}

	@Nullable
	public final StrictList<String> getCommandList(final String path) {

		// Nowhere to copy from
		//if (!this.isSet(path) && this.defaults == null)
		//	return null;

		final List<String> list = this.getStringList(path);
		Valid.checkBoolean(!list.isEmpty(), "Please set at least one command alias in '" + path + "' (" + this.getFileName() + ") for this will be used as your main command!");

		for (int i = 0; i < list.size(); i++) {
			String command = list.get(i);

			command = command.startsWith("/") ? command.substring(1) : command;
			list.set(i, command);
		}

		return new StrictList<>(list);
	}

	public final SerializedMap getMap(final String path) {
		final LinkedHashMap<?, ?> map = this.getMap(path, Object.class, Object.class);

		return SerializedMap.of(map);
	}

	public final <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType, Object... valueDeserializeParams) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();
		final boolean exists = this.isSet(path);

		// Add path prefix right away
		path = this.buildPathPrefix(path);

		// Add defaults
		if (this.defaults != null && !exists) {
			Valid.checkBoolean(this.defaults.isStored(path), "Default '" + this.getFileName() + "' lacks a map at " + path);

			for (final String key : this.defaults.retrieveConfigurationSection(path).getKeys(false))
				this.copyDefault(path + "." + key, valueType);
		}

		// Load key-value pairs from config to our map
		if (exists) {
			final Object object = this.section.retrieve(path);
			Valid.checkBoolean(object instanceof ConfigSection, "Expected a map at '" + path + "', got " + object.getClass().getSimpleName() + ": " + object);

			for (final Map.Entry<String, Object> entry : ((ConfigSection) object).map.entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final Value value = SerializeUtil.deserialize(valueType, entry.getValue(), valueDeserializeParams);

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(false, path, key, keyType);
				this.checkAssignable(false, path, value, valueType);

				map.put(key, value);
			}
		}

		return map;
	}

	public final <Key, Value> LinkedHashMap<Key, List<Value>> getMapList(@NonNull String path, final Class<Key> keyType, final Class<Value> setType, Object... setDeserializeParameters) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, List<Value>> map = new LinkedHashMap<>();
		final boolean exists = this.isSet(path);

		// Add path prefix right away
		path = this.buildPathPrefix(path);

		// Add defaults
		if (this.defaults != null && !exists) {
			Valid.checkBoolean(this.defaults.isStored(path), "Default '" + this.getFileName() + "' lacks a map at " + path);

			for (final String key : this.defaults.retrieveConfigurationSection(path).getKeys(false))
				this.copyDefault(path + "." + key, setType);
		}

		// Load key-value pairs from config to our map
		if (exists) {
			final Object object = this.section.retrieve(path);
			Valid.checkBoolean(object instanceof ConfigSection, "Expected a map at '" + path + "', got " + object.getClass().getSimpleName() + ": " + object);

			for (final Map.Entry<String, Object> entry : ((ConfigSection) object).map.entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final List<Value> value = SerializeUtil.deserialize(List.class, entry.getValue(), setDeserializeParameters);

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(false, path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						this.checkAssignable(false, path, item, setType);

				map.put(key, value);
			}
		}

		return map;
	}

	// ------------------------------------------------------------------------------------
	// Setting values
	// ------------------------------------------------------------------------------------

	public final void save(String path, Object value) {
		this.set(path, value);

		this.save();
	}

	public final void set(String path, Object value) {
		path = this.buildPathPrefix(path);
		value = SerializeUtil.serialize(value);

		this.section.store(path, value);
	}

	public final boolean isSet(String path) {
		path = this.buildPathPrefix(path);

		return this.section.isStored(path);
	}

	public void move(final String fromRelative, final String toAbsolute) {
		this.move(this.getObject(fromRelative), fromRelative, toAbsolute);
	}

	public void move(final Object value, String fromPathRel, final String toPathAbs) {
		this.set(fromPathRel, null);
		this.set(toPathAbs, value);

		Common.log("&7Update " + this.getFileName() + ". Move &b\'&f" + this.buildPathPrefix(fromPathRel) + "&b\' &7(was \'" + value + "&7\') to " + "&b\'&f" + toPathAbs + "&b\'" + "&r");
	}

	// ------------------------------------------------------------------------------------
	// File manipulation
	// ------------------------------------------------------------------------------------

	public final void reload() {
		Valid.checkNotNull(this.file, "Cannot call reload() before loading a file!");

		this.load(this.file);
	}

	final void load(@NonNull File file) {
		synchronized (loadedSections) {
			try {
				final FileInputStream stream = new FileInputStream(file);
				final String path = file.getAbsolutePath();
				ConfigSection section = loadedSections.get(path);

				if (section == null) {
					section = new ConfigSection();

					loadedSections.put(path, section);
				}

				this.section = section;
				this.file = file;

				this.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
				this.onLoad();
				this.save();

			} catch (final Exception ex) {
				Remain.sneaky(ex);
			}
		}
	}

	private final void load(@NonNull Reader reader) {

		try {
			final BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
			final StringBuilder builder = new StringBuilder();

			try {
				String line;

				while ((line = input.readLine()) != null) {
					builder.append(line);
					builder.append('\n');
				}
			} finally {
				input.close();
			}

			this.loadFromString(builder.toString());

		} catch (final Exception ex) {
			Remain.sneaky(ex);
		}
	}

	abstract void loadFromString(@NonNull String contents);

	protected void onLoad() {
	}

	public final void save() {
		Valid.checkNotNull(this.file, "Cannot call save() for " + this + " when no file was set! Call load first!");

		this.save(file);
	}

	public final void save(@NonNull File file) {
		synchronized (loadedSections) {
			try {
				this.onSave();

				final File parent = file.getCanonicalFile().getParentFile();

				if (parent != null)
					parent.mkdirs();

				final String data = this.saveToString();
				final Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);

				try {
					writer.write(data);

				} finally {
					writer.close();
				}

			} catch (final Exception ex) {
				Remain.sneaky(ex);
			}
		}
	}

	protected void onSave() {
	}

	@NonNull
	abstract String saveToString();

	public final void deleteFile() {
		if (this.file.exists()) {

			this.file.delete();
			this.unregister();
		}
	}

	public final void unregister() {
		Valid.checkNotNull(this.file, "Cannot unregister null file before settings were loaded!");

		loadedSections.remove(this.file.getAbsolutePath());
	}

	// ------------------------------------------------------------------------------------
	// Path prefix
	// ------------------------------------------------------------------------------------

	final String buildPathPrefix(@NonNull final String path) {
		final String prefixed = this.pathPrefix != null ? this.pathPrefix + (!path.isEmpty() ? "." + path : "") : path;
		final String newPath = prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;

		// Check for a case where there is multiple dots at the end... #somePeople
		Valid.checkBoolean(!newPath.endsWith("."), "Path '" + path + "' must not end with '.' after path prefix '" + this.pathPrefix + "': " + newPath);
		return newPath;
	}

	protected final void setPathPrefix(final String pathPrefix) {
		if (pathPrefix != null) {
			Valid.checkBoolean(!pathPrefix.endsWith("."), "Path prefix must not end with a dot: " + pathPrefix);
			Valid.checkBoolean(!pathPrefix.endsWith(".yml"), "Path prefix must not end with .yml!");
		}

		this.pathPrefix = pathPrefix != null && !pathPrefix.isEmpty() ? pathPrefix : null;
	}

	protected final String getPathPrefix() {
		return this.pathPrefix;
	}

	// ------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------

	public final void clear() {
		this.section.clear();
	}

	public final String getFileName() {
		return this.file == null ? "null" : this.file.getName();
	}

	public final String getHeader() {
		return this.header;
	}

	public final void setHeader(String... value) {
		this.header = String.join("\n", value);
	}

	public final boolean isEmpty() {
		return this.section.isEmpty();
	}

	public final SerializedMap serialize() {
		return this.section.serialize();
	}

	// ------------------------------------------------------------------------------------
	// Class
	// ------------------------------------------------------------------------------------

	public static final class AccusativeHelper {

		private final String accusativeSingural; // 1 second (Slovak case - sekundu)
		private final String accusativePlural; // 2-4 seconds (Slovak case - sekundy, not in English)
		private final String genitivePlural; // 0 or 5+ seconds (Slovak case - sekund)

		private AccusativeHelper(final String raw) {
			final String[] values = raw.split(", ");

			if (values.length == 2) {
				this.accusativeSingural = values[0];
				this.accusativePlural = values[1];
				this.genitivePlural = this.accusativePlural;

				return;
			}

			if (values.length != 3)
				throw new FoException("Malformed type, use format: 'second, seconds' OR 'sekundu, sekundy, sekund' (if your language has it)");

			this.accusativeSingural = values[0];
			this.accusativePlural = values[1];
			this.genitivePlural = values[2];
		}

		public String getPlural() {
			return this.genitivePlural;
		}

		public String formatWithCount(final long count) {
			return count + " " + this.formatWithoutCount(count);
		}

		public String formatWithoutCount(final long count) {
			if (count == 1)
				return this.accusativeSingural;

			if (count > 1 && count < 5)
				return this.accusativePlural;

			return this.genitivePlural;
		}
	}

	public static final class TitleHelper {

		private final String title, subtitle;

		private TitleHelper(final String title, final String subtitle) {
			this.title = Common.colorize(title);
			this.subtitle = Common.colorize(subtitle);
		}

		public void playLong(final Player player) {
			this.playLong(player, null);
		}

		public void playLong(final Player player, final Function<String, String> replacer) {
			this.play(player, 5, 4 * 20, 15, replacer);
		}

		public void playShort(final Player player) {
			this.playShort(player, null);
		}

		public void playShort(final Player player, final Function<String, String> replacer) {
			this.play(player, 3, 2 * 20, 5, replacer);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut) {
			this.play(player, fadeIn, stay, fadeOut, null);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut, Function<String, String> replacer) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer != null ? replacer.apply(this.title) : this.title, replacer != null ? replacer.apply(this.subtitle) : this.subtitle);
		}
	}

	public static final class LocationList implements Iterable<Location> {

		private final FileConfig settings;

		private final List<Location> points;

		private LocationList(final FileConfig settings, final List<Location> points) {
			this.settings = settings;
			this.points = points;
		}

		public boolean toggle(final Location location) {
			for (final Location point : this.points)
				if (Valid.locationEquals(point, location)) {
					this.points.remove(point);

					this.settings.save();
					return false;
				}

			this.points.add(location);
			this.settings.save();

			return true;
		}

		public void add(final Location location) {
			Valid.checkBoolean(!this.hasLocation(location), "Location at " + location + " already exists!");

			this.points.add(location);
			this.settings.save();
		}

		public void remove(final Location location) {
			final Location point = this.find(location);
			Valid.checkNotNull(point, "Location at " + location + " does not exist!");

			this.points.remove(point);
			this.settings.save();
		}

		public boolean hasLocation(final Location location) {
			return this.find(location) != null;
		}

		public Location find(final Location location) {
			for (final Location entrance : this.points)
				if (Valid.locationEquals(entrance, location))
					return entrance;

			return null;
		}

		public List<Location> getLocations() {
			return Collections.unmodifiableList(this.points);
		}

		@Override
		public Iterator<Location> iterator() {
			return this.points.iterator();
		}

		public int size() {
			return this.points.size();
		}
	}
}
