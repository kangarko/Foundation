package org.mineacademy.fo.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;

/**
 * An engine that reads rule set from a file such as in ChatControl.
 */
public abstract class RuleSetReader<T extends Rule> {

	/**
	 * The matching keyword for creating new rules
	 * such as "match" in "match bitch" operator in ChatControl
	 */
	private final String newKeyword;

	/**
	 * Create a new rule set reader for the matching keyword
	 * that will dictate new rule creation
	 *
	 * @param newKeyword
	 */
	public RuleSetReader(String newKeyword) {
		this.newKeyword = newKeyword;
	}

	/**
	 * Load rules from the given file path in your plugin folder
	 *
	 * @param path
	 * @return
	 */
	protected final List<T> loadFromFile(String path) {
		try {
			final File file = FileUtil.extract(path);

			return loadFromFile(file);

		} catch (final Throwable t) {
			Common.throwError(t,
					"Failed to parse rules from " + path,
					"Check for syntax errors.",
					"Error: " + t.getMessage());

			return null;
		}
	}

	/*
	 * Load rules from the given file
	 */
	private final List<T> loadFromFile(File file) {
		final List<T> rules = new ArrayList<>();
		final List<String> lines = FileUtil.readLines(file);

		// The temporary rule being created
		T rule = null;
		String match = null;

		//System.out.println("# READING " + file);

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i).trim();

			//System.out.println(i + "/" + lines.size() + ": " + line);

			if (!line.isEmpty() && !line.startsWith("#")) {

				// If a line starts with matcher then assume a new rule is found and start creating it. This makes a new instance of the object.
				if (line.startsWith(newKeyword + " ")) {

					// Found another match, assuming previous rule is finished creating.
					if (rule != null) {
						Valid.checkBoolean(!rules.contains(rule), "Duplicate rule found in: " + file + "! Line (" + (i + 1) + "): '" + line + "' Rules: " + rules);

						if (canFinish(rule))
							rules.add(rule);
					}

					try {
						match = line.replace(newKeyword + " ", "");
						rule = createRule(file, match);

					} catch (final Throwable t) {
						Common.throwError(t,
								"Error creating rule from line (" + (i + 1) + "): " + line,
								"File: " + file,
								"Error: %error",
								"Processing aborted.");

						return rules;
					}
				}

				// If something is being created then attempt to parse operators.
				else {
					Valid.checkNotNull(match, "Cannot define operator when no rule is being created! File: '" + file + "' Line (" + (i + 1) + "): '" + line + "'");

					if (rule != null)
						try {
							rule.onOperatorParse(line.split(" "));

						} catch (final Throwable t) {
							Common.throwError(t,
									"Error parsing rule operator from line (" + (i + 1) + "): " + line,
									"File: " + file,
									"Error: %error");
						}
				}
			}

			// Reached end of the file and a rule is still being created, finish it.
			if (i + 1 == lines.size() && rule != null && canFinish(rule))
				rules.add(rule);
		}

		return rules;
	}

	/**
	 * Return true if we can finish creating the given rule
	 *
	 * @param rule
	 * @return
	 */
	protected boolean canFinish(T rule) {
		return true;
	}

	/**
	 * Creates a new rule from the value of the {@link #newKeyword}
	 * where the new keyword is stripped from the value.
	 *
	 * Example: When the rule starts with "match one two", the value is only "one two" etc.
	 *
	 * @param file
	 * @param value
	 *
	 * @return the rule created, or null if the value is not valid
	 */
	@Nullable
	protected abstract T createRule(File file, String value);
}
