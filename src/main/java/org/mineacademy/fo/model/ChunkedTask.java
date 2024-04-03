package org.mineacademy.fo.model;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;

import lombok.Getter;
import lombok.Setter;

/**
 * Splits manipulating with large about of items in a list
 * into smaller pieces
 */
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

	/*
	 * Private flag to prevent dupe executions and cancel running tasks
	 */
	@Getter
	private boolean processing = false;
	private boolean firstLaunch = false;

	/**
	 * Create a new task that will process the given amount of times on each run
	 * (see getWaitPeriodTicks() and wait for 1 second between each time
	 *
	 * @param processAmount
	 */
	public ChunkedTask(int processAmount) {
		this.processAmount = processAmount;
	}

	/**
	 * Create a new task that will process the given amount of times on each run
	 * and wait the given amount of ticks between each time
	 *
	 * @param processAmount
	 * @param waitPeriodTicks
	 */
	public ChunkedTask(int processAmount, int waitPeriodTicks) {
		this.processAmount = processAmount;
		this.waitPeriodTicks = waitPeriodTicks;
	}

	/**
	 * Start the chain, will run several sync tasks until done
	 */
	public final void startChain() {

		if (!this.firstLaunch) {
			this.processing = true;

			this.firstLaunch = true;
		}

		Common.runLater(() -> {

			// Cancelled prematurely
			if (!this.processing) {
				this.onFinish(false);
				this.firstLaunch = false;

				return;
			}

			final long now = System.currentTimeMillis();

			boolean finished = false;
			int processed = 0;

			for (int i = this.currentIndex; i < this.currentIndex + this.processAmount; i++) {
				if (!this.canContinue(i)) {
					finished = true;

					break;
				}

				processed++;

				try {
					this.onProcess(i);

				} catch (final Throwable t) {
					Common.error(t, "Error in " + this + " processing index " + processed);
					this.processing = false;
					this.firstLaunch = false;

					this.onFinish(false);
					return;
				}
			}

			if (processed > 0 || !finished)
				Common.log(this.getProcessMessage(now, processed));

			if (!finished) {
				this.currentIndex += this.processAmount;

				Common.runLaterAsync(this.waitPeriodTicks, this::startChain);

			} else {
				this.processing = false;
				this.firstLaunch = false;

				this.onFinish(true);
			}
		});
	}

	/**
	 * Attempts to cancel this running task, throwing error if it is not running (use {@link #isProcessing()}
	 */
	public final void cancel() {
		Valid.checkBoolean(this.processing, "Chunked task is not running: " + this);

		this.processing = false;
	}

	/**
	 * Called when we process a single item
	 *
	 * @param item
	 */
	protected abstract void onProcess(int index) throws Throwable;

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
		return "Processed " + String.format("%,d", processed) + " " + this.getLabel() + ". Took " + (System.currentTimeMillis() - initialTime) + " ms";
	}

	/**
	 * Called when the processing is finished
	 *
	 * @param gracefully true if natural end, false if {@link #cancel()} used
	 */
	protected void onFinish(boolean gracefully) {
		this.onFinish();
	}

	/**
	 * @see #onFinish(boolean)
	 * @deprecated it is prefered to call {@link #onFinish(boolean)} instead
	 */
	@Deprecated
	protected void onFinish() {
	}

	/**
	 * Get the label for the process message
	 * "blocks" by default
	 *
	 * @return
	 */
	protected String getLabel() {
		return "blocks";
	}
}