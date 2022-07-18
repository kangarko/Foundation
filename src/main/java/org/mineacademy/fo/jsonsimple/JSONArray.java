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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** JsonArray is a common non-thread safe data format for a collection of data. The contents of a JsonArray are only
 * validated as JSON values on serialization. Meaning all values added to a JsonArray must be recognized by the Jsoner
 * for it to be a true 'JsonArray', so it is really a JsonableArrayList that will serialize to a JsonArray if all of
 * its contents are valid JSON.
 * @author https://cliftonlabs.github.io/json-simple/
 * @since 2.0.0 */
public class JSONArray extends ArrayList<Object> implements Jsonable {
	/** The serialization version this class is compatible with. This value doesn't need to be incremented if and only
	 * if the only changes to occur were updating comments, updating javadocs, adding new fields to the class, changing
	 * the fields from static to non-static, or changing the fields from transient to non transient. All other changes
	 * require this number be incremented. */
	private static final long serialVersionUID = 1L;

	/** Instantiates an empty JsonArray. */
	public JSONArray() {
	}

	/** Instantiate a new JsonArray using ArrayList's constructor of the same type.
	 * @param collection represents the elements to produce the JsonArray with. */
	public JSONArray(final Collection<?> collection) {
		super(collection);
	}

	/** Calls add for the given collection of elements, but returns the JsonArray for chaining calls.
	 * @param collection represents the items to be appended to the JsonArray.
	 * @return the JsonArray to allow chaining calls.
	 * @see ArrayList#addAll(Collection)
	 * @since 3.1.0 for inline instantiation. */
	public JSONArray addAllChain(final Collection<?> collection) {
		this.addAll(collection);
		return this;
	}

	/** Calls add for the given index and collection, but returns the JsonArray for chaining calls.
	 * @param index represents what index the element is added to in the JsonArray.
	 * @param collection represents the item to be appended to the JsonArray.
	 * @return the JsonArray to allow chaining calls.
	 * @see ArrayList#addAll(int, Collection)
	 * @since 3.1.0 for inline instantiation. */
	public JSONArray addAllChain(final int index, final Collection<?> collection) {
		this.addAll(index, collection);
		return this;
	}

	/** Calls add for the given element, but returns the JsonArray for chaining calls.
	 * @param index represents what index the element is added to in the JsonArray.
	 * @param element represents the item to be appended to the JsonArray.
	 * @return the JsonArray to allow chaining calls.
	 * @see ArrayList#add(int, Object)
	 * @since 3.1.0 for inline instantiation. */
	public JSONArray addChain(final int index, final Object element) {
		this.add(index, element);
		return this;
	}

	/** Calls add for the given element, but returns the JsonArray for chaining calls.
	 * @param element represents the item to be appended to the JsonArray.
	 * @return the JsonArray to allow chaining calls.
	 * @see ArrayList#add(Object)
	 * @since 3.1.0 for inline instantiation. */
	public JSONArray addChain(final Object element) {
		this.add(element);
		return this;
	}

	/** A convenience method that assumes every element of the JsonArray is castable to T before adding it to a
	 * collection of Ts.
	 * @param <T> represents the type that all of the elements of the JsonArray should be cast to and the type the
	 *        collection will contain.
	 * @param destination represents where all of the elements of the JsonArray are added to after being cast to the
	 *        generic type
	 *        provided.
	 * @throws ClassCastException if the unchecked cast of an element to T fails. */

	public <T> void asCollection(final Collection<T> destination) {
		for (final Object o : this)
			destination.add((T) o);
	}

	/**
	 * If the value is a {@linkplain JSONObject} already, it will be casted and returned.
	 * If the value is a {@linkplain Map}, it will be wrapped in a {@linkplain JSONObject}. The wrapped {@linkplain Map} will be returned.
	 * In any other case this method returns {@code null}.
	 *
	 * @param index key of the value
	 * @return a {@linkplain JSONObject} or {@code null}
	 */
	public JSONObject getObject(final int index) {
		final Object value = this.get(index);

		if (value != null)
			if (value instanceof JSONObject)
				return (JSONObject) value;

			else if (value instanceof Map)
				return new JSONObject((Map<String, ?>) value);

		return null;
	}

