package org.mineacademy.fo.remain.nbt;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

class ProxiedList<E extends NBTProxy> implements ProxyList<E> {

	private final ReadWriteNBTCompoundList nbt;
	private final Class<E> proxy;

	public ProxiedList(ReadWriteNBTCompoundList nbt, Class<E> proxyClass) {
		this.nbt = nbt;
		this.proxy = proxyClass;
	}

	@Override
	public E get(int index) {
		final ReadWriteNBT tag = nbt.get(index);
		return new ProxyBuilder<>(tag, proxy).build();
	}

	@Override
	public int size() {
		return nbt.size();
	}

	@Override
	public void remove(int index) {
		nbt.remove(index);
	}

	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}

	@Override
	public E addCompound() {
		final ReadWriteNBT tag = nbt.addCompound();
		return new ProxyBuilder<>(tag, proxy).build();
	}

	@Override
	public boolean isEmpty() {
		return nbt.isEmpty();
	}

	private class Itr implements Iterator<E> {
		/**
		 * Index of element to be returned by subsequent call to next.
		 */
		int cursor = 0;

		/**
		 * Index of element returned by most recent call to next or
		 * previous.  Reset to -1 if this element is deleted by a call
		 * to remove.
		 */
		int lastRet = -1;

		@Override
		public boolean hasNext() {
			return cursor != size();
		}

		@Override
		public E next() {
			try {
				final int i = cursor;
				final E next = get(i);
				lastRet = i;
				cursor = i + 1;
				return next;
			} catch (final IndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			if (lastRet < 0)
				throw new IllegalStateException();

			try {
				ProxiedList.this.remove(lastRet);
				if (lastRet < cursor)
					cursor--;
				lastRet = -1;
			} catch (final IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

	}

}
