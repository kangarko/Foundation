/*
 * TaskChain for Bukkit
 *
 * Written by Aikar <aikar@aikar.co>
 * https://aikar.co
 * https://starlis.com
 *
 * @license MIT
 */

package org.mineacademy.fo.model;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * TaskChain v2.6 - by Daniel Ennis <aikar@aikar.co>
 *
 * Facilitates Control Flow for the Bukkit Scheduler to easily jump between
 * Async and Sync tasks without deeply nested callbacks, passing the response of the
 * previous task to the next task to use.
 *
 * Updated for MineAcademy by kangarko
 *
 * Usage example:
 * @see #example
 */
public class TaskChain<T> {

	private static final ThreadLocal<TaskChain<?>> currentChain = new ThreadLocal<>();

	private final ConcurrentLinkedQueue<TaskHolder<?, ?>> chainQueue = new ConcurrentLinkedQueue<>();

	protected Runnable doneCallback;
	protected BiConsumer<Exception, Task<?, ?>> errorHandler;

	private TaskHolder<?, ?> currentHolder;
	private Object previous;
	private boolean async;

	/**
	 * =============================================================================================
	 */

	/**
	 * Starts a new chain.
	 *
	 * @return
	 */
	public static <T> TaskChain<T> newChain() {
		return new TaskChain<>();
	}

	/**
	 * =============================================================================================
	 */

	/**
	 * Call to abort execution of the chain.
	 */
	public void abort() throws AbortChainException {
		throw new AbortChainException();
	}

	/**
	 * =============================================================================================
	 */

	/**
	 * Checks if the previous task return was null.
	 *
	 * If not null, the previous task return will forward to the next task.
	 * @return
	 */
	public TaskChain<T> abortIfNull() {
		return abortIfNull(null, null);
	}

	/**
	 * Checks if the previous task return was null, and aborts if it was, optionally
	 * sending a message to the player.
	 *
	 * If not null, the previous task return will forward to the next task.
	 * @param player
	 * @param msg
	 * @return
	 */
	public TaskChain<T> abortIfNull(Player player, String msg) {
		return current((obj) -> {
			if (obj == null) {
				if (msg != null && player != null) {
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
				}
				abort();
				return null;
			}
			return obj;
		});
	}

	public TaskChain<TaskChain<?>> returnChain() {
		return currentFirst(() -> this);
	}

	/**
	 * Adds a delay to the chain execution.
	 *
	 * @param ticks # of ticks to delay before next task (20 = 1 second)
	 * @return
	 */
	public TaskChain<T> delay(final int ticks) {
		return currentCallback((input, next) -> {
			Bukkit.getScheduler().scheduleSyncDelayedTask(SimplePlugin.getInstance(), () -> next.accept(input), ticks);
		});
	}

