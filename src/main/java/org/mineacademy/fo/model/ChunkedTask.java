package org.mineacademy.fo.model;

import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Splits manipulating with large about of items in a list
 * into smaller pieces
 */
@RequiredArgsConstructor
public abstract class ChunkedTask {

	/**
	 * How many ticks should we wait before processing the next bulk amount?
	 */
	@Setter
	private int waitPeriodTicks = 20;

	/**
	 * How many items should we process at once?
	 */
	private final int processAmount;

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

			for (int i = currentIndex; i < currentIndex + processAmount; i++) {
				if (!canContinue(i)) {
					finished = true;

					break;
				}

				onProcess(i);
				processed++;
			}

			Common.log(getProcessMessage(now, processed));

			if (!finished) {
				currentIndex += processAmount;

				Common.runLaterAsync(waitPeriodTicks, () -> startChain());

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