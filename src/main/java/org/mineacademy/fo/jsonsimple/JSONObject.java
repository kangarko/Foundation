/* Copyright 2016-2017 Clifton Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package org.mineacademy.fo.jsonsimple;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** JsonObject is a common non-thread safe data format for string to data mappings. The contents of a JsonObject are
 * only validated as JSON values on serialization. Meaning all values added to a JsonObject must be recognized by the
 * Jsoner for it to be a true 'JsonObject', so it is really a JsonableHashMap that will serialize to a JsonObject if all
 * of its contents are valid JSON.
 * @author https://cliftonlabs.github.io/json-simple/
 * @since 2.0.0 */
public class JSONObject extends HashMap<String, Object> implements Jsonable {
	/** The serialization version this class is compatible with. This value doesn't need to be incremented if and only
	 * if the only changes to occur were updating comments, updating javadocs, adding new fields to the class, changing
	 * the fields from static to non-static, or changing the fields from transient to non transient. All other changes
	 * require this number be incremented. */
	private static final long serialVersionUID = 2L;

	/** Instantiates an empty JsonObject. */
	public JSONObject() {
	}

	/** Instantiate a new JsonObject by accepting a map's entries, which could lead to de/serialization issues of the
	 * resulting JsonObject since the entry values aren't validated as JSON values.
	 * @param map represents the mappings to produce the JsonObject with. */
	public JSONObject(final Map<String, ?> map) {
		super(map);
	}

	/**
	 * If the value is a {@linkplain JSONObject} already, it will be casted and returned.
	 * If the value is a {@linkplain Map}, it will be wrapped in a {@linkplain JSONObject}. The wrapped {@linkplain Map} will be returned.
	 * In any other case this method returns {@code null}.
	 *
	 * @param key key of the value
	 * @return a {@linkplain JSONObject} or {@code null}
	 */
	public JSONObject getObject(String key) {
		final Object value = this.get(key);

		if (value != null)
			if (value instanceof JSONObject)
				return (JSONObject) value;

			else if (value instanceof Map)
				return new JSONObject((Map<String, ?>) value);

		return null;
	}

