package org.mineacademy.fo;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.ConfigStringSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.settings.ConfigSection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 *
 * This is a platform-neutral class, which is extended by "SerializeUtil" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SerializeUtilCore {

	/**
	 * A list of custom serializers.
	 */
	private static final List<Serializer> serializers = new ArrayList<>();

	/**
	 * Converts the given object into something you can safely save in file as a String.
	 *
	 * @param language determines the file that the object originated from, if unsure set to YAM.
	 * @param object
	 * @return
	 */
	public static Object serialize(Language language, Object object) {

		if (object == null || ValidCore.isPrimitiveWrapper(object) || object instanceof String)
			return object;

		for (final Serializer serializer : serializers) {
			final Object result = serializer.serialize(language, object);

			if (result != null)
				return result;
		}

		if (object instanceof Map || object instanceof SerializedMap) {
			final Map<?, ?> oldMap = object instanceof Map ? (Map<?, ?>) object : ((SerializedMap) object).asMap();

			if (language == Language.JSON) {
				final JsonObject json = new JsonObject();

				for (final Map.Entry<?, ?> entry : oldMap.entrySet()) {
					ValidCore.checkNotNull(entry.getKey(), "Map key cannot be null: " + oldMap);

					final Object key = serialize(language, entry.getKey());
					final Object value = serialize(language, entry.getValue());

					if (value == null)
						continue;

					if (!(key instanceof String) && !(key instanceof Number))
						throw new FoException("JSON Map requires keys that are String or a number, found " + key.getClass().getSimpleName() + " key: " + key + " with value '" + value + "'");

					if (value instanceof Boolean)
						json.addProperty(key.toString(), (Boolean) value);

					else if (value instanceof Character)
						json.addProperty(key.toString(), (Character) value);

					else if (value instanceof Number)
						json.addProperty(key.toString(), (Number) value);

					else if (value instanceof String)
						json.addProperty(key.toString(), (String) value);

					else if (value instanceof JsonElement)
						json.add(key.toString(), (JsonElement) value);

					else
						throw new FoException("JSON Map requires values that are String, primitive or JsonElement, found " + (value == null ? "null" : value.getClass().getSimpleName()) + ": " + value + " for key " + key + " in object " + object);
				}

				return json;
			}

			else {
				final Map<Object, Object> newMap = new LinkedHashMap<>();

				for (final Map.Entry<?, ?> entry : oldMap.entrySet())
					newMap.put(serialize(language, entry.getKey()), serialize(language, entry.getValue()));

				return newMap;
			}
		}

		else if (object instanceof Iterable || object.getClass().isArray() || object instanceof IsInList) {
			Iterable<Object> iterable = object instanceof Iterable ? (Iterable<Object>) object : object instanceof IsInList ? ((IsInList<Object>) object).getList() : null;

			// Support Object[] as well as primitive arrays
			if (object.getClass().isArray()) {
				final int length = Array.getLength(object);
				final Object[] iterableArray = new Object[length];

				for (int i = 0; i < length; i++)
					iterableArray[i] = Array.get(object, i);

				iterable = Arrays.asList(iterableArray);
			}

			if (language == Language.JSON) {
				final JsonArray jsonList = new JsonArray();

				for (final Object element : iterable) {
					if (element == null)
						jsonList.add(JsonNull.INSTANCE);

					if (element instanceof Boolean)
						jsonList.add(new JsonPrimitive((Boolean) element));

					else if (element instanceof Character)
						jsonList.add(new JsonPrimitive((Character) element));

					else if (element instanceof Number)
						jsonList.add(new JsonPrimitive((Number) element));

					else if (element instanceof String)
						jsonList.add(new JsonPrimitive((String) element));

					else
						jsonList.add(CommonCore.GSON.toJsonTree(serialize(Language.JSON, element)));
				}

				return jsonList;
			}

			else {
				final List<Object> list = new ArrayList<>();

				for (final Object element : iterable)
					list.add(serialize(language, element));

				return list;
			}
		}

		// Shortcuts for Java methods
		else if (object instanceof UUID)
			return object.toString();

		else if (object instanceof Color)
			return "#" + ((Color) object).getRGB();

		else if (object instanceof Pattern)
			return ((Pattern) object).pattern();

		else if (object instanceof BigDecimal) {
			final BigDecimal big = (BigDecimal) object;

			return big.toPlainString();
		}

		// Shortcut for our own classes
		else if (object instanceof ConfigSerializable)
			return serialize(language, ((ConfigSerializable) object).serialize());

		else if (object instanceof ConfigStringSerializable)
			return ((ConfigStringSerializable) object).serialize();

		else if (object instanceof ConfigSection)
			return serialize(language, ((ConfigSection) object).getValues(false));

		// Shortcut for third party
		else if (object instanceof Style) {
			final Map<String, Object> map = new HashMap<>();
			final Style style = (Style) object;

			if (style.color() != null)
				map.put("Color", style.color().asHexString());

			final List<String> decorations = new ArrayList<>();

			for (final Map.Entry<TextDecoration, State> entry : style.decorations().entrySet())
				if (entry.getValue() == State.TRUE)
					decorations.add(entry.getKey().name());

			map.put("Decorations", decorations);

			return map;
		}

		// Adjust enum serialization at the end
		else if (object instanceof Enum)
			return object.getClass().getSimpleName().equals("ChatColor") ? ((Enum<?>) object).name() : object.toString();

		// Prevent serialization of these
		else if (object instanceof SimpleComponent)
			throw new FoException("Serializing SimpleComponent is ambigious, if you want to serialize it literally, call SimpleComponent#serialize().toJson(), otherwise call SimpleComponent#toAdventureJson()");

		throw new FoException("Does not know how to serialize " + object.getClass() + "! Does it extends ConfigSerializable? Data: " + object);
	}

	/**
	 * Attempts to convert the given object saved in the given mode (i.e. in a .yml file) back
	 * into its Java class, i.e. a Location.
	 *
	 * @param <T>
	 * @param language
	 * @param classOf
	 * @param object
	 * @param parameters
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T deserialize(@NonNull Language language, @NonNull final Class<T> classOf, @NonNull Object object, final Object... parameters) {

		for (final Serializer serializer : serializers) {
			final Object result = serializer.deserialize(language, classOf, object, parameters);

			if (result != null)
				return (T) result;
		}

		if (classOf == String.class)
			object = object.toString();

		else if (classOf == Byte.class)
			object = Byte.parseByte(object.toString().replace("_", ""));

		else if (classOf == Short.class)
			object = Short.parseShort(object.toString().replace("_", ""));

		else if (classOf == Integer.class)
			object = Integer.parseInt(object.toString().replace("_", ""));

		else if (classOf == Long.class)
			object = Long.decode(object.toString().replace("_", ""));

		else if (classOf == Double.class)
			object = Double.parseDouble(object.toString().replace("_", ""));

		else if (classOf == Float.class)
			object = Float.parseFloat(object.toString().replace("_", ""));

		else if (classOf == Boolean.class)
			object = Boolean.parseBoolean(object.toString());

		else if (classOf == SerializedMap.class)
			object = SerializedMap.of(language, object);

		else if (classOf == UUID.class)
			object = UUID.fromString(object.toString());

		else if (classOf == SimpleComponent.class)
			throw new FoException("Deserializing SimpleComponent is ambigious, if you want to deserialize it literally from JSON, "
					+ "use SimpleComponent$deserialize(SerializedMap.of(Language.JSON, object.toString())), otherwise call SimpleComponent#fromMini");

		else if (classOf == Style.class) {
			final SerializedMap map = SerializedMap.of(object);
			Style.Builder style = Style.style();

			if (map.containsKey("Color"))
				style = style.color(TextColor.fromHexString(map.getString("Color")));

			if (map.containsKey("Decorations"))
				for (final String decoration : map.getStringList("Decorations"))
					style = style.decorate(TextDecoration.valueOf(decoration));

			object = style.build();
		}

		else if (Enum.class.isAssignableFrom(classOf)) {
			object = ReflectionUtil.lookupEnum((Class<Enum>) classOf, object.toString());

			if (object == null)
				return null;
		}

		else if (Color.class.isAssignableFrom(classOf))
			object = CompChatColor.fromString(object.toString()).getColor();

		else if (List.class.isAssignableFrom(classOf) && object instanceof List) {
			// Good

		} else if (Map.class.isAssignableFrom(classOf)) {
			if (object instanceof Map)
				return (T) object;

			if (language == Language.JSON)
				return (T) SerializedMap.of(language, object).asMap();

			throw new FoException("Does not know how to turn " + object.getClass().getSimpleName() + " into a Map! (Keep in mind we can only serialize into Map<String, Object>. Data: " + object);

		} else if (classOf.isArray()) {
			final Class<?> arrayType = classOf.getComponentType();
			T[] array;

			if (object instanceof List) {
				final List<?> rawList = (List<?>) object;
				array = (T[]) Array.newInstance(classOf.getComponentType(), rawList.size());

				for (int i = 0; i < rawList.size(); i++) {
					final Object element = rawList.get(i);

					array[i] = element == null ? null : (T) deserialize(language, arrayType, element, (Object[]) null);
				}
			}

			else {
				final Object[] rawArray = (Object[]) object;
				array = (T[]) Array.newInstance(classOf.getComponentType(), rawArray.length);

				for (int i = 0; i < array.length; i++)
					array[i] = rawArray[i] == null ? null : (T) deserialize(language, classOf.getComponentType(), rawArray[i], (Object[]) null);
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
				arguments.add(SerializedMap.of(language, object));
				Collections.addAll(arguments, parameters);

				// Find deserialize(SerializedMap, args[]) method
				final Method deserialize = ReflectionUtil.getMethod(classOf, "deserialize", argumentClasses.toArray(new Class[argumentClasses.size()]));

				ValidCore.checkNotNull(deserialize,
						"Expected " + classOf.getSimpleName() + " to have a public static deserialize(SerializedMap, " + CommonCore.join(argumentClasses) + ") method to deserialize: " + object + " when params were given: " + CommonCore.join(parameters));

				ValidCore.checkBoolean(argumentClasses.size() == arguments.size(),
						classOf.getSimpleName() + "#deserialize(SerializedMap, " + argumentClasses.size() + " args) expected, " + arguments.size() + " given to deserialize: " + object);

				return ReflectionUtil.invokeStatic(deserialize, arguments.toArray());
			}

			final Method deserialize = ReflectionUtil.getMethod(classOf, "deserialize", SerializedMap.class);

			if (deserialize != null)
				return ReflectionUtil.invokeStatic(deserialize, SerializedMap.of(language, object));

			throw new FoException("Unable to deserialize " + classOf.getSimpleName()
					+ ", please write 'public static deserialize(SerializedMap map) or deserialize(SerializedMap map, X arg1, Y arg2, etc.) method to deserialize: " + object);
		}

		else if (ConfigStringSerializable.class.isAssignableFrom(classOf)) {
			if (parameters != null && parameters.length > 0) {
				final List<Class<?>> argumentClasses = new ArrayList<>();
				final List<Object> arguments = new ArrayList<>();

				// Build parameters
				argumentClasses.add(String.class);

				for (final Object param : parameters) {
					ValidCore.checkNotNull(param, "Deserialize param cannot be null in " + classOf.getSimpleName() + ".fromString(). Got params: " + Arrays.toString(parameters));

					argumentClasses.add(param.getClass());
				}

				// Build parameter instances
				arguments.add(object.toString());

				Collections.addAll(arguments, parameters);

				// Find deserialize(SerializedMap, args[]) method
				final Method fromString = ReflectionUtil.getMethod(classOf, "fromString", argumentClasses.toArray(new Class[argumentClasses.size()]));

				ValidCore.checkNotNull(fromString,
						"Expected " + classOf.getSimpleName() + " to have a public static fromString(String, " + CommonCore.join(argumentClasses) + ") method to deserialize: " + object + " when params were given: " + CommonCore.join(parameters));

				ValidCore.checkBoolean(argumentClasses.size() == arguments.size(),
						classOf.getSimpleName() + "#fromString(String, " + argumentClasses.size() + " args) expected, " + arguments.size() + " given to deserialize: " + object);

				return ReflectionUtil.invokeStatic(fromString, arguments.toArray());
			}

			final Method fromString = ReflectionUtil.getMethod(classOf, "fromString", String.class);

			if (fromString != null)
				return ReflectionUtil.invokeStatic(fromString, object.toString());

			throw new FoException("Unable to deserialize " + classOf.getSimpleName()
					+ ", please write 'public static fromString(String) or fromString(String string, X arg1, Y arg2, etc.) method to deserialize: " + object);
		}

		// Search for "getByName" method used by us or some Bukkit classes such as Enchantment
		else if (object instanceof String) {
			final Method method = ReflectionUtil.getMethod(classOf, "getByName", String.class);

			if (method != null)
				return ReflectionUtil.invokeStatic(method, object);
		}

		else if (ConfigSection.class.isAssignableFrom(classOf) || classOf == Object.class) {
			// Good
		}

		else
			throw new FoException("Does not know how to turn " + classOf + " into a serialized object from data: " + object);

		return (T) object;
	}

	/**
	 * Adds a custom serializer for serializing objects into strings.
	 *
	 * @param <T>
	 * @param handler
	 */
	public static <T> void addSerializer(Serializer handler) {
		serializers.add(handler);
	}

	/**
	 * A custom serializer for serializing objects into strings.
	 */
	public interface Serializer {

		/**
		 * Turn the given object into something we can save inside the given config language, for most cases this is a String.
		 *
		 * @param language
		 * @param object
		 * @return
		 */
		Object serialize(Language language, Object object);

		/**
		 * Turn the given object back into its original form, this is the opposite of {@link #serialize(Language, Object)}.
		 *
		 * @param <T>
		 * @param language
		 * @param classOf
		 * @param object
		 * @param parameters
		 * @return
		 */
		<T> T deserialize(@NonNull Language language, @NonNull final Class<T> classOf, @NonNull Object object, final Object... parameters);
	}

	/**
	 * The markup language the objects should be serialized to or deserialized from
	 */
	public enum Language {
		JSON,
		YAML
	}
}
