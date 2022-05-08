package org.mineacademy.fo.model;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;

/**
 * An engine that reads rule set from a file such as in ChatControl.
 * @param <T>
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
	 * Load all items in this ruleset
	 */
	public abstract void load();

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Toggle the given rule on/off
	 *
	 * @param rule
	 * @param disabled
	 */
	public final void toggleMessage(Rule rule, boolean disabled) {

		final File file = rule.getFile();
		Valid.checkBoolean(file.exists(), "No such file: " + file + " Rule: " + rule);

		final List<String> lines = FileUtil.readLines(file);
		boolean found = false;

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);

			// Found our rule
			if (line.equals(this.newKeyword + " " + rule.getGroupName()))
				found = true;

			// Found something else
			else if (line.startsWith("#") || line.isEmpty() || line.startsWith("match ")) {
				if (found && i > 0 && disabled) {
					lines.add(i, "disabled");

					break;
				}
			}

			// Found the disabled operator
			else if (line.equals("disabled"))
				if (found && !disabled) {
					lines.remove(i);

					break;
				}
		}

		Valid.checkBoolean(found, "Failed to disable rule " + rule);
		this.saveAndLoad(file, lines);
	}

	/**
	 * Save the given file with the given lines and reloads
	 *
	 * @param rule
	 * @param lines
	 */
	protected final void saveAndLoad(File file, List<String> lines) {
		FileUtil.write(file, lines, StandardOpenOption.TRUNCATE_EXISTING);

		this.load();
	}

	/**
	 * Load rules from the given file path in your plugin folder
	 *
	 * @param path
	 * @return
	 */
	protected final List<T> loadFromFile(String path) {
		final File file = FileUtil.extract(path);

		return this.loadFromFile(file);
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

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i).trim();

			if (!line.isEmpty() && !line.startsWith("#"))
				// If a line starts with matcher then assume a new rule is found and start creating it. This makes a new instance of the object.
				if (line.startsWith(this.newKeyword + " ")) {

					// Found another match, assuming previous rule is finished creating.
					if (rule != null)
						if (this.canFinish(rule))
							rules.add(rule);

					try {
						match = line.replace(this.newKeyword + " ", "");
						rule = this.createRule(file, match);

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
					if (!this.onNoMatchLineParse(file, line))
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

			// Reached end of the file and a rule is still being created, finish it.
			if (i + 1 == lines.size() && rule != null && this.canFinish(rule))
				rules.add(rule);
		}

		return rules;
	}

	/**
	 * Called if there is no match {@link #newKeyword} but something is on the line
	 * enabling you to inject your own custom operators and settings
	 *
	 * Return true if you processed the line, false if we should throw an error
	 *
	 * @param file the current file
	 * @param line
	 * @return
	 */
	protected boolean onNoMatchLineParse(File file, String line) {
		return false;
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
	protected abstract T createRule(File file, String value);
}
