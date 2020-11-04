package org.mineacademy.fo.remain.nbt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.mineacademy.fo.exception.FoException;

/**
 * Abstract List implementation for ListCompounds
 *
 * @author tr7zw
 *
 * @param <T>
 */
public abstract class NBTList<T> implements List<T> {

	private final String listName;
	private final NBTCompound parent;
	private final NBTType type;
	protected Object listObject;

	protected NBTList(NBTCompound owner, String name, NBTType type, Object list) {
		parent = owner;
		listName = name;
		this.type = type;
		this.listObject = list;
	}

	/**
	 * @return Name of this list-compound
	 */
	public String getName() {
		return listName;
	}

	/**
	 * @return The Compound's parent Object
	 */
	public NBTCompound getParent() {
		return parent;
	}

	protected void save() {
		parent.set(listName, listObject);
	}

	protected abstract Object asTag(T object);

	@Override
	public boolean add(T element) {
		try {
			parent.getWriteLock().lock();
			if (WrapperVersion.getVersion().getVersionId() >= WrapperVersion.MC1_14_R1.getVersionId()) {
				WrapperReflection.LIST_ADD.run(listObject, size(), asTag(element));
			} else {
				WrapperReflection.LEGACY_LIST_ADD.run(listObject, asTag(element));
			}
			save();
			return true;
		} catch (final Exception ex) {
			throw new FoException(ex);
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public void add(int index, T element) {
		try {
			parent.getWriteLock().lock();
			if (WrapperVersion.getVersion().getVersionId() >= WrapperVersion.MC1_14_R1.getVersionId()) {
				WrapperReflection.LIST_ADD.run(listObject, index, asTag(element));
			} else {
				WrapperReflection.LEGACY_LIST_ADD.run(listObject, asTag(element));
			}
			save();
		} catch (final Exception ex) {
			throw new FoException(ex);
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public T set(int index, T element) {
		try {
			parent.getWriteLock().lock();
			final T prev = get(index);
			WrapperReflection.LIST_SET.run(listObject, index, asTag(element));
			save();
			return prev;
		} catch (final Exception ex) {
			throw new FoException(ex);
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public T remove(int i) {
		try {
			parent.getWriteLock().lock();
			final T old = get(i);
			WrapperReflection.LIST_REMOVE_KEY.run(listObject, i);
			save();
			return old;
		} catch (final Exception ex) {
			throw new FoException(ex);
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public int size() {
		try {
			parent.getReadLock().lock();
			return (int) WrapperReflection.LIST_SIZE.run(listObject);
		} catch (final Exception ex) {
			throw new FoException(ex);
		} finally {
			parent.getReadLock().unlock();
		}
	}

	/**
	 * @return The type that this list contains
	 */
	public NBTType getType() {
		return type;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public void clear() {
		while (!isEmpty()) {
			remove(0);
		}
	}

	@Override
	public boolean contains(Object o) {
		try {
			parent.getReadLock().lock();
			for (int i = 0; i < size(); i++) {
				if (o.equals(get(i)))
					return true;
			}
			return false;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public int indexOf(Object o) {
		try {
			parent.getReadLock().lock();
			for (int i = 0; i < size(); i++) {
				if (o.equals(get(i)))
					return i;
			}
			return -1;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		try {
			parent.getWriteLock().lock();
			final int size = size();
			for (final T ele : c) {
				add(ele);
			}
			return size != size();
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		try {
			parent.getWriteLock().lock();
			final int size = size();
			for (final T ele : c) {
				add(index++, ele);
			}
			return size != size();
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		try {
			parent.getReadLock().lock();
			for (final Object ele : c) {
				if (!contains(ele))
					return false;
			}
			return true;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public int lastIndexOf(Object o) {
		try {
			parent.getReadLock().lock();
			int index = -1;
			for (int i = 0; i < size(); i++) {
				if (o.equals(get(i)))
					index = i;
			}
			return index;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		try {
			parent.getWriteLock().lock();
			final int size = size();
			for (final Object obj : c) {
				remove(obj);
			}
			return size != size();
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		try {
			parent.getWriteLock().lock();
			final int size = size();
			for (final Object obj : c) {
				for (int i = 0; i < size(); i++) {
					if (!obj.equals(get(i))) {
						remove(i--);
					}
				}
			}
			return size != size();
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public boolean remove(Object o) {
		try {
			parent.getWriteLock().lock();
			final int size = size();
			int id = -1;
			while ((id = indexOf(o)) != -1) {
				remove(id);
			}
			return size != size();
		} finally {
			parent.getWriteLock().unlock();
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {

			private int index = -1;

			@Override
			public boolean hasNext() {
				return size() > index + 1;
			}

			@Override
			public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return get(++index);
			}

			@Override
			public void remove() {
				NBTList.this.remove(index);
				index--;
			}
		};
	}

	@Override
	public ListIterator<T> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<T> listIterator(int startIndex) {
		final NBTList<T> list = this;
		return new ListIterator<T>() {

			int index = startIndex - 1;

			@Override
			public void add(T e) {
				list.add(index, e);
			}

			@Override
			public boolean hasNext() {
				return size() > index + 1;
			}

			@Override
			public boolean hasPrevious() {
				return index >= 0 && index <= size();
			}

			@Override
			public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return get(++index);
			}

			@Override
			public int nextIndex() {
				return index + 1;
			}

			@Override
			public T previous() {
				if (!hasPrevious())
					throw new NoSuchElementException("Id: " + (index - 1));
				return get(index--);
			}

			@Override
			public int previousIndex() {
				return index - 1;
			}

			@Override
			public void remove() {
				list.remove(index);
				index--;
			}

			@Override
			public void set(T e) {
				list.set(index, e);
			}
		};
	}

	@Override
	public Object[] toArray() {
		try {
			parent.getReadLock().lock();
			final Object[] ar = new Object[size()];
			for (int i = 0; i < size(); i++)
				ar[i] = get(i);
			return ar;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public <E> E[] toArray(E[] a) {
		try {
			parent.getReadLock().lock();
			final E[] ar = Arrays.copyOf(a, size());
			Arrays.fill(ar, null);
			final Class<?> arrayclass = a.getClass().getComponentType();
			for (int i = 0; i < size(); i++) {
				final T obj = get(i);
				if (arrayclass.isInstance(obj)) {
					ar[i] = (E) get(i);
				} else {
					throw new ArrayStoreException("The array does not match the objects stored in the List.");
				}
			}
			return ar;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		try {
			parent.getReadLock().lock();
			final ArrayList<T> list = new ArrayList<>();
			for (int i = fromIndex; i < toIndex; i++)
				list.add(get(i));
			return list;
		} finally {
			parent.getReadLock().unlock();
		}
	}

	@Override
	public String toString() {
		try {
			parent.getReadLock().lock();
			return listObject.toString();
		} finally {
			parent.getReadLock().unlock();
		}
	}

}
