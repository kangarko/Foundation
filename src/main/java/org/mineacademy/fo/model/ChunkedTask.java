package org.mineacademy.fo.model;

import org.mineacademy.fo.Common;

import lombok.Getter;

/**
 * Splits manipulating with large about of items in a list
 * into smaller pieces
 */
public abstract class ChunkedTask {

	/**
	 * How many items should we process at once?
	 */
	private static final int BULK_AMOUNT = 100_000;

	/**
	 * How many ticks should we wait before processing the next bulk amount?
	 */
	private static final int WAIT_PERIOD_TICKS = 20;

	/*
	 * The current index where we are processing at, right now
	 */
	@Getter
	private int currentIndex = 0;

	/**
	 * Start the chain, will run several sync tasks until done
	 */
	public final void startChain() {
		Common.runLater(() -> {
			final long now = System.currentTimeMillis();

			boolean finished = false;
			int processed = 0;

			for (int i = currentIndex; i < currentIndex + BULK_AMOUNT; i++) {
				if (!canContinue(i)) {
					finished = true;

					break;
				}

				onProcess(i);
				processed++;
			}

			Common.log(getProcessMessage(now, processed));

			if (!finished) {
				currentIndex += BULK_AMOUNT;

				Common.runLaterAsync(WAIT_PERIOD_TICKS, () -> startChain());

			} else
				onFinish();
		});
	}

	/**
	 * Called when we process a single item
	 *
	 * @param item
	 */
	protected abstract void onProcess(int index);

	/**
	 * Return if the task may execute the next index
	 *
	 * @param index
	 * @return true if can continue
	 */
	protected abstract boolean canContinue(int index);

	/**
	 * Get the message to send to the console on each progress, or null if no message
	 *
	 * @param initialTime
	 * @param processed
	 * @return
	 */
	protected String getProcessMessage(long initialTime, int processed) {
		return "Processed " + String.format("%,d", processed) + " blocks. Took " + (System.currentTimeMillis() - initialTime) + " ms";
	}

	/**
	 * Called when the processing is finished
	 */
	protected void onFinish() {
	}
}