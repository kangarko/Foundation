package org.mineacademy.fo.collection;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

/**
 * Strict set that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public class StrictHashSet<E> extends AbstractStrictCollection implements StrictSet<E> {

	private static final Object PRESENT = new Object();

	private final StrictLinkedHashMap<E, Object> map;

	public StrictHashSet(E... elements) {
		this();

		setAll(Arrays.asList(elements));
	}

	public StrictHashSet(Collection<E> oldList) {
		this();

		setAll(oldList);
	}

	public StrictHashSet(Collection<E> oldList, String removeMsg, String addMsg) {
		this(removeMsg, addMsg);

		setAll(oldList);
	}

	public StrictHashSet() {
		this("Cannot remove '%s' as it is not in the set!", "Value '%s' is already in the set!");
	}

	public StrictHashSet(String removeMessage, String addMessage) {
		super(removeMessage, addMessage);
		this.map = new StrictLinkedHashMap<>(removeMessage, addMessage);
	}

	@Override public void setAll(Iterable<E> collection) {
		clear();

		this.addAll0(collection);
	}

	public boolean remove(Object value) {
		Valid.checkNotNull(value, "Cannot remove null values");
		final boolean removed = map.remove(value) != null;

		Valid.checkBoolean(removed, String.format(getCannotRemoveMessage(), value));
		return removed;
	}

	public boolean add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!map.containsKey(key), String.format(getCannotAddMessage(), key));
		map.put(key, PRESENT);
		return true;
	}

	@Override public boolean addAll0(@NotNull final Iterable<? extends E> i) {
		for (E element : i) {
			add(element);
		}
		return true;
	}

	@Override public boolean addAll(@NotNull final Collection<? extends E> c) {
		for (E element : c) {
			 add(element);
		}
		return true;
	}

	@Override public boolean removeAll(@NotNull final Collection<?> c) {
		boolean modified = false;
		for (Object o : c) {
			boolean b = remove(o);
			if (!modified)
				modified = b;
		}
		return modified;
	}

	@Override public boolean retainAll(@NotNull final Collection<?> c) {
		Iterator<E> i = iterator();
		boolean modified = false;
		while (i.hasNext()) {
			if (!c.contains(i.next())) {
				i.remove();
				modified = true;
			}
		}
		return modified;
	}

	public boolean contains(Object key) {
		return map.containsKey(key);
	}

	@Override public boolean containsAll(@NotNull final Collection<?> c) {
		return map.keySet().containsAll(c);
	}

	public E getAt(int index) {
		int i = 0;

		for (final E e : map.keySet()) {
			if (i++ == index)
				return e;
		}

		throw new FoException("Index (" + index + ") + out of size (" + map.size() + ")");
	}

	public void clear() {
		map.clear();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	public Set<E> getSource() {
		return map.keySet();
	}

	@Override @NotNull
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	public <T> T[] toArray(T[] e) {
		return map.keySet().toArray(e);
	}

	@NotNull @Override public Object[] toArray() {
		return map.keySet().toArray();
	}

	@Override
	public Object serialize() {
		return SerializeUtil.serializeList(new ArrayList<>(this));
	}

	@Override
	public String toString() {
		return "StrictSet{\n" + StringUtils.join(map.keySet(), "\n") + "}";
	}
}
