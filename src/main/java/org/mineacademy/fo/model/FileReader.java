package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;

/**
 * An engine that reads from a file. You need to override most of the methods to
 * make it functional.
 *
 * @deprecated cleanup & improvements needed to make it less ChatControl-specific.
 */
@Deprecated
public abstract class FileReader<T> {

	private final String matcher;
	private T object = null; // The object being created.

	public FileReader(String matcher) {
		this.matcher = matcher;
	}

	protected final T getObject() {
		return object;
	}

	public abstract T newInstance(String line);

	public abstract void onLineParse(String line);

	public boolean canFinish() {
		return true;
	}

	public void onObjectPreSave() {
	}

	public final List<T> load(String path) {
		try {
			return load0(path);
		} catch (final Throwable t) {
			Common.throwError(t,
					"&cError reading " + path,
					"&cCheck for syntax errors.",
					"Error: " + t.getMessage());

			return null;
		}
	}

	private final List<T> load0(String path) {
		final List<String> lines = FileUtil.readLines(FileUtil.extract(path));

		final ArrayList<String> loadedLines = new ArrayList<>();
		final ArrayList<T> loadedValues = new ArrayList<>();

		String previous = null;
		object = null;

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i).trim();

			if (!line.isEmpty() && !line.startsWith("#"))
				// If a line starts with matcher then assume a new rule is found and start creating it. This makes a new instance of the object.
				if (line.startsWith(matcher + " ")) {
					if (object != null) { // Found another match, assuming previous rule is finished creating.
						onObjectPreSave();

						if (canFinish()) {
							Valid.checkBoolean(!loadedLines.contains(line), path + " already contains: " + line + " (line " + i + ")");

							loadedLines.add(line);
							loadedValues.add(object);
						}
					}

					object = newInstance(line);
					previous = object.toString();

				} else {
					if (object == null && previous == null)
						throw new NullPointerException("File " + path + " appears to have wrong encoding! Learn how to fix it here: https://github.com/kangarko/chatcontrol-pro/wiki/Use-Right-Encoding");

					Valid.checkNotNull(object, "Cannot define operator when nothing is being created! File: \'" + path + "\' Line " + i + ": \'" + line + "\' Previous: \'" + previous + "\'");

					// If something is being created then attempt to parse operators.
					onLineParse(line);
				}

			if (i + 1 == lines.size() && object != null) { // Reached end of the file but something is still being created, finish it
				onObjectPreSave();

				if (canFinish()) {
					loadedLines.add(line);
					loadedValues.add(object);
				}
			}
		}

		return loadedValues;
	}
}
