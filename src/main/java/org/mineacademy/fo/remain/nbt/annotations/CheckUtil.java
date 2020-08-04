package org.mineacademy.fo.remain.nbt.annotations;

import java.lang.reflect.Method;

import org.mineacademy.fo.remain.nbt.NbtApiException;
import org.mineacademy.fo.remain.nbt.utils.MinecraftVersion;

public class CheckUtil {

	public static boolean isAvaliable(final Method method) {
		if (MinecraftVersion.getVersion().getVersionId() < method.getAnnotation(AvaliableSince.class).version().getVersionId())
			throw new NbtApiException("The Method '" + method.getName() + "' is only avaliable for the Versions " + method.getAnnotation(AvaliableSince.class).version() + "+, but still got called!");
		return true;
	}

}
