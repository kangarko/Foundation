package org.mineacademy.fo;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictCollection;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.SerializeFailedException;
import org.mineacademy.fo.jsonsimple.JSONArray;
import org.mineacademy.fo.jsonsimple.JSONObject;
import org.mineacademy.fo.jsonsimple.JSONParseException;
import org.mineacademy.fo.jsonsimple.JSONParser;
import org.mineacademy.fo.jsonsimple.Jsonable;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.settings.ConfigSection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SerializeUtilCore {

	/**
	 * A custom serializer extending the serialize method
	 */
	private static final List<Serializer> customSerializers = new ArrayList<>();

	/**
	 * Converts the given object into something you can safely save in file as a string
	 *
	 * @param mode determines the file that the object originated from, if unsure just set to YAML
	 * @param object
	 * @return
	 */
	public static Object serialize(Mode mode, Object object) {
		if (object == null)
			return null;

		final boolean isJson = mode == Mode.JSON;
		object = RemainCore.getRootOfSectionPathData(object);

		for (final Serializer customSerializer : customSerializers) {
			final Object result = customSerializer.serialize(mode, object);

			if (result != null)
				return result;
		}

		if (object instanceof ConfigSerializable)
			return serialize(mode, ((ConfigSerializable) object).serialize().serialize());

		else if (object instanceof StrictCollection)
			return serialize(mode, ((StrictCollection) object).serialize());

		else if (object instanceof CompChatColor)
			return ((CompChatColor) object).toSaveableString();

		else if (object instanceof BoxedMessage) {
			final Component[] messages = ((BoxedMessage) object).getMessages();

			return RemainCore.convertAdventureToMini(Component.textOfChildren(messages));

		} else if (object instanceof UUID)
			return object.toString();

		else if (object instanceof Enum<?>)
			return object.toString();

		else if (object instanceof SimpleTime)
			return ((SimpleTime) object).getRaw();

		else if (object instanceof Color)
			return "#" + ((Color) object).getRGB();

		else if (object instanceof RangedValue)
			return ((RangedValue) object).toLine();

		else if (object instanceof RangedSimpleTime)
			return ((RangedSimpleTime) object).toLine();

		else if (object instanceof Component)
			return RemainCore.convertAdventureToMini((Component) object);

		else if (object instanceof Path)
			throw new FoException("Cannot serialize Path " + object + ", did you mean to convert it into a name?");

		else if (object instanceof Iterable || object.getClass().isArray() || object instanceof IsInList) {

			if (isJson) {
				final JSONArray jsonList = new JSONArray();

				if (object instanceof Iterable || object instanceof IsInList) {
					for (final Object element : object instanceof IsInList ? ((IsInList<?>) object).getList() : (Iterable<?>) object)
						addJsonElement(element, jsonList);

				} else {
					final Object[] array = (Object[]) object;

					for (int i = 0; i < array.length; i++)
						jsonList.add(toJsonElement(array[i]));
				}

				return jsonList;
			}

			else {
				if (object instanceof Iterable || object instanceof IsInList) {
					final List<Object> serialized = new ArrayList<>();

					for (final Object element : object instanceof IsInList ? ((IsInList<?>) object).getList() : (Iterable<?>) object)
						serialized.add(serialize(mode, element));

					return serialized;

				} else {
					// Supports Object[] as well as primitive arrays
					final int length = Array.getLength(object);
					final Object[] serialized = new Object[length];

					for (int i = 0; i < length; i++) {
						final Object element = Array.get(object, i);

						serialized[i] = serialize(mode, element);
					}

					return serialized;
				}
			}

		} else if (object instanceof Map || object instanceof StrictMap) {
			final Map<Object, Object> oldMap = object instanceof StrictMap ? ((StrictMap<Object, Object>) object).getSource() : (Map<Object, Object>) object;

			if (isJson) {
				final JSONObject json = new JSONObject();

				for (final Map.Entry<Object, Object> entry : oldMap.entrySet()) {
					final Object key = serialize(mode, entry.getKey());
					final Object value = serialize(mode, entry.getValue());

					if (key != null)
						ValidCore.checkBoolean(key instanceof String || key instanceof Number,
								"JSON requires Map to be translated into keys that are String or Numbers, found " + key.getClass().getSimpleName() + " key: " + key + " with value '" + value + "'");

					if (value != null)
						ValidCore.checkBoolean(value instanceof String || value instanceof Boolean || value instanceof Character || value instanceof Number || value instanceof List
								|| value instanceof JSONObject || value instanceof JSONArray,
								"JSON requires Map to be translated into values that are String or List only, found " + value.getClass().getSimpleName() + ": " + value + " for key " + key);

					if (value instanceof List) {
						final JSONArray array = new JSONArray();

						for (final Object listValue : (List<?>) value)
							if (listValue == null || listValue instanceof Boolean || listValue instanceof Character || listValue instanceof String || listValue instanceof Number
									|| listValue instanceof JSONArray || listValue instanceof JSONObject)
								array.add(listValue);

							else
								throw new FoException("JSON requires List to only contain primitive types or strings, found " + listValue.getClass().getSimpleName() + ": " + listValue);

						json.put(key == null ? null : key.toString(), array);

					} else
						json.put(key == null ? null : key.toString(), value == null ? null : value);
				}

				return json;

			}

			else {
				final Map<Object, Object> newMap = new LinkedHashMap<>();

				for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
					newMap.put(serialize(mode, entry.getKey()), serialize(mode, entry.getValue()));

				return newMap;
			}
		}

		else if (object instanceof ConfigSection)
			return serialize(mode, ((ConfigSection) object).getValues(true));

		else if (object instanceof Pattern)
			return ((Pattern) object).pattern();

		else if (object instanceof Integer || object instanceof Double || object instanceof Float || object instanceof Long || object instanceof Short
				|| object instanceof String || object instanceof Boolean || object instanceof Character)
			return object;

		else if (object instanceof BigDecimal) {
			final BigDecimal big = (BigDecimal) object;

			return big.toPlainString();
		}

		throw new SerializeFailedException("Does not know how to serialize " + object.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + object);
	}

	/*
	 * Helps to add an unknown element into a json list
	 */
	private static void addJsonElement(Object element, JSONArray jsonList) {
		if (element == null)
			return;

		if (element instanceof Jsonable)
			jsonList.add(element);

		else {
			element = serialize(Mode.JSON, element);

			// Assume the element is a JSON string
			try {
				jsonList.add(JSONParser.deserialize(element.toString()));

			} catch (final JSONParseException ex) {
				final String message = ex.getMessage();

				// Apparently not a json string :/
				if (message.contains("The unexpected character") && (message.contains("was found at position 0") || message.contains("was found at position 1")))
					jsonList.add(element.toString());
				else
					CommonCore.error(ex, "Failed to deserialize JSON collection from string: " + element);
			}
		}
	}

	/*
	 * Helps to add an unknown element into a json list
	 */
	private static Object toJsonElement(Object element) {
		if (element == null)
			return null;

		if (element instanceof Jsonable)
			return element;

		else {
			element = serialize(Mode.JSON, element);

			// Assume the element is a JSON string
			try {
				return JSONParser.deserialize(element.toString());

			} catch (final JSONParseException ex) {
				final String message = ex.getMessage();

				// Apparently not a json string :/
				if (message.contains("The unexpected character") && (message.contains("was found at position 0") || message.contains("was found at position 1")))
					return element.toString();
				else
					CommonCore.error(ex, "Failed to deserialize JSON collection from string: " + element);
			}
		}

		return null;
	}

	/**
	 * Attempts to convert the given object saved in the given mode (i.e. in a .yml file) back
	 * into its Java class, i.e. a Location.
	 *
	 * @param <T>
	 * @param mode
	 * @param classOf
	 * @param object
	 * @return
	 */
	public static <T> T deserialize(@NonNull Mode mode, @NonNull final Class<T> classOf, @NonNull final Object object) {
		return deserialize(mode, classOf, object, (Object[]) null);
	}

	/**
	 * Attempts to convert the given object saved in the given mode (i.e. in a .yml file) back
	 * into its Java class, i.e. a Location.
	 *
	 * @param <T>
	 * @param mode
	 * @param classOf
	 * @param object
	 * @param parameters
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T deserialize(@NonNull Mode mode, @NonNull final Class<T> classOf, @NonNull Object object, final Object... parameters) {

		final boolean isJson = mode == Mode.JSON;

		for (final Serializer customDeserializer : customSerializers) {
			final T result = customDeserializer.deserialize(mode, classOf, object, parameters);

			if (result != null)
				return result;
		}

		if (classOf == String.class)
			object = object.toString();

		else if (classOf == Integer.class)
			object = Integer.parseInt(object.toString());

		else if (classOf == Long.class)
			object = Long.decode(object.toString());

		else if (classOf == Double.class)
			object = Double.parseDouble(object.toString());

		else if (classOf == Float.class)
			object = Float.parseFloat(object.toString());

		else if (classOf == Boolean.class)
			object = Boolean.parseBoolean(object.toString());

		else if (classOf == SerializedMap.class)
			object = isJson ? SerializedMap.fromJson(object.toString()) : SerializedMap.of(object);

		else if (classOf == BoxedMessage.class)
			object = new BoxedMessage(CommonCore.colorize(object.toString()));

		else if (classOf == SimpleTime.class)
			object = SimpleTime.from(object.toString());

		else if (classOf == RangedValue.class)
			object = RangedValue.parse(object.toString());

		else if (classOf == RangedSimpleTime.class)
			object = RangedSimpleTime.parse(object.toString());

		else if (classOf == CompChatColor.class)
			object = CompChatColor.of(object.toString());

		else if (classOf == UUID.class)
			object = UUID.fromString(object.toString());

		else if (classOf == Component.class)
			object = CommonCore.colorize(object.toString());

		else if (Enum.class.isAssignableFrom(classOf)) {
			object = ReflectionUtilCore.lookupEnum((Class<Enum>) classOf, object.toString());

			if (object == null)
				return null;
		}

		else if (Color.class.isAssignableFrom(classOf))
			object = CompChatColor.of(object.toString()).getColor();
		else if (List.class.isAssignableFrom(classOf) && object instanceof List) {
			// Good

		} else if (Map.class.isAssignableFrom(classOf)) {
			if (object instanceof Map)
				return (T) object;

			if (object instanceof ConfigSection)
				return (T) ((ConfigSection) object).getValues(false);

			if (isJson)
				return (T) SerializedMap.fromJson(object.toString()).asMap();

			throw new SerializeFailedException("Does not know how to turn " + object.getClass().getSimpleName() + " into a Map! (Keep in mind we can only serialize into Map<String, Object> Data: " + object);

		} else if (classOf.isArray()) {
			final Class<?> arrayType = classOf.getComponentType();
			T[] array;

			if (object instanceof List) {
				final List<?> rawList = (List<?>) object;
				array = (T[]) Array.newInstance(classOf.getComponentType(), rawList.size());

				for (int i = 0; i < rawList.size(); i++) {
					final Object element = rawList.get(i);

					array[i] = element == null ? null : (T) deserialize(mode, arrayType, element, (Object[]) null);
				}
			}

			else {
				final Object[] rawArray = (Object[]) object;
				array = (T[]) Array.newInstance(classOf.getComponentType(), rawArray.length);

				for (int i = 0; i < array.length; i++)
					array[i] = rawArray[i] == null ? null : (T) deserialize(mode, classOf.getComponentType(), rawArray[i], (Object[]) null);
			}

			return (T) array;
		}

		// Try to call our own serializers
		else if (ConfigSerializable.class.isAssignableFrom(classOf)) {
			if (parameters != null && parameters.length > 0) {
				final List<Class<?>> argumentClasses = new ArrayList<>();
				final List<Object> arguments = new ArrayList<>();

				// Build parameters
				argumentClasses.add(SerializedMap.class);
				for (final Object param : parameters)
					argumentClasses.add(param.getClass());

				// Build parameter instances
				arguments.add(isJson ? SerializedMap.fromJson(object.toString()) : SerializedMap.of(object));
				Collections.addAll(arguments, parameters);

				// Find deserialize(SerializedMap, args[]) method
				final Method deserialize = ReflectionUtilCore.getMethod(classOf, "deserialize", argumentClasses.toArray(new Class[argumentClasses.size()]));

				ValidCore.checkNotNull(deserialize,
						"Expected " + classOf.getSimpleName() + " to have a public static deserialize(SerializedMap, " + CommonCore.join(argumentClasses) + ") method to deserialize: " + object + " when params were given: " + CommonCore.join(parameters));

				ValidCore.checkBoolean(argumentClasses.size() == arguments.size(),
						classOf.getSimpleName() + "#deserialize(SerializedMap, " + argumentClasses.size() + " args) expected, " + arguments.size() + " given to deserialize: " + object);

				return ReflectionUtilCore.invokeStatic(deserialize, arguments.toArray());
			}

			final Method deserialize = ReflectionUtilCore.getMethod(classOf, "deserialize", SerializedMap.class);

			if (deserialize != null)
				return ReflectionUtilCore.invokeStatic(deserialize, isJson ? SerializedMap.fromJson(object.toString()) : SerializedMap.of(object));

			throw new SerializeFailedException("Unable to deserialize " + classOf.getSimpleName()
					+ ", please write 'public static deserialize(SerializedMap map) or deserialize(SerializedMap map, X arg1, Y arg2, etc.) method to deserialize: " + object);
		}

		// Step 3 - Search for "getByName" method used by us or some Bukkit classes such as Enchantment
		else if (object instanceof String) {
			final Method method = ReflectionUtilCore.getMethod(classOf, "getByName", String.class);

			if (method != null)
				return ReflectionUtilCore.invokeStatic(method, object);
		}

		else if (classOf == Object.class) {
			// Good
		}

		else
			throw new SerializeFailedException("Does not know how to turn " + classOf + " into a serialized object from data: " + object);

		return (T) object;
	}

	/**
	 * Add a custom serializer to serialize objects into strings
	 *
	 * @param serializer
	 */
	public static void addCustomSerializer(Serializer serializer) {
		customSerializers.add(serializer);
	}

	/**
	 * A custom serializer for serializing objects into strings
	 */
	public interface Serializer {
		Object serialize(Mode mode, Object object);

		<T> T deserialize(Mode mode, final Class<T> classOf, Object object, final Object... parameters);
	}

	/**
	 * How should we de/serialize the objects in this class?
	 */
	public enum Mode {
		JSON,
		YAML
	}
}
