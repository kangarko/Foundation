package org.mineacademy.fo.model;

import com.google.common.collect.ForwardingQueue;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/**
 * Represents a simple limited queue only storing a handful of entries in
 *
 * @param <E>
 */
public final class LimitedQueue<E> extends ForwardingQueue<E> {

	/**
	 * The delegate queue
	 */
	private final Queue<E> delegate;

	/**
	 * The maximum queue size
	 */
	private final int capacity;

	/**
	 * Create a new limited queue with the given capacity
	 *
	 * @param capacity
	 */
	public LimitedQueue(final int capacity) {
		this.delegate = new ArrayDeque<>(capacity);
		this.capacity = capacity;
	}

	@Override
	protected Queue<E> delegate() {
		return delegate;
	}

	/**
	 * See {@link Queue#add(Object)} however if the queue is full, we call {@link Queue#poll()} first before adding
	 */
	@Override
	public boolean add(final E element) {
		if (size() >= capacity)
			delegate.poll();

		return delegate.add(element);
	}

	/**
	 * See {@link Queue#addAll(Collection)}
	 */
	@Override
	public boolean addAll(final Collection<? extends E> collection) {
		return standardAddAll(collection);
	}

	/**
	 * See {@link Queue#offer(Object)}
	 */
	@Override
	public boolean offer(final E o) {
		return standardOffer(o);
	}

}