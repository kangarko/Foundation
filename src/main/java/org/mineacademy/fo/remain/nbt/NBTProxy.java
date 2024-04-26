package org.mineacademy.fo.remain.nbt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface NBTProxy {

	Map<Class<?>, NBTHandler<Object>> handlers = new HashMap<>();

	default void init() {

	}

	default Casing getCasing() {
		return Casing.PascalCase;
	}

	default <T> NBTHandler<T> getHandler(Class<T> clazz) {
		return (NBTHandler<T>) handlers.get(clazz);
	}

	default Collection<NBTHandler<Object>> getHandlers() {
		return handlers.values();
	}

	default <T> void registerHandler(Class<T> clazz, NBTHandler<T> handler) {
		handlers.put(clazz, (NBTHandler<Object>) handler);
	}

}
