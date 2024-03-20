package org.mineacademy.fo.remain.nbt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface NBTProxy {

	final Map<Class<?>, NBTHandler<Object>> handlers = new HashMap<>();

	public default void init() {

	}

	public default Casing getCasing() {
		return Casing.PascalCase;
	}

	public default <T> NBTHandler<T> getHandler(Class<T> clazz) {
		return (NBTHandler<T>) handlers.get(clazz);
	}

	public default Collection<NBTHandler<Object>> getHandlers() {
		return handlers.values();
	}

	public default <T> void registerHandler(Class<T> clazz, NBTHandler<T> handler) {
		handlers.put(clazz, (NBTHandler<Object>) handler);
	}

}
