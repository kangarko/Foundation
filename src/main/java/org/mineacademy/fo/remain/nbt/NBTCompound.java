package org.mineacademy.fo.remain.nbt;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

/**
 * Base class representing NMS Compounds. For a standalone implementation check
 * {@link NBTContainer}
 *
 * @author tr7zw
 */
public class NBTCompound {

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();

	private final String compundName;
	private final NBTCompound parent;

	protected NBTCompound(final NBTCompound owner, final String name) {
		this.compundName = name;
		this.parent = owner;
	}

	Lock getReadLock() {
		return readLock;
	}

	Lock getWriteLock() {
		return writeLock;
	}

	protected void saveCompound() {
		if (parent != null)
			parent.saveCompound();
	}

	/**
	 * @return The Compound name
	 */
	public String getName() {
		return compundName;
	}

	/**
	 * @return The NMS Compound behind this Object
	 */
	public Object getCompound() {
		return parent.getCompound();
	}

	protected void setCompound(final Object compound) {
		parent.setCompound(compound);
	}

	/**
	 * @return The parent Compound
	 */
	public NBTCompound getParent() {
		return parent;
	}

	/**
	 * Merges all data from comp into this compound. This is done in one action, so
	 * it also works with Tiles/Entities
	 *
	 * @param comp
	 */
	void mergeCompound(final NBTCompound comp) {
		try {
			writeLock.lock();
			NBTReflectionUtil.mergeOtherNBTCompound(this, comp);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setString(final String key, final String value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_STRING, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public String getString(final String key) {
		try {
			readLock.lock();
			return (String) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_STRING, key);
		} finally {
			readLock.unlock();
		}
	}

	protected String getContent(final String key) {
		return NBTReflectionUtil.getContent(this, key);
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setInteger(final String key, final Integer value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_INT, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Integer getInteger(final String key) {
		try {
			readLock.lock();
			return (Integer) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_INT, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setDouble(final String key, final Double value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_DOUBLE, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Double getDouble(final String key) {
		try {
			readLock.lock();
			return (Double) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_DOUBLE, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setByte(final String key, final Byte value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_BYTE, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Byte getByte(final String key) {
		try {
			readLock.lock();
			return (Byte) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_BYTE, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setShort(final String key, final Short value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_SHORT, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Short getShort(final String key) {
		try {
			readLock.lock();
			return (Short) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_SHORT, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setLong(final String key, final Long value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_LONG, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Long getLong(final String key) {
		try {
			readLock.lock();
			return (Long) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_LONG, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setFloat(final String key, final Float value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_FLOAT, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Float getFloat(final String key) {
		try {
			readLock.lock();
			return (Float) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_FLOAT, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setByteArray(final String key, final byte[] value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_BYTEARRAY, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public byte[] getByteArray(final String key) {
		try {
			readLock.lock();
			return (byte[]) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_BYTEARRAY, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setIntArray(final String key, final int[] value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_INTARRAY, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public int[] getIntArray(final String key) {
		try {
			readLock.lock();
			return (int[]) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_INTARRAY, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setBoolean(final String key, final Boolean value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_BOOLEAN, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	protected void set(final String key, final Object val) {
		NBTReflectionUtil.set(this, key, val);
		saveCompound();
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public Boolean getBoolean(final String key) {
		try {
			readLock.lock();
			return (Boolean) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_BOOLEAN, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Uses Gson to store an {@link Serializable} Object
	 *
	 * @param key
	 * @param value
	 */
	public void setObject(final String key, final Object value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setObject(this, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Uses Gson to retrieve a stored Object
	 *
	 * @param key
	 * @param type Class of the Object
	 * @return The created Object or null if empty
	 */
	public <T> T getObject(final String key, final Class<T> type) {
		try {
			readLock.lock();
			return NBTReflectionUtil.getObject(this, key, type);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Save an ItemStack as a compound under a given key
	 *
	 * @param key
	 * @param item
	 */
	public void setItemStack(final String key, final ItemStack item) {
		try {
			writeLock.lock();
			removeKey(key);
			addCompound(key).mergeCompound(NBTItem.convertItemtoNBT(item));
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Get an ItemStack that was saved at the given key
	 *
	 * @param key
	 * @return
	 */
	public ItemStack getItemStack(final String key) {
		try {
			readLock.lock();
			final NBTCompound comp = getCompound(key);
			return NBTItem.convertNBTtoItem(comp);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	public void setUUID(final String key, final UUID value) {
		try {
			writeLock.lock();
			NBTReflectionUtil.setData(this, WrapperReflection.COMPOUND_SET_UUID, key, value);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	public UUID getUUID(final String key) {
		try {
			readLock.lock();
			return (UUID) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_UUID, key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * @param key
	 * @return True if the key is set
	 */
	public Boolean hasKey(final String key) {
		try {
			readLock.lock();
			final Boolean b = (Boolean) NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_HAS_KEY, key);

			if (b == null)
				return false;

			return b;
		} catch (final Throwable t) {
			if (MinecraftVersion.atLeast(V.v1_8))
				t.printStackTrace();

			return false;

		} finally {
			readLock.unlock();
		}
	}

	/**
	 * @param key Deletes the given Key
	 */
	public void removeKey(final String key) {
		try {
			writeLock.lock();
			NBTReflectionUtil.remove(this, key);
			saveCompound();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @return Set of all stored Keys
	 */
	public Set<String> getKeys() {
		try {
			readLock.lock();
			return NBTReflectionUtil.getKeys(this);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Creates a subCompound
	 *
	 * @param name Key to use
	 * @return The subCompound Object
	 */
	public NBTCompound addCompound(final String name) {
		try {
			writeLock.lock();
			if (getType(name) == NBTType.NBTTagCompound)
				return getCompound(name);
			NBTReflectionUtil.addNBTTagCompound(this, name);
			final NBTCompound comp = getCompound(name);
			if (comp == null)
				throw new NbtApiException("Error while adding Compound, got null!");
			saveCompound();
			return comp;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The Compound instance or null
	 */
	public NBTCompound getCompound(final String name) {
		try {
			readLock.lock();
			if (getType(name) != NBTType.NBTTagCompound)
				return null;
			final NBTCompound next = new NBTCompound(this, name);
			if (NBTReflectionUtil.valideCompound(next))
				return next;
			return null;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved String List
	 */
	public NBTList<String> getStringList(final String name) {
		try {
			writeLock.lock();
			final NBTList<String> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagString, String.class);
			saveCompound();
			return list;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Integer List
	 */
	public NBTList<Integer> getIntegerList(final String name) {
		try {
			writeLock.lock();
			final NBTList<Integer> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagInt, Integer.class);
			saveCompound();
			return list;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Float List
	 */
	public NBTList<Float> getFloatList(final String name) {
		try {
			writeLock.lock();
			final NBTList<Float> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagFloat, Float.class);
			saveCompound();
			return list;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Double List
	 */
	public NBTList<Double> getDoubleList(final String name) {
		try {
			writeLock.lock();
			final NBTList<Double> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagDouble, Double.class);
			saveCompound();
			return list;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Long List
	 */
	public NBTList<Long> getLongList(final String name) {
		try {
			writeLock.lock();
			final NBTList<Long> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagLong, Long.class);
			saveCompound();
			return list;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Compound List
	 */
	public NBTCompoundList getCompoundList(final String name) {
		try {
			writeLock.lock();
			final NBTCompoundList list = (NBTCompoundList) NBTReflectionUtil.getList(this, name, NBTType.NBTTagCompound, NBTListCompound.class);
			saveCompound();
			return list;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The type of the given stored key or null
	 */
	public NBTType getType(final String name) {
		try {
			readLock.lock();
			if (WrapperVersion.getVersion() == WrapperVersion.MC1_7_R4)
				return null;
			final Object o = NBTReflectionUtil.getData(this, WrapperReflection.COMPOUND_GET_TYPE, name);
			if (o == null)
				return null;
			return NBTType.valueOf((byte) o);
		} finally {
			readLock.unlock();
		}
	}

	public void writeCompound(final OutputStream stream) {
		try {
			writeLock.lock();
			NBTReflectionUtil.writeApiNBT(this, stream);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public String toString() {
		/*
		 * StringBuilder result = new StringBuilder(); for (String key : getKeys()) {
		 * result.append(toString(key)); } return result.toString();
		 */
		return asNBTString();
	}

	/**
	 * @param key
	 * @return A string representation of the given key
	 * @deprecated Just use toString()
	 */
	@Deprecated
	public String toString(final String key) {
		/*
		 * StringBuilder result = new StringBuilder(); NBTCompound compound = this;
		 * while (compound.getParent() != null) { result.append("   "); compound =
		 * compound.getParent(); } if (this.getType(key) == NBTType.NBTTagCompound) {
		 * return this.getCompound(key).toString(); } else { return result + "-" + key +
		 * ": " + getContent(key) + System.lineSeparator(); }
		 */
		return asNBTString();
	}

	/**
	 * @return A {@link String} representation of the NBT in Mojang JSON. This is different from normal JSON!
	 * @deprecated Just use toString()
	 */
	@Deprecated
	public String asNBTString() {
		try {
			readLock.lock();
			final Object comp = NBTReflectionUtil.gettoCompount(getCompound(), this);
			if (comp == null)
				return "{}";
			return comp.toString();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Uses the nbt-string to match this compound with another object. This allows
	 * two "technically" different Compounds to match, if they have the same content
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return toString().equals(obj.toString());
	}

}
