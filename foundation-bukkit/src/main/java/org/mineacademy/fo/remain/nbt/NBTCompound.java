package org.mineacademy.fo.remain.nbt;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.remain.nbt.PathUtil.PathSegment;

/**
 * Base class representing NMS Compounds. For a standalone implementation check
 * {@link NBTContainer}
 *
 * @author tr7zw
 *
 */
public class NBTCompound implements ReadWriteNBT {

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = this.readWriteLock.readLock();
	private final Lock writeLock = this.readWriteLock.writeLock();

	private final String compundName;
	private final NBTCompound parent;
	private final boolean readOnly;
	private Object readOnlyCache;

	protected NBTCompound(NBTCompound owner, String name) {
		this(owner, name, false);
	}

	protected NBTCompound(NBTCompound owner, String name, boolean readOnly) {
		this.compundName = name;
		this.parent = owner;
		this.readOnly = readOnly;
	}

	protected Lock getReadLock() {
		return this.readLock;
	}

	protected Lock getWriteLock() {
		return this.writeLock;
	}

	protected void saveCompound() {
		if (this.parent != null)
			this.parent.saveCompound();
	}

	protected void setResolvedObject(Object object) {
		if (this.isClosed())
			throw new NbtApiException("Tried using closed NBT data!");
		if (this.readOnly)
			this.readOnlyCache = object;
	}

	protected void setClosed() {
		if (this.parent != null)
			this.parent.setClosed();
	}

	protected boolean isClosed() {
		if (this.parent != null)
			return this.parent.isClosed();
		return false;
	}

	protected boolean isReadOnly() {
		return this.readOnly;
	}

	protected Object getResolvedObject() {
		if (this.isClosed())
			throw new NbtApiException("Tried using closed NBT data!");
		if (this.readOnlyCache != null)
			return this.readOnlyCache;
		final Object rootnbttag = this.getCompound();
		if (rootnbttag == null)
			return null;
		if (!NBTReflectionUtil.valideCompound(this))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = NBTReflectionUtil.getToCompount(rootnbttag, this);
		if (this.readOnly)
			this.readOnlyCache = workingtag;
		return workingtag;
	}

	/**
	 * @return The Compound name
	 */
	public String getName() {
		return this.compundName;
	}

	/**
	 * @return The NMS Compound behind this Object
	 */
	public Object getCompound() {
		return this.parent.getCompound();
	}

	protected void setCompound(Object compound) {
		this.parent.setCompound(compound);
	}

	/**
	 * @return The parent Compound
	 */
	public NBTCompound getParent() {
		return this.parent;
	}