	/** A convenience method that assumes there is a BigDecimal, Number, or String at the given index. If a Number or
	 * String is there it is used to construct a new BigDecimal.
	 * @param index representing where the value is expected to be at.
	 * @return the value stored at the key or the default provided if the key doesn't exist.
	 * @throws ClassCastException if there was a value but didn't match the assumed return types.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal.
	 * @see BigDecimal
	 * @see Number#doubleValue() */
	public BigDecimal getBigDecimal(final int index) {
		Object returnable = this.get(index);
		if (returnable instanceof BigDecimal) {
			/* Success there was a BigDecimal. */
		} else if (returnable instanceof Number)
			/* A number can be used to construct a BigDecimal. */
			returnable = new BigDecimal(returnable.toString());
		else if (returnable instanceof String)
			/* A number can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return (BigDecimal) returnable;
	}

	/** A convenience method that assumes there is a Boolean or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a boolean.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray. */
	public Boolean getBoolean(final int index) {
		Object returnable = this.get(index);
		if (returnable instanceof String)
			returnable = Boolean.valueOf((String) returnable);
		return (Boolean) returnable;
	}

	/** A convenience method that assumes there is a Number or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a byte.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Number */
	public Byte getByte(final int index) {
		Object returnable = this.get(index);
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

	public JSONArray getArray(final int index) {
		final Object collection = this.get(index);

		if (collection instanceof JSONArray)
			return (JSONArray) collection;

		else if (collection instanceof Collection)
			return new JSONArray((Collection<?>) collection);

		return null;
	}

	/** A convenience method that assumes there is a Number or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a double.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Number */
	public Double getDouble(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).doubleValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a float.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Number */
	public Float getFloat(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).floatValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a int.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Number */
	public Integer getInteger(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).intValue();
	}

	/** A convenience method that assumes there is a Number or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a long.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Number */
	public Long getLong(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).longValue();
	}

	/** A convenience method that assumes there is a Map value at the given index.
	 * @param <T> the kind of map to expect at the index. Note unless manually added, Map values will be a JsonObject.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a Map.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Map */

	public <T extends Map<?, ?>> T getMap(final int index) {
		/* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
		return (T) this.get(index);
	}

	/** A convenience method that assumes there is a Number or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a short.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
	 *         represents the double or float Infinity or NaN.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
	 * @see Number */
	public Short getShort(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).shortValue();
	}

	/** A convenience method that assumes there is a Boolean, Number, or String value at the given index.
	 * @param index represents where the value is expected to be at.
	 * @return the value at the index provided cast to a String.
	 * @throws ClassCastException if there was a value but didn't match the assumed return type.
	 * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray. */
	public String getString(final int index) {
		Object returnable = this.get(index);
		if (returnable instanceof Boolean)
			returnable = returnable.toString();
		else if (returnable instanceof Number)
			returnable = returnable.toString();
		return (String) returnable;
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
		boolean isFirstElement = true;
		final Iterator<Object> elements = this.iterator();
		writable.write('[');
		while (elements.hasNext()) {
			if (isFirstElement)
				isFirstElement = false;
			else
				writable.write(',');
			JSONParser.serialize(elements.next(), writable);
		}
		writable.write(']');
	}

	/**
	 * Converts this {@linkplain JSONArray} into an array of {@linkplain String}s using the {@link #getString(int)} method.
	 *
	 * @return an array of {@linkplain String}s
	 * @since 1.0.0
	 */
	public String[] toStringArray() {
		final String[] array = new String[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getString(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} into an array of {@linkplain JSONObject}s using the {@link #getObject(int)} method.
	 *
	 * @return an array of {@linkplain JSONObject}s
	 * @since 1.0.0
	 */
	public JSONObject[] toObjectArray() {

		final JSONObject[] array = new JSONObject[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getObject(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} into an array of {@linkplain JSONArray}s using the {@link #getArray(int)} method.
	 *
	 * @return an array of {@linkplain JSONArray}s.
	 * @since 1.0.0
	 */
	public JSONArray[] toArrayArray() {

		final JSONArray[] array = new JSONArray[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getArray(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code byte} array.
	 *
	 * @return a primitive {@code byte} array
	 * @since 2.0.0
	 */
	public byte[] toPrimitiveByteArray() {

		final byte[] array = new byte[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getByte(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code short} array.
	 *
	 * @return a primitive {@code short} array
	 * @since 2.0.0
	 */
	public short[] toPrimitiveShortArray() {

		final short[] array = new short[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getShort(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code int} array.
	 *
	 * @return a primitive {@code int} array
	 * @since 2.0.0
	 */
	public int[] toPrimitiveIntArray() {

		final int[] array = new int[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getInteger(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code long} array.
	 *
	 * @return a primitive {@code long} array
	 * @since 2.0.0
	 */
	public long[] toPrimitiveLongArray() {

		final long[] array = new long[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getLong(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code float} array.
	 *
	 * @return a primitive {@code float} array
	 * @since 2.0.0
	 */
	public float[] toPrimitiveFloatArray() {

		final float[] array = new float[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getFloat(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code double} array.
	 *
	 * @return a primitive {@code double} array
	 * @since 2.0.0
	 */
	public double[] toPrimitiveDoubleArray() {

		final double[] array = new double[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getDouble(index);

		return array;
	}

	/**
	 * Converts this {@linkplain JSONArray} to a primitive {@code boolean} array.
	 *
	 * @return a primitive {@code boolean} array
	 * @since 2.0.0
	 */
	public boolean[] toPrimitiveBooleanArray() {

		final boolean[] array = new boolean[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getBoolean(index);

		return array;
	}
}
