package org.mineacademy.fo.remain.nbt;

import java.util.Objects;
import java.util.Optional;

final class CodecHelper {

	public static Object convertItemStackToNbt(final Object itemStack) {
		Object result = null;
		try {
			// FIXME caching, this has 0 exception handling
			result = NBTReflectionUtil.itemstack_codec.encodeStart(NBTReflectionUtil.nbtRegistryOps, itemStack);
			Objects.requireNonNull(result);
			return ((Optional<Object>) result.getClass().getMethod("result").invoke(result)).get();
		} catch (final Exception e) {
			throw new NbtApiException("Failed to convert ItemStack to NBT. " + result + " " + itemStack, e);
		}
	}

	public static Object convertNbtToItemStack(final Object nbt) {
		Object result = null;
		try {
			// FIXME caching, this has 0 exception handling
			result = NBTReflectionUtil.itemstack_codec.parse(NBTReflectionUtil.nbtRegistryOps, nbt);
			Objects.requireNonNull(result);
			return ((Optional<Object>) result.getClass().getMethod("result").invoke(result)).get();
		} catch (final Exception e) {
			throw new NbtApiException("Failed to convert NBT to ItemStack. " + result + " " + nbt, e);
		}
	}

}