	/**
	 * Merges all data from comp into this compound. This is done in one action, so
	 * it also works with Tiles/Entities
	 *
	 * @param comp
	 */
	public void mergeCompound(NBTCompound comp) {
		if (comp == null)
			return;
		try {
			this.writeLock.lock();
			NBTReflectionUtil.mergeOtherNBTCompound(this, comp);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public void mergeCompound(ReadableNBT comp) {
		if (comp instanceof NBTCompound)
			this.mergeCompound((NBTCompound) comp);
		else
			throw new NbtApiException("Unknown NBT object: " + comp);
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setString(String key, String value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_STRING, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public String getString(String key) {
		try {
			this.readLock.lock();
			return (String) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_STRING, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setInteger(String key, Integer value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_INT, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Integer getInteger(String key) {
		try {
			this.readLock.lock();
			return (Integer) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_INT, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setDouble(String key, Double value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_DOUBLE, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Double getDouble(String key) {
		try {
			this.readLock.lock();
			return (Double) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_DOUBLE, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setByte(String key, Byte value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_BYTE, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Byte getByte(String key) {
		try {
			this.readLock.lock();
			return (Byte) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_BYTE, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setShort(String key, Short value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_SHORT, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Short getShort(String key) {
		try {
			this.readLock.lock();
			return (Short) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_SHORT, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setLong(String key, Long value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_LONG, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Long getLong(String key) {
		try {
			this.readLock.lock();
			return (Long) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_LONG, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setFloat(String key, Float value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_FLOAT, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Float getFloat(String key) {
		try {
			this.readLock.lock();
			return (Float) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_FLOAT, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setByteArray(String key, byte[] value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_BYTEARRAY, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public byte[] getByteArray(String key) {
		try {
			this.readLock.lock();
			return (byte[]) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_BYTEARRAY, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setIntArray(String key, int[] value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_INTARRAY, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public int[] getIntArray(String key) {
		try {
			this.readLock.lock();
			return (int[]) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_INTARRAY, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * Requires at least 1.16
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setLongArray(String key, long[] value) {
		ValidCore.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_16), "NBTCompount#setLongArray() is only available in 1.16+");

		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_LONGARRAY, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * Requires at least 1.16
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public long[] getLongArray(String key) {
		ValidCore.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_16), "NBTCompount#getLongArray() is only available in 1.16+");

		try {
			this.readLock.lock();
			return (long[]) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_LONGARRAY, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setBoolean(String key, Boolean value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_BOOLEAN, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	protected void set(String key, Object val) {
		NBTReflectionUtil.set(this, key, val);
		this.saveCompound();
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public Boolean getBoolean(String key) {
		try {
			this.readLock.lock();
			return (Boolean) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_BOOLEAN, key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Uses Gson to store an {@link Serializable} Object. Deprecated to clarify that
	 * it's probably missused. Preferably do the serializing yourself.
	 *
	 * @param key
	 * @param value
	 */
	@Deprecated
	public void setObject(String key, Object value) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.setObject(this, key, value);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Uses Gson to retrieve a stored Object Deprecated to clarify that it's
	 * probably missused. Preferably do the serializing yourself.
	 * @param <T>
	 * @param key
	 * @param type Class of the Object
	 * @return The created Object or null if empty
	 */
	@Deprecated
	public <T> T getObject(String key, Class<T> type) {
		try {
			this.readLock.lock();
			return NBTReflectionUtil.getObject(this, key, type);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Save an ItemStack as a compound under a given key
	 *
	 * @param key
	 * @param item
	 */
	@Override
	public void setItemStack(String key, ItemStack item) {
		try {
			this.writeLock.lock();
			this.removeKey(key);
			this.addCompound(key).mergeCompound(NBTItem.convertItemtoNBT(item));
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Get an ItemStack that was saved at the given key
	 *
	 * @param key
	 * @return
	 */
	@Override
	public ItemStack getItemStack(String key) {
		try {
			this.readLock.lock();
			final NBTCompound comp = this.getCompound(key);
			if (comp == null)
				return null; // NBTReflectionUtil#convertNBTCompoundtoNMSItem doesn't accept null
			return NBTItem.convertNBTtoItem(comp);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Save an ItemStack Array as a compound under a given key
	 *
	 * @param key
	 * @param items
	 */
	@Override
	public void setItemStackArray(String key, ItemStack[] items) {
		try {
			this.writeLock.lock();
			this.removeKey(key);
			this.addCompound(key).mergeCompound(NBTItem.convertItemArraytoNBT(items));
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Get an {@link ItemStack} array that was saved at the given key, or null if no
	 * stored data was found
	 *
	 * @param key key
	 * @return The stored {@link ItemStack} array, or null if stored data wasn't
	 *         found
	 */
	@Override
	public ItemStack[] getItemStackArray(String key) {
		try {
			this.readLock.lock();
			final NBTCompound comp = this.getCompound(key);
			if (comp == null)
				return null; // NBTReflectionUtil#convertNBTCompoundtoNMSItem doesn't accept null
			return NBTItem.convertNBTtoItemArray(comp);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Setter
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void setUUID(String key, UUID value) {
		try {
			this.writeLock.lock();
			if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R1))
				NBTReflectionUtil.setData(this, ReflectionMethod.COMPOUND_SET_UUID, key, value);
			else
				this.setString(key, value.toString());
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Getter
	 *
	 * @param key
	 * @return The stored value or NMS fallback
	 */
	@Override
	public UUID getUUID(String key) {
		try {
			this.readLock.lock();
			if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R1)
					&& this.getType(key) == NBTType.NBTTagIntArray)
				return (UUID) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_UUID, key);
			else if (this.getType(key) == NBTType.NBTTagString)
				try {
					return UUID.fromString(this.getString(key));
				} catch (final IllegalArgumentException ex) {
					return null;
				}
			else
				return null;
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Checks whether the provided key exists
	 *
	 * @param key String key
	 * @return True if the key is set
	 * @deprecated Use {@link #hasTag(String)} instead
	 */
	@Deprecated
	public Boolean hasKey(String key) {
		return this.hasTag(key);
	}

	/**
	 * Checks whether the provided key exists
	 *
	 * @param key String key
	 * @return true, if the key is set
	 */
	@Override
	public boolean hasTag(String key) {
		try {
			this.readLock.lock();
			final Boolean b = (Boolean) NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_HAS_KEY, key);
			if (b == null)
				return false;
			return b;
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * @param key Deletes the given Key
	 */
	@Override
	public void removeKey(String key) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.remove(this, key);
			this.saveCompound();
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @return Set of all stored Keys
	 */
	@Override
	public Set<String> getKeys() {
		try {
			this.readLock.lock();
			return new HashSet<>(NBTReflectionUtil.getKeys(this));
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Creates a subCompound, or returns it if already provided
	 *
	 * @param name Key to use
	 * @return The subCompound Object
	 */
	public NBTCompound addCompound(String name) {
		try {
			this.writeLock.lock();
			if (this.getType(name) == NBTType.NBTTagCompound)
				return this.getCompound(name);
			NBTReflectionUtil.addNBTTagCompound(this, name);
			final NBTCompound comp = this.getCompound(name);
			if (comp == null)
				throw new NbtApiException("Error while adding Compound, got null!");
			this.saveCompound();
			return comp;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The Compound instance or null
	 */
	@Override
	public NBTCompound getCompound(String name) {
		try {
			this.readLock.lock();
			if (this.getType(name) != NBTType.NBTTagCompound)
				return null;
			final NBTCompound next = new NBTCompound(this, name, this.readOnly);
			if (NBTReflectionUtil.valideCompound(next))
				return next;
			return null;
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * The same as addCompound, just with a name that better reflects what it does
	 *
	 * @param name
	 * @return
	 */
	@Override
	public NBTCompound getOrCreateCompound(String name) {
		return this.addCompound(name);
	}

	/**
	 * @param name
	 * @return The retrieved String List
	 */
	@Override
	public NBTList<String> getStringList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<String> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagString, String.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Integer List
	 */
	@Override
	public NBTList<Integer> getIntegerList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<Integer> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagInt, Integer.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Integer List
	 */
	@Override
	public NBTList<int[]> getIntArrayList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<int[]> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagIntArray, int[].class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Integer List
	 */
	@Override
	public NBTList<UUID> getUUIDList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<UUID> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagIntArray, UUID.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Float List
	 */
	@Override
	public NBTList<Float> getFloatList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<Float> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagFloat, Float.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Double List
	 */
	@Override
	public NBTList<Double> getDoubleList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<Double> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagDouble, Double.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Long List
	 */
	@Override
	public NBTList<Long> getLongList(String name) {
		try {
			this.writeLock.lock();
			final NBTList<Long> list = NBTReflectionUtil.getList(this, name, NBTType.NBTTagLong, Long.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Returns the type of the list, null if not a list
	 *
	 * @param name
	 * @return
	 */
	@Override
	public NBTType getListType(String name) {
		try {
			this.readLock.lock();
			if (this.getType(name) != NBTType.NBTTagList)
				return null;
			return NBTReflectionUtil.getListType(this, name);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * @param name
	 * @return The retrieved Compound List
	 */
	@Override
	public NBTCompoundList getCompoundList(String name) {
		try {
			this.writeLock.lock();
			final NBTCompoundList list = (NBTCompoundList) (Object) NBTReflectionUtil.getList(this, name,
					NBTType.NBTTagCompound, NBTListCompound.class);
			this.saveCompound();
			return list;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Returns the stored value if exists, or provided value otherwise.
	 * <p>
	 * Supported types:
	 * {@code Boolean, Byte, Short, Integer, Long, Float, Double, byte[], int[], long[]},
	 * {@link String}, {@link UUID}, and {@link Enum}
	 *
	 * @param key          key
	 * @param defaultValue default non-null value
	 * @param <T>          value type
	 * @return Stored or provided value
	 */
	@Override

	public <T> T getOrDefault(String key, T defaultValue) {
		if (defaultValue == null)
			throw new NullPointerException("Default type in getOrDefault can't be null!");
		if (!this.hasTag(key))
			return defaultValue;

		final Class<?> clazz = defaultValue.getClass();
		if (clazz == Boolean.class || clazz == boolean.class)
			return (T) this.getBoolean(key);
		if (clazz == Byte.class || clazz == byte.class)
			return (T) this.getByte(key);
		if (clazz == Short.class || clazz == short.class)
			return (T) this.getShort(key);
		if (clazz == Integer.class || clazz == int.class)
			return (T) this.getInteger(key);
		if (clazz == Long.class || clazz == long.class)
			return (T) this.getLong(key);
		if (clazz == Float.class || clazz == float.class)
			return (T) this.getFloat(key);
		if (clazz == Double.class || clazz == double.class)
			return (T) this.getDouble(key);
		if (clazz == byte[].class)
			return (T) this.getByteArray(key);
		if (clazz == int[].class)
			return (T) this.getIntArray(key);
		if (clazz == long[].class)
			return (T) this.getLongArray(key);
		if (clazz == String.class)
			return (T) this.getString(key);
		if (clazz == UUID.class) {
			final UUID uuid = this.getUUID(key);
			return uuid == null ? defaultValue : (T) uuid;
		}
		if (clazz.isEnum()) {
			@SuppressWarnings("rawtypes")
			final Object obj = this.getEnum(key, (Class) defaultValue.getClass());
			return obj == null ? defaultValue : (T) obj;
		}

		throw new NbtApiException("Unsupported type for getOrDefault: " + clazz.getName());
	}

	/**
	 * Returns the stored value if exists, or null.
	 * <p>
	 * Supported types:
	 * {@code Boolean, Byte, Short, Integer, Long, Float, Double, byte[], int[], long[]},
	 * {@link String}, {@link UUID}, and {@link Enum}
	 *
	 * @param key  key
	 * @param type data type
	 * @param <T>  value type
	 * @return Stored value or null
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public <T> T getOrNull(String key, Class<?> type) {
		if (type == null)
			throw new NullPointerException("Default type in getOrNull can't be null!");
		if (!this.hasTag(key))
			return null;

		if (type == Boolean.class || type == boolean.class)
			return (T) this.getBoolean(key);
		if (type == Byte.class || type == byte.class)
			return (T) this.getByte(key);
		if (type == Short.class || type == short.class)
			return (T) this.getShort(key);
		if (type == Integer.class || type == int.class)
			return (T) this.getInteger(key);
		if (type == Long.class || type == long.class)
			return (T) this.getLong(key);
		if (type == Float.class || type == float.class)
			return (T) this.getFloat(key);
		if (type == Double.class || type == double.class)
			return (T) this.getDouble(key);
		if (type == byte[].class)
			return (T) this.getByteArray(key);
		if (type == int[].class)
			return (T) this.getIntArray(key);
		if (type == long[].class)
			return (T) this.getLongArray(key);
		if (type == String.class)
			return (T) this.getString(key);
		if (type == UUID.class)
			return (T) this.getUUID(key);
		if (type.isEnum())
			return (T) this.getEnum(key, (Class) type);

		throw new NbtApiException("Unsupported type for getOrNull: " + type.getName());
	}

	@Override
	public <T> T resolveOrNull(String key, Class<?> type) {
		final List<PathSegment> keys = PathUtil.splitPath(key);
		NBTCompound tag = this;
		for (int i = 0; i < keys.size() - 1; i++) {
			final PathSegment segment = keys.get(i);
			if (!segment.hasIndex()) {
				tag = tag.getCompound(segment.getPath());
				if (tag == null)
					return null;
			} else if (tag.getType(segment.getPath()) == NBTType.NBTTagList
					&& tag.getListType(segment.getPath()) == NBTType.NBTTagCompound) {
				final NBTCompoundList list = tag.getCompoundList(segment.getPath());
				if (segment.getIndex() >= 0)
					tag = list.get(segment.getIndex());
				else
					tag = list.get(list.size() + segment.getIndex());
			}
		}
		final PathSegment segment = keys.get(keys.size() - 1);
		if (!segment.hasIndex())
			return tag.getOrNull(segment.getPath(), type);
		else
			return this.getIndexedValue(tag, segment, (Class<T>) type);
	}

	@Override
	public <T> T resolveOrDefault(String key, T defaultValue) {
		final List<PathSegment> keys = PathUtil.splitPath(key);
		NBTCompound tag = this;
		for (int i = 0; i < keys.size() - 1; i++) {
			final PathSegment segment = keys.get(i);
			if (!segment.hasIndex()) {
				tag = tag.getCompound(segment.getPath());
				if (tag == null)
					return defaultValue;
			} else if (tag.getType(segment.getPath()) == NBTType.NBTTagList
					&& tag.getListType(segment.getPath()) == NBTType.NBTTagCompound) {
				final NBTCompoundList list = tag.getCompoundList(segment.getPath());
				if (segment.getIndex() >= 0)
					tag = list.get(segment.getIndex());
				else
					tag = list.get(list.size() + segment.getIndex());
			}
		}
		final PathSegment segment = keys.get(keys.size() - 1);
		if (!segment.hasIndex())
			return tag.getOrDefault(segment.getPath(), defaultValue);
		else
			return this.getIndexedValue(tag, segment, (Class<T>) defaultValue.getClass());
	}

	private <T> T getIndexedValue(NBTCompound comp, PathSegment segment, Class<T> type) {
		if (type == String.class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagList
					&& comp.getListType(segment.getPath()) == NBTType.NBTTagString)
				if (segment.getIndex() >= 0)
					return (T) comp.getStringList(segment.getPath()).get(segment.getIndex());
				else {
					final List<String> list = comp.getStringList(segment.getPath());
					return (T) list.get(list.size() + segment.getIndex());
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		if (type == int.class || type == Integer.class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagList
					&& comp.getListType(segment.getPath()) == NBTType.NBTTagInt) {
				if (segment.getIndex() >= 0)
					return (T) comp.getIntegerList(segment.getPath()).get(segment.getIndex());
				else {
					final List<Integer> list = comp.getIntegerList(segment.getPath());
					return (T) list.get(list.size() + segment.getIndex());
				}
			} else if (comp.getType(segment.getPath()) == NBTType.NBTTagIntArray)
				if (segment.getIndex() >= 0) {
					final int[] array = comp.getIntArray(segment.getPath());
					if (array != null)
						return (T) (Integer) array[segment.getIndex()];
				} else {
					final int[] array = comp.getIntArray(segment.getPath());
					if (array != null)
						return (T) (Integer) array[array.length + segment.getIndex()];
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		if (type == long.class || type == Long.class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagList
					&& comp.getListType(segment.getPath()) == NBTType.NBTTagLong) {
				if (segment.getIndex() >= 0)
					return (T) comp.getLongList(segment.getPath()).get(segment.getIndex());
				else {
					final List<Long> list = comp.getLongList(segment.getPath());
					return (T) list.get(list.size() + segment.getIndex());
				}
			} else if (comp.getType(segment.getPath()) == NBTType.NBTTagLongArray)
				if (segment.getIndex() >= 0) {
					final long[] array = comp.getLongArray(segment.getPath());
					if (array != null)
						return (T) (Long) array[segment.getIndex()];
				} else {
					final long[] array = comp.getLongArray(segment.getPath());
					if (array != null)
						return (T) (Long) array[array.length + segment.getIndex()];
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		if (type == float.class || type == Float.class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagList
					&& comp.getListType(segment.getPath()) == NBTType.NBTTagFloat)
				if (segment.getIndex() >= 0)
					return (T) comp.getFloatList(segment.getPath()).get(segment.getIndex());
				else {
					final List<Float> list = comp.getFloatList(segment.getPath());
					return (T) list.get(list.size() + segment.getIndex());
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		if (type == double.class || type == Double.class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagList
					&& comp.getListType(segment.getPath()) == NBTType.NBTTagDouble)
				if (segment.getIndex() >= 0)
					return (T) comp.getDoubleList(segment.getPath()).get(segment.getIndex());
				else {
					final List<Double> list = comp.getDoubleList(segment.getPath());
					return (T) list.get(list.size() + segment.getIndex());
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		if (type == int[].class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagList
					&& comp.getListType(segment.getPath()) == NBTType.NBTTagIntArray)
				if (segment.getIndex() >= 0)
					return (T) comp.getIntArrayList(segment.getPath()).get(segment.getIndex());
				else {
					final List<int[]> list = comp.getIntArrayList(segment.getPath());
					return (T) list.get(list.size() + segment.getIndex());
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		if (type == byte.class || type == Byte.class) {
			if (comp.getType(segment.getPath()) == NBTType.NBTTagByteArray)
				if (segment.getIndex() >= 0) {
					final byte[] array = comp.getByteArray(segment.getPath());
					if (array != null)
						return (T) (Byte) array[segment.getIndex()];
				} else {
					final byte[] array = comp.getByteArray(segment.getPath());
					if (array != null)
						return (T) (Byte) array[array.length + segment.getIndex()];
				}
			throw new NbtApiException("No fitting list/array found for " + segment.getPath() + " of type " + type);
		}
		throw new NbtApiException("Unable to get indexed value for type " + type);
	}

	@Override
	public ReadWriteNBT resolveCompound(String key) {
		final List<PathSegment> keys = PathUtil.splitPath(key);
		NBTCompound tag = this;
		for (int i = 0; i < keys.size(); i++) {
			final PathSegment segment = keys.get(i);
			if (!segment.hasIndex()) {
				tag = tag.getCompound(segment.getPath());
				if (tag == null)
					return null;
			} else if (tag.getType(segment.getPath()) == NBTType.NBTTagList
					&& tag.getListType(segment.getPath()) == NBTType.NBTTagCompound) {
				final NBTCompoundList list = tag.getCompoundList(segment.getPath());
				if (segment.getIndex() >= 0)
					tag = list.get(segment.getIndex());
				else
					tag = list.get(list.size() + segment.getIndex());
			}
		}
		return tag;
	}

	@Override
	public ReadWriteNBT resolveOrCreateCompound(String key) {
		final List<PathSegment> keys = PathUtil.splitPath(key);
		NBTCompound tag = this;
		for (int i = 0; i < keys.size(); i++) {
			final PathSegment segment = keys.get(i);
			if (!segment.hasIndex()) {
				tag = tag.getOrCreateCompound(segment.getPath());
				if (tag == null)
					return null;
			} else if (tag.getType(segment.getPath()) == NBTType.NBTTagList
					&& tag.getListType(segment.getPath()) == NBTType.NBTTagCompound) {
				final NBTCompoundList list = tag.getCompoundList(segment.getPath());
				if (segment.getIndex() >= 0)
					tag = list.get(segment.getIndex());
				else
					tag = list.get(list.size() + segment.getIndex());
			}
		}
		return tag;
	}

	/**
	 * Set a key to the given Enum value. It gets stored as a String. Passing null
	 * as value will call removeKey(key) instead.
	 *
	 * @param <E>
	 * @param key
	 * @param value
	 */
	@Override
	public <E extends Enum<?>> void setEnum(String key, E value) {
		if (value == null) {
			this.removeKey(key);
			return;
		}
		this.setString(key, value.name());
	}

	/**
	 * Get an Enum value that has been set via setEnum or setString(key,
	 * value.name()). Passing null/invalid keys will return null.
	 *
	 * @param <E>
	 * @param key
	 * @param type
	 * @return
	 */
	@Override
	public <E extends Enum<E>> E getEnum(String key, Class<E> type) {
		if (key == null || type == null)
			return null;
		final String name = this.getString(key);
		if (name == null)
			return null;
		try {
			return Enum.valueOf(type, name);
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * @param name
	 * @return The type of the given stored key or null
	 */
	@Override
	public NBTType getType(String name) {
		try {
			this.readLock.lock();
			if (MinecraftVersion.getVersion() == MinecraftVersion.MC1_7_R4) {
				final Object nbtbase = NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET, name);
				if (nbtbase == null)
					return null;
				return NBTType.valueOf((byte) ReflectionMethod.COMPOUND_OWN_TYPE.run(nbtbase));
			}
			final Object o = NBTReflectionUtil.getData(this, ReflectionMethod.COMPOUND_GET_TYPE, name);
			if (o == null)
				return null;
			return NBTType.valueOf((byte) o);
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public void writeCompound(OutputStream stream) {
		try {
			this.writeLock.lock();
			NBTReflectionUtil.writeApiNBT(this, stream);
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public String toString() {
		/*
		 * StringBuilder result = new StringBuilder(); for (String key : getKeys()) {
		 * result.append(toString(key)); } return result.toString();
		 */
		return this.asNBTString();
	}

	/**
	 * @deprecated Just use toString()
	 * @param key
	 * @return A string representation of the given key
	 */
	@Deprecated
	public String toString(String key) {
		/*
		 * StringBuilder result = new StringBuilder(); NBTCompound compound = this;
		 * while (compound.getParent() != null) { result.append("   "); compound =
		 * compound.getParent(); } if (this.getType(key) == NBTType.NBTTagCompound) {
		 * return this.getCompound(key).toString(); } else { return result + "-" + key +
		 * ": " + getContent(key) + System.lineSeparator(); }
		 */
		return this.asNBTString();
	}

	/**
	 * Remove all keys from this compound
	 */
	@Override
	public void clearNBT() {
		for (final String key : this.getKeys())
			this.removeKey(key);
	}

	/**
	 * @deprecated Just use toString()
	 * @return A {@link String} representation of the NBT in Mojang JSON. This is
	 *         different from normal JSON!
	 */
	@Deprecated
	public String asNBTString() {
		try {
			this.readLock.lock();
			final Object comp = this.getResolvedObject();
			if (comp == null)
				return "{}";
			if (MinecraftVersion.isForgePresent() && MinecraftVersion.getVersion() == MinecraftVersion.MC1_7_R4)
				return Forge1710Mappings.toString(comp);
			else
				return comp.toString();
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	/**
	 * Does a deep compare to check if everything is the same
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof NBTCompound) {
			final NBTCompound other = (NBTCompound) obj;
			if (this.getKeys().equals(other.getKeys())) {
				for (final String key : this.getKeys())
					if (!isEqual(this, other, key))
						return false;
				return true;
			}
		}
		return false;
	}

	private static boolean isEqual(NBTCompound compA, NBTCompound compB, String key) {
		if (compA.getType(key) != compB.getType(key))
			return false;
		switch (compA.getType(key)) {
			case NBTTagByte:
				return compA.getByte(key).equals(compB.getByte(key));
			case NBTTagByteArray:
				return Arrays.equals(compA.getByteArray(key), compB.getByteArray(key));
			case NBTTagCompound: {
				final NBTCompound tmp = compA.getCompound(key);
				return tmp != null && tmp.equals(compB.getCompound(key));
			}
			case NBTTagDouble:
				return compA.getDouble(key).equals(compB.getDouble(key));
			case NBTTagEnd:
				return true; // ??
			case NBTTagFloat:
				return compA.getFloat(key).equals(compB.getFloat(key));
			case NBTTagInt:
				return compA.getInteger(key).equals(compB.getInteger(key));
			case NBTTagIntArray:
				return Arrays.equals(compA.getIntArray(key), compB.getIntArray(key));
			case NBTTagList:
				return NBTReflectionUtil.getEntry(compA, key).toString()
						.equals(NBTReflectionUtil.getEntry(compB, key).toString()); // Just string compare the 2 lists
			case NBTTagLong:
				return compA.getLong(key).equals(compB.getLong(key));
			case NBTTagShort:
				return compA.getShort(key).equals(compB.getShort(key));
			case NBTTagString:
				return compA.getString(key).equals(compB.getString(key));
			case NBTTagLongArray:
				return Arrays.equals(compA.getLongArray(key), compB.getLongArray(key));
		}
		return false;
	}

}