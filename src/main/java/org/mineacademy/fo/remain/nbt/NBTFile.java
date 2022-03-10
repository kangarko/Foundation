package org.mineacademy.fo.remain.nbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * {@link NBTCompound} implementation backed by a {@link File}
 *
 * @author tr7zw
 *
 */
public class NBTFile extends NBTCompound {

	private final File file;
	private Object nbt;

	/**
	 * Creates a NBTFile that uses @param file to store it's data. If this file
	 * exists, the data will be loaded.
	 *
	 * @param file
	 * @throws IOException
	 */
	public NBTFile(File file) throws IOException {
		super(null, null);
		if (file == null)
			throw new NullPointerException("File can't be null!");
		this.file = file;
		if (file.exists()) {
			final FileInputStream inputsteam = new FileInputStream(file);
			this.nbt = NBTReflectionUtil.readNBT(inputsteam);
		} else {
			this.nbt = ObjectCreator.NMS_NBTTAGCOMPOUND.getInstance();
			this.save();
		}
	}

	/**
	 * Saves the data to the file
	 *
	 * @throws IOException
	 */
	public void save() throws IOException {
		try {
			this.getWriteLock().lock();
			if (!this.file.exists()) {
				this.file.getParentFile().mkdirs();
				if (!this.file.createNewFile())
					throw new IOException("Unable to create file at " + this.file.getAbsolutePath());
			}
			final FileOutputStream outStream = new FileOutputStream(this.file);
			NBTReflectionUtil.writeNBT(this.nbt, outStream);
		} finally {
			this.getWriteLock().unlock();
		}
	}

	/**
	 * @return The File used to store the data
	 */
	public File getFile() {
		return this.file;
	}

	@Override
	public Object getCompound() {
		return this.nbt;
	}

	@Override
	protected void setCompound(Object compound) {
		this.nbt = compound;
	}

}
