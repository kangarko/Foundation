package org.mineacademy.fo.remain.nbt;

import javax.annotation.Nonnull;

public interface NBTHandler<T> {

	default boolean fuzzyMatch(Object obj) {
		return false;
	}

	void set(@Nonnull ReadWriteNBT nbt, @Nonnull String key, @Nonnull T value);

	T get(@Nonnull ReadableNBT nbt, @Nonnull String key);

}
