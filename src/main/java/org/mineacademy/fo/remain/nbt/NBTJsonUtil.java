package org.mineacademy.fo.remain.nbt;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

final class NBTJsonUtil {

	/**
	 * 1.20.3-1.21.4 only. Used to convert items into Json, used in Chat Hover Components.
	 *
	 * @param itemStack
	 * @return
	 * @throws NbtApiException
	 */

	public static JsonElement itemStackToJson(ItemStack itemStack) {
		try {
			final Codec<Object> itemStackCodec = (Codec<Object>) ClassWrapper.NMS_ITEMSTACK.getClazz()
					.getField(MojangToMapping.getMapping().get("net.minecraft.world.item.ItemStack#CODEC")).get(null);
			final Object stack = ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, itemStack);
			final DataResult<JsonElement> result = itemStackCodec.encode(stack, JsonOps.INSTANCE,
					JsonOps.INSTANCE.emptyMap());
			final Optional<JsonElement> opt = (Optional<JsonElement>) result.getClass().getMethod("result").invoke(result);
			return opt.orElse(null);
		} catch (final Exception ex) {
			throw new NbtApiException("Error trying to get Json of an ItemStack.", ex);
		}
	}

}