	/**
	 * Execute a task on the main thread, with no previous input, and a callback to return the response to.
	 *
	 * It's important you don't perform blocking operations in this method. Only use this if
	 * the task will be scheduling a different sync operation outside of the TaskChains scope.
	 *
	 * Usually you could achieve the same design with a blocking API by switching to an async task
	 * for the next task and running it there.
	 *
	 * This method would primarily be for cases where you need to use an API that ONLY provides
	 * a callback style API.
	 *
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> syncFirstCallback(AsyncExecutingFirstTask<R> task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	/**
	 * @see #syncFirstCallback(AsyncExecutingFirstTask) but ran off main thread
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> asyncFirstCallback(AsyncExecutingFirstTask<R> task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #syncFirstCallback(AsyncExecutingFirstTask) but ran on current thread the Chain was created on
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> currentFirstCallback(AsyncExecutingFirstTask<R> task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * Execute a task on the main thread, with the last output, and a callback to return the response to.
	 *
	 * It's important you don't perform blocking operations in this method. Only use this if
	 * the task will be scheduling a different sync operation outside of the TaskChains scope.
	 *
	 * Usually you could achieve the same design with a blocking API by switching to an async task
	 * for the next task and running it there.
	 *
	 * This method would primarily be for cases where you need to use an API that ONLY provides
	 * a callback style API.
	 *
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> syncCallback(AsyncExecutingTask<R, T> task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	public TaskChain<?> syncCallback(AsyncExecutingGenericTask task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	/**
	 * @see #syncCallback(AsyncExecutingTask) but ran off main thread
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> asyncCallback(AsyncExecutingTask<R, T> task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #syncCallback(AsyncExecutingTask) but ran off main thread
	 * @param task
	 * @return
	 */
	public TaskChain<?> asyncCallback(AsyncExecutingGenericTask task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #syncCallback(AsyncExecutingTask) but ran on current thread the Chain was created on
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> currentCallback(AsyncExecutingTask<R, T> task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * @see #syncCallback(AsyncExecutingTask) but ran on current thread the Chain was created on
	 * @param task
	 * @return
	 */
	public TaskChain<?> currentCallback(AsyncExecutingGenericTask task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * Execute task on main thread, with no input, returning an output
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> syncFirst(FirstTask<R> task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	/**
	 * @see #syncFirst(FirstTask) but ran off main thread
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> asyncFirst(FirstTask<R> task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #syncFirst(FirstTask) but ran on current thread the Chain was created on
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> currentFirst(FirstTask<R> task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * Execute task on main thread, with the last returned input, returning an output
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> sync(Task<R, T> task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	/**
	 * Execute task on main thread, with no input or output
	 * @param task
	 * @return
	 */
	public TaskChain<?> sync(GenericTask task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	/**
	 * @see #sync(Task) but ran off main thread
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> async(Task<R, T> task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #sync(GenericTask) but ran off main thread
	 * @param task
	 * @return
	 */
	public TaskChain<?> async(GenericTask task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #sync(Task) but ran on current thread the Chain was created on
	 * @param task
	 * @param <R>
	 * @return
	 */
	public <R> TaskChain<R> current(Task<R, T> task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * @see #sync(GenericTask) but ran on current thread the Chain was created on
	 * @param task
	 * @return
	 */
	public TaskChain<?> current(GenericTask task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * Execute task on main thread, with the last output, and no furthur output
	 * @param task
	 * @return
	 */
	public TaskChain<?> syncLast(LastTask<T> task) {
		return add0(new TaskHolder<>(this, false, task));
	}

	/**
	 * @see #syncLast(LastTask) but ran off main thread
	 * @param task
	 * @return
	 */
	public TaskChain<?> asyncLast(LastTask<T> task) {
		return add0(new TaskHolder<>(this, true, task));
	}

	/**
	 * @see #syncLast(LastTask) but ran on current thread the Chain was created on
	 * @param task
	 * @return
	 */
	public TaskChain<?> currentLast(LastTask<T> task) {
		return add0(new TaskHolder<>(this, null, task));
	}

	/**
	 * Finished adding tasks, begins executing them.
	 */
	public void execute() {
		execute0();
	}

	protected void execute0() {
		async = !Bukkit.isPrimaryThread();
		nextTask();
	}

	public void executeNext() {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SimplePlugin.getInstance(), this::execute, 1);
	}

	public void execute(Runnable done) {
		this.doneCallback = done;
		execute();
	}

	public void execute(BiConsumer<Exception, Task<?, ?>> errorHandler) {
		this.errorHandler = errorHandler;
		execute();
	}

	public void execute(Runnable done, BiConsumer<Exception, Task<?, ?>> errorHandler) {
		this.doneCallback = done;
		this.errorHandler = errorHandler;
		execute();
	}

	protected void done() {
		if (this.doneCallback != null)
			this.doneCallback.run();
	}

	protected TaskChain add0(TaskHolder<?, ?> task) {
		this.chainQueue.add(task);
		return this;
	}

	/**
	 * Fires off the next task, and switches between Async/Sync as necessary.
	 */
	private void nextTask() {
		synchronized (this) {
			this.currentHolder = this.chainQueue.poll();
		}

		if (this.currentHolder == null) {
			this.previous = null;
			// All Done!
			this.done();
			return;
		}

		Boolean isNextAsync = this.currentHolder.async;
		if (isNextAsync == null) {
			isNextAsync = this.async;
		}

		if (isNextAsync) {
			if (this.async) {
				this.currentHolder.run();
			} else {
				Bukkit.getScheduler().runTaskAsynchronously(SimplePlugin.getInstance(), () -> {
					this.async = true;
					this.currentHolder.run();
				});
			}
		} else {
			if (this.async) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(SimplePlugin.getInstance(), () -> {
					this.async = false;
					this.currentHolder.run();
				});
			} else {
				this.currentHolder.run();
			}
		}
	}

	/**
	 * Provides foundation of a task with what the previous task type should return
	 * to pass to this and what this task will return.
	 * @param <R> Return Type
	 * @param <A> Argument Type Expected
	 */
	private static class TaskHolder<R, A> {
		private final TaskChain<?> chain;
		private final Task<R, A> task;
		public final Boolean async;

		private boolean executed = false;
		private boolean aborted = false;

		private TaskHolder(TaskChain<?> chain, Boolean async, Task<R, A> task) {
			this.task = task;
			this.chain = chain;
			this.async = async;
		}

		/**
		 * Called internally by Task Chain to facilitate executing the task and then the next task.
		 */
		private void run() {
			final Object arg = this.chain.previous;
			this.chain.previous = null;

			try {
				currentChain.set(this.chain);
				if (this.task instanceof AsyncExecutingTask) {
					((AsyncExecutingTask<R, A>) this.task).runAsync((A) arg, this::next);
				} else {
					next(this.task.run((A) arg));
				}
			} catch (final AbortChainException ignored) {
				this.abort();
			} catch (final Exception e) {
				if (this.chain.errorHandler != null) {
					this.chain.errorHandler.accept(e, this.task);
				} else {
					Common.log("TaskChain Exception on " + this.task.getClass().getName());
					Common.log(ExceptionUtils.getFullStackTrace(e));
				}
				this.abort();
			} finally {
				currentChain.remove();
			}
		}

		/**
		 * Abort the chain, and clear tasks for GC.
		 */
		private synchronized void abort() {
			this.aborted = true;
			this.chain.previous = null;
			this.chain.chainQueue.clear();
			this.chain.done();
		}

		/**
		 * Accepts result of previous task and executes the next
		 */
		private void next(Object resp) {
			synchronized (this) {
				if (this.aborted) {
					this.chain.done();
					return;
				}
				if (this.executed) {
					this.chain.done();
					throw new RuntimeException("This task has already been executed.");
				}
				this.executed = true;
			}

			this.chain.async = !Bukkit.isPrimaryThread(); // We don't know where the task called this from.
			this.chain.previous = resp;
			this.chain.nextTask();
		}
	}

	public static class AbortChainException extends Throwable {

		private static final long serialVersionUID = 1L;
	}

	/**
	 * Generic task with synchronous return (but may execute on any thread)
	 * @param <R>
	 * @param <A>
	 */
	public interface Task<R, A> {
		/**
		 * Gets the current chain that is executing this task. This method should only be called on the same thread
		 * that is executing the task.
		 * @return
		 */
		default TaskChain<?> getCurrentChain() {
			return currentChain.get();
		}

		R run(A input) throws AbortChainException;
	}

	public interface AsyncExecutingTask<R, A> extends Task<R, A> {
		/**
		 * Gets the current chain that is executing this task. This method should only be called on the same thread
		 * that is executing the task.
		 *
		 * Since this is an AsyncExecutingTask, You must call this method BEFORE passing control to another thread.
		 * @return
		 */
		@Override
		default TaskChain<?> getCurrentChain() {
			return currentChain.get();
		}

		@Override
		default R run(A input) throws AbortChainException {
			// unused
			return null;
		}

		void runAsync(A input, Consumer<R> next) throws AbortChainException;
	}

	public interface FirstTask<R> extends Task<R, Object> {
		@Override
		default R run(Object input) throws AbortChainException {
			return run();
		}

		R run() throws AbortChainException;
	}

	public interface AsyncExecutingFirstTask<R> extends AsyncExecutingTask<R, Object> {
		@Override
		default R run(Object input) throws AbortChainException {
			// Unused
			return null;
		}

		@Override
		default void runAsync(Object input, Consumer<R> next) throws AbortChainException {
			run(next);
		}

		void run(Consumer<R> next) throws AbortChainException;
	}

	public interface LastTask<A> extends Task<Object, A> {
		@Override
		default Object run(A input) throws AbortChainException {
			runLast(input);
			return null;
		}

		void runLast(A input) throws AbortChainException;
	}

	public interface GenericTask extends Task<Object, Object> {
		@Override
		default Object run(Object input) throws AbortChainException {
			runGeneric();
			return null;
		}

		void runGeneric() throws AbortChainException;
	}

	public interface AsyncExecutingGenericTask extends AsyncExecutingTask<Object, Object> {
		@Override
		default Object run(Object input) throws AbortChainException {
			return null;
		}

		@Override
		default void runAsync(Object input, Consumer<Object> next) throws AbortChainException {
			run(() -> next.accept(null));
		}

		void run(Runnable next) throws AbortChainException;
	}

	private static class SharedTaskChain<R> extends TaskChain<R> {
		private final TaskChain<R> backingChain;

		private SharedTaskChain(TaskChain<R> backingChain) {
			this.backingChain = backingChain;
		}

		@Override
		public void execute() {
			synchronized (backingChain) {
				// This executes SharedTaskChain.execute(Runnable), which says execute
				// my wrapped chains queue of events, but pass a done callback for when its done.
				// We then use the backing chain callback method to not execute the next task in the
				// backing chain until the current one is fully done.
				final SharedTaskChain<R> sharedChain = this;
				backingChain.currentCallback((AsyncExecutingGenericTask) sharedChain::execute);
				backingChain.execute();
			}
		}

		@Override
		public void execute(Runnable done) {
			this.doneCallback = done;
			execute0();
		}
	}
}