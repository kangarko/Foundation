package org.mineacademy.fo.remain.nbt;

import javax.annotation.Nonnull;

public interface NBTHandler<T> {

	public default boolean fuzzyMatch(Object obj) {
		return false;
	}

	public void set(@Nonnull ReadWriteNBT nbt, @Nonnull String key, @Nonnull T value);

	public T get(@Nonnull ReadableNBT nbt, @Nonnull String key);

}
