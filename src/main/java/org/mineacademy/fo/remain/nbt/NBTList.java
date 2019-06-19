package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

/**
 * Represents a NBT list
 */
public class NBTList {

	private final String listname;
	private final NBTCompound parent;
	private final NBTType type;
	private final Object listobject;

	protected NBTList(NBTCompound owner, String name, NBTType type, Object list) {
		parent = owner;
		listname = name;
		this.type = type;
		this.listobject = list;
		if (!(type == NBTType.NBTTagString || type == NBTType.NBTTagCompound)) {
			System.err.println("List types that are not String or Compound are not supported!");
		}
	}

	protected void save() {
		parent.set(listname, listobject);
	}

	public NBTListCompound addCompound() {
		if (type != NBTType.NBTTagCompound) {
			new Throwable("Using Compound method on a non Compound list!").printStackTrace();
			return null;
		}
		try {
			final Object comp = NBTReflectionUtil.getNBTTagCompound().newInstance();

			if (MinecraftVersion.atLeast(V.v1_14)) {
				final Method m = listobject.getClass().getMethod("add", int.class, NBTReflectionUtil.getNBTBase());
				m.invoke(listobject, 0, comp);

			} else {
				final Method m = listobject.getClass().getMethod("add", NBTReflectionUtil.getNBTBase());
				m.invoke(listobject, comp);
			}

			return new NBTListCompound(this, comp);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public NBTListCompound getCompound(int id) {
		if (type != NBTType.NBTTagCompound) {
			new Throwable("Using Compound method on a non Compound list!").printStackTrace();
			return null;
		}
		try {
			final Method m = listobject.getClass().getMethod("get", int.class);
			final Object comp = m.invoke(listobject, id);
			return new NBTListCompound(this, comp);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public String getString(int i) {
		if (type != NBTType.NBTTagString) {
			new Throwable("Using String method on a non String list!").printStackTrace();
			return null;
		}
		try {
			final Method m = listobject.getClass().getMethod("getString", int.class);
			return (String) m.invoke(listobject, i);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public void addString(String s) {
		if (type != NBTType.NBTTagString) {
			new Throwable("Using String method on a non String list!").printStackTrace();
			return;
		}
		try {
			if (MinecraftVersion.atLeast(V.v1_14)) {
				final Method m = listobject.getClass().getMethod("add", int.class, NBTReflectionUtil.getNBTBase());
				m.invoke(listobject, 0, NBTReflectionUtil.getNBTTagString().getConstructor(String.class).newInstance(s));

			} else {
				final Method m = listobject.getClass().getMethod("add", NBTReflectionUtil.getNBTBase());
				m.invoke(listobject, NBTReflectionUtil.getNBTTagString().getConstructor(String.class).newInstance(s));
			}
			save();
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public void setString(int i, String s) {
		if (type != NBTType.NBTTagString) {
			new Throwable("Using String method on a non String list!").printStackTrace();
			return;
		}

		try {
			if (MinecraftVersion.olderThan(V.v1_8)) {
				getList().set(i, NBTReflectionUtil.getNBTTagString().getConstructor(String.class).newInstance(s));

			} else {
				final Method m = listobject.getClass().getMethod("a", int.class, NBTReflectionUtil.getNBTBase());

				m.invoke(listobject, i, NBTReflectionUtil.getNBTTagString().getConstructor(String.class).newInstance(s));
			}

			save();

		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	private List getList() throws ReflectiveOperationException {
		final Field l = listobject.getClass().getDeclaredField("list");
		l.setAccessible(true);

		return (List) l.get(listobject);
	}

	public void remove(int i) {
		try {
			if (MinecraftVersion.olderThan(V.v1_8)) {
				getList().remove(i);

			} else {
				final Method m = listobject.getClass().getMethod(NBTReflectionUtil.getRemoveMethodName(), int.class);
				m.invoke(listobject, i);
			}

			save();
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public int size() {
		try {
			final Method m = listobject.getClass().getMethod("size");
			return (int) m.invoke(listobject);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	public NBTType getType() {
		return type;
	}

}
