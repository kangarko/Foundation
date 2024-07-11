package org.mineacademy.fo.model;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * Represents a simple limited queue only storing a handful of entries in
 *
 * @param <E>
 */
public final class LimitedQueue<E> implements Queue<E> {

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

	/**
	 * See {@link Queue#add(Object)} however if the queue is full, we call {@link Queue#poll()} first before adding
	 */
	@Override
	public boolean add(final E element) {
		if (this.size() >= this.capacity)
			this.delegate.poll();

		return this.delegate.add(element);
	}

	/**
	 * See {@link Queue#addAll(Collection)}
	 */

	@Override
	public boolean addAll(final Collection<? extends E> collection) {
		return this.delegate.addAll(collection);
	}

	/**
	 * See {@link Queue#offer(Object)}
	 */

	@Override
	public boolean offer(final E o) {
		return this.delegate.offer(o);
	}

	@Override
	public E poll() {
		return this.delegate.poll();
	}

	@Override
	public E remove() {
		return this.delegate.remove();
	}

	@Override
	public E peek() {
		return this.delegate.peek();
	}

	@Override
	public E element() {
		return this.delegate.element();
	}

	@Override
	public Iterator<E> iterator() {
		return this.delegate.iterator();
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return this.delegate.removeAll(collection);
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		return this.delegate.contains(object);
	}

	@Override
	public boolean remove(Object object) {
		return this.delegate.remove(object);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return this.delegate.containsAll(collection);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return this.delegate.retainAll(collection);
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public Object[] toArray() {
		return this.delegate.toArray();
	}

	@Override
	public <T extends Object> T[] toArray(T[] array) {
		return this.delegate.toArray(array);
	}
}