package org.mineacademy.fo.model;

import lombok.RequiredArgsConstructor;

/**
 * This runnable only runs itself when the
 * run method is called for the first time.
 */
@RequiredArgsConstructor
public final class OneTimeRunnable {

	/**
	 * The actual runnable to be run
	 */
	private final Runnable runnable;

	/**
	 * Has {@link #runnable} been run?
	 */
	private boolean hasBeenRun = false;

	/**
	 * Attempts to run the {@link #runnable}.
	 * <p>
	 * If it was not run yet, it is run and flagged as run, otherwise nothing happens.
	 */
	public void runIfHasnt() {
		if (hasBeenRun)
			return;

		try {
			runnable.run();

		} finally {
			hasBeenRun = true;
		}
	}

	/**
	 * Return if the runnable was run
	 *
	 * @return if the runnable has been run already
	 */
	public boolean hasBeenRun() {
		return hasBeenRun;
	}
}