	/** A convenience method that assumes there is a BigDecimal, Number, or String at the given key. If a Number is
	 * there its Number#toString() is used to construct a new BigDecimal(String). If a String is there it is used to
	 * construct a new BigDecimal(String).
	 * @param key representing where the value ought to be paired with.
	 * @return a BigDecimal representing the value paired with the key.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see BigDecimal
	 * @see Number#toString()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public BigDecimal getBigDecimal(final String key) {
		Object returnable = this.get(key);
		if (returnable instanceof BigDecimal) {
			/* Success there was a BigDecimal or it defaulted. */
		} else if (returnable instanceof Number)
			/* A number can be used to construct a BigDecimal */
			returnable = new BigDecimal(returnable.toString());
		else if (returnable instanceof String)
			/* A number can be used to construct a BigDecimal */
			returnable = new BigDecimal((String) returnable);
		return (BigDecimal) returnable;
	}

	/** A convenience method that assumes there is a BigDecimal, Number, or String at the given key. If a Number is
	 * there its Number#toString() is used to construct a new BigDecimal(String). If a String is there it is used to
	 * construct a new BigDecimal(String).
	 * @param key representing where the value ought to be paired with.
	 * @return a BigDecimal representing the value paired with the key or JsonKey#getValue() if the key isn't present.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see BigDecimal
	 * @see Number#toString()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public BigDecimal getBigDecimalOrDefault(final String key, final BigDecimal def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;

		if (returnable instanceof BigDecimal) {
			/* Success there was a BigDecimal or it defaulted. */
		} else if (returnable instanceof Number)
			/* A number can be used to construct a BigDecimal */
			returnable = new BigDecimal(returnable.toString());
		else if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal */
			returnable = new BigDecimal((String) returnable);
		return (BigDecimal) returnable;
	}

	/** A convenience method that assumes there is a Boolean or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Boolean representing the value paired with the key.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Boolean getBoolean(final String key) {
		Object returnable = this.get(key);
		if (returnable instanceof String)
			returnable = Boolean.valueOf((String) returnable);
		return (Boolean) returnable;
	}

	/** A convenience method that assumes there is a Boolean or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Boolean representing the value paired with the key or JsonKey#getValue() if the key isn't present.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Boolean getBooleanOrDefault(final String key, final boolean def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable instanceof String)
			returnable = Boolean.valueOf((String) returnable);
		return (Boolean) returnable;
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Byte representing the value paired with the key (which may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#byteValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Byte getByte(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).byteValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Byte representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
	 *         may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#byteValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Byte getByteOrDefault(final String key, final byte def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).byteValue();
	}

	/** A convenience method that assumes there is a Collection at the given key.
	 * @param <T> the kind of collection to expect at the key. Note unless manually added, collection values will be a
	 *        JsonArray.
	 * @param key representing where the value ought to be paired with.
	 * @return a Collection representing the value paired with the key.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */

	public JSONArray getArray(final String key) {
		final Object collection = this.get(key);

		if (collection instanceof JSONArray)
			return (JSONArray) collection;

		else if (collection instanceof Collection)
			return new JSONArray((Collection<?>) collection);

		return null;
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Double representing the value paired with the key (which may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#doubleValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Double getDouble(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).doubleValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Double representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
	 *         may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#doubleValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Double getDoubleOrDefault(final String key, final double def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).doubleValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return an Integer representing the value paired with the key (which may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#intValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Integer getInteger(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).intValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return an Integer representing the value paired with the key or JsonKey#getValue() if the key isn't present
	 *         (which may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#intValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Integer getIntegerOrDefault(final String key, final int def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).intValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Long representing the value paired with the key (which may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#longValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Long getLong(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).longValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a Long representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
	 *         may involve rounding or truncation).
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @see Number#longValue()
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public Long getLongOrDefault(final String key, final long def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).longValue();
	}

	/** A convenience method that assumes there is a Map at the given key.
	 * @param <T> the kind of map to expect at the key. Note unless manually added, Map values will be a JsonObject.
	 * @param key representing where the value ought to be paired with.
	 * @return a Map representing the value paired with the key.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */

	public <T extends Map<?, ?>> T getMap(final String key) {
		/* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
		return (T) this.get(key);
	}

	/** A convenience method that assumes there is a Map at the given key.
	 * @param <T> the kind of map to expect at the key. Note unless manually added, Map values will be a JsonObject.
	 * @param key representing where the value ought to be paired with.
	 * @return a Map representing the value paired with the key or JsonKey#getValue() if the key isn't present.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */

	public <T extends Map<?, ?>> T getMapOrDefault(final String key, final T def) {
		/* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		return (T) returnable;
	}

	/** A convenience method that assumes there is a Boolean, Number, or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a String representing the value paired with the key.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public String getString(final String key) {
		Object returnable = this.get(key);
		if (returnable instanceof Boolean)
			returnable = returnable.toString();
		else if (returnable instanceof Number)
			returnable = returnable.toString();
		return (String) returnable;
	}

	/** A convenience method that assumes there is a Boolean, Number, or String value at the given key.
	 * @param key representing where the value ought to be paired with.
	 * @return a String representing the value paired with the key or JsonKey#getValue() if the key isn't present.
	 * @throws ClassCastException if the value didn't match the assumed return type.
	 *
	 * @since 2.3.0 to utilize JsonKey */
	public String getStringOrDefault(final String key, final String def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable instanceof Boolean)
			returnable = returnable.toString();
		else if (returnable instanceof Number)
			returnable = returnable.toString();
		return (String) returnable;
	}

	/** Calls putAll for the given map, but returns the JsonObject for chaining calls.
	 * @param map represents the map to be copied into the JsonObject.
	 * @return the JsonObject to allow chaining calls.
	 * @see Map#putAll(Map)
	 * @since 3.1.0 for inline instantiation. */
	public JSONObject putAllChain(final Map<String, Object> map) {
		this.putAll(map);
		return this;
	}

	/** Calls put for the given key and value, but returns the JsonObject for chaining calls.
	 * @param key represents the value's association in the map.
	 * @param value represents the key's association in the map.
	 * @return the JsonObject to allow chaining calls.
	 * @see Map#put(Object, Object)
	 * @since 3.1.0 for inline instantiation. */
	public JSONObject putChain(final String key, final Object value) {
		this.put(key, value);
		return this;
	}

	/** Convenience method that calls remove for the given key.
	 * @param key represents the value's association in the map.
	 * @return an object representing the removed value or null if there wasn't one.
	 * @since 3.1.1 to use JsonKey instead of calling JsonKey#getKey() each time.
	 * @see Map#remove(Object) */
	public Object remove(final String key) {
		return this.remove(key);
	}

	/** Convenience method that calls remove for the given key and value.
	 * @param key represents the value's association in the map.
	 * @param value represents the expected value at the given key.
	 * @return a boolean, which is true if the value was removed. It is false otherwise.
	 * @since 3.1.1 to use JsonKey instead of calling JsonKey#getKey() each time.
	 * @see Map#remove(Object, Object) */
	public boolean remove(final String key, final Object value) {
		return this.remove(key, value);
	}

	/** Ensures the given keys are present.
	 * @param keys represents the keys that must be present.
	 * @throws NoSuchElementException if any of the given keys are missing.
	 * @since 2.3.0 to ensure critical keys are in the JsonObject. */
	public void requireKeys(final String... keys) {
		/* Track all of the missing keys. */
		final Set<String> missing = new HashSet<>();
		for (final String subkey : keys)
			if (!this.containsKey(subkey))
				missing.add(subkey);
		if (!missing.isEmpty()) {
			/* Report any missing keys in the exception. */
			final StringBuilder sb = new StringBuilder();
			for (final String subkey : missing)
				sb.append(subkey).append(", ");
			sb.setLength(sb.length() - 2);
			final String s = missing.size() > 1 ? "s" : "";
			throw new NoSuchElementException("A JsonObject is missing required key" + s + ": " + sb.toString());
		}
	}

	/* (non-Javadoc)
	 * @see org.json.simple.Jsonable#toJson() */
	@Override
	public String toJson() {
		final StringWriter writable = new StringWriter();
		try {
			this.toJson(writable);
		} catch (final IOException caught) {
			/* See java.io.StringWriter. */
		}
		return writable.toString();
	}

	/* (non-Javadoc)
	 * @see org.json.simple.Jsonable#toJson(java.io.Writer) */
	@Override
	public void toJson(final Writer writable) throws IOException {
		/* Writes the map in JSON object format. */
		boolean isFirstEntry = true;
		writable.write('{');
		for (final Entry<String, Object> entry : this.entrySet()) {
			if (isFirstEntry)
				isFirstEntry = false;
			else
				writable.write(',');
			JSONParser.serialize(entry.getKey(), writable);
			writable.write(':');
			JSONParser.serialize(entry.getValue(), writable);
		}
		writable.write('}');
	}

	/**
	 * @see #toJson()
	 */
	@Override
	public String toString() {
		return this.toJson();
	}
}
