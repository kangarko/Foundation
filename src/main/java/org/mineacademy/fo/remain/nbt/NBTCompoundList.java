package org.mineacademy.fo.remain.nbt;

import org.apache.commons.lang.NotImplementedException;
import org.mineacademy.fo.remain.nbt.nmsmappings.ClassWrapper;
import org.mineacademy.fo.remain.nbt.nmsmappings.ReflectionMethod;
import org.mineacademy.fo.remain.nbt.utils.MinecraftVersion;

/**
 * {@link NBTListCompound} implementation for NBTLists
 *
 * @author tr7zw
 */
public class NBTCompoundList extends NBTList<NBTListCompound> {

	NBTCompoundList(final NBTCompound owner, final String name, final NBTType type, final Object list) {
		super(owner, name, type, list);
	}

	/**
	 * Adds a new Compound to the end of the List and returns it.
	 *
	 * @return The added {@link NBTListCompound}
	 */
	public NBTListCompound addCompound() {
		return (NBTListCompound) addCompound(null);
	}

	/**
	 * Adds a copy of the Compound to the end of the List and returns it.
	 * When null is given, a new Compound will be created
	 *
	 * @param comp
	 * @return
	 */
	private NBTCompound addCompound(final NBTCompound comp) {
		try {
			final Object compound = ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_14_R1.getVersionId())
				ReflectionMethod.LIST_ADD.run(listObject, size(), compound);
			else
				ReflectionMethod.LEGACY_LIST_ADD.run(listObject, compound);
			getParent().saveCompound();
			final NBTListCompound listcomp = new NBTListCompound(this, compound);
			if (comp != null) listcomp.mergeCompound(comp);
			return listcomp;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	/**
	 * Adds a new Compound to the end of the List.
	 *
	 * @param empty
	 * @return True, if compound was added
	 * @deprecated Please use addCompound!
	 */
	@Override
	@Deprecated
	public boolean add(final NBTListCompound empty) {
		return addCompound(empty) != null;
	}

	@Override
	public void add(final int index, final NBTListCompound element) {
		if (element != null)
			throw new NotImplementedException("You need to pass null! ListCompounds from other lists won't work.");
		try {
			final Object compound = ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_14_R1.getVersionId())
				ReflectionMethod.LIST_ADD.run(listObject, index, compound);
			else
				ReflectionMethod.LEGACY_LIST_ADD.run(listObject, compound);
			super.getParent().saveCompound();
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	@Override
	public NBTListCompound get(final int index) {
		try {
			final Object compound = ReflectionMethod.LIST_GET_COMPOUND.run(listObject, index);
			return new NBTListCompound(this, compound);
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	@Override
	public NBTListCompound set(final int index, final NBTListCompound element) {
		throw new NotImplementedException("This method doesn't work in the ListCompound context.");
	}

	@Override
	protected Object asTag(final NBTListCompound object) {
		return null;
	}

}
