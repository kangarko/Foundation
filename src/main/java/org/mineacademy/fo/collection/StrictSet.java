package org.mineacademy.fo.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

/**
 * Strict set that only allows to remove elements that are contained within, or add elements that are not.
 *
 * Failing to do so results in an error, with optional error message.
 */
public class StrictSet<E> extends StrictCollection implements Iterable<E> {

	private final Set<E> list = new HashSet<>();

	public StrictSet(E... elements) {
		this();

		setAll(Arrays.asList(elements));
	}

	public StrictSet(Collection<E> oldList) {
		this();

		setAll(oldList);
	}

	public StrictSet(Collection<E> oldList, String removeMsg, String addMsg) {
		this(removeMsg, addMsg);

		setAll(oldList);
	}

	public StrictSet() {
		this("Cannot remove '%s' as it is not in the set!", "Value '%s' is already in the set!");
	}

	public StrictSet(String removeMessage, String addMessage) {
		super(removeMessage, addMessage);
	}

	public void setAll(Collection<E> collection) {
		clear();

		for (final E val : collection)
			add(val);
	}

	public void remove(E value) {
		Valid.checkNotNull(value, "Cannot remove null values");
		final boolean removed = list.remove(value);

		Valid.checkBoolean(removed, String.format(getCannotRemoveMessage(), value));
	}

	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!list.contains(key), String.format(getCannotAddMessage(), key));

		list.add(key);
	}

	public boolean contains(E key) {
		return list.contains(key);
	}

	public E getAt(int index) {
		int i = 0;

		final Iterator<E> it = list.iterator();

		while (it.hasNext()) {
			final E e = it.next();

			if (i++ == index)
				return e;
		}

		throw new FoException("Index (" + index + ") + out of size (" + list.size() + ")");
	}

	public void clear() {
		list.clear();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	public Set<E> getSource() {
		return list;
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	public E[] toArray(E[] e) {
		return list.toArray(e);
	}

	@Override
	public Object serialize() {
		return SerializeUtil.serializeList(list);
	}

	@Override
	public String toString() {
		return "StrictSet{\n" + StringUtils.join(list, "\n") + "}";
	}
}