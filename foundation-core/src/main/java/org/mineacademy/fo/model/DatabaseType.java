package org.mineacademy.fo.model;

import java.util.Arrays;

import org.mineacademy.fo.exception.FoException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the database type.
 */
@Getter
@RequiredArgsConstructor
public enum DatabaseType {
	LOCAL("local", "sqlite", false),
	REMOTE("remote", "mysql", true);

	private final String key;
	private final String driver;
	private final boolean remote;

	@Override
	public String toString() {
		return this.key;
	}

	public static DatabaseType fromKey(String key) {
		for (final DatabaseType type : values())
			if (type.key.equalsIgnoreCase(key) || type.driver.equalsIgnoreCase(key))
				return type;

		throw new FoException("No such database type: " + key + ", available: " + Arrays.toString(values()), false);
	}
}