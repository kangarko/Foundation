package org.mineacademy.fo.remain.nbt;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class ProxiedList<E extends NBTProxy> implements ProxyList<E> {

	private final ReadWriteNBTCompoundList nbt;
	private final Class<E> proxy;

	public ProxiedList(ReadWriteNBTCompoundList nbt, Class<E> proxyClass) {
		this.nbt = nbt;
		this.proxy = proxyClass;
	}

	@Override
	public E get(int index) {
		final ReadWriteNBT tag = this.nbt.get(index);
		return new ProxyBuilder<>(tag, this.proxy).build();
	}

	@Override
	public int size() {
		return this.nbt.size();
	}

	@Override
	public void remove(int index) {
		this.nbt.remove(index);
	}

	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}

	@Override
	public E addCompound() {
		final ReadWriteNBT tag = this.nbt.addCompound();
		return new ProxyBuilder<>(tag, this.proxy).build();
	}

	@Override
	public boolean isEmpty() {
		return this.nbt.isEmpty();
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
			return this.cursor != ProxiedList.this.size();
		}

		@Override
		public E next() {
			try {
				final int i = this.cursor;
				final E next = ProxiedList.this.get(i);
				this.lastRet = i;
				this.cursor = i + 1;
				return next;
			} catch (final IndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			if (this.lastRet < 0)
				throw new IllegalStateException();

			try {
				ProxiedList.this.remove(this.lastRet);
				if (this.lastRet < this.cursor)
					this.cursor--;
				this.lastRet = -1;
			} catch (final IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

	}

}
