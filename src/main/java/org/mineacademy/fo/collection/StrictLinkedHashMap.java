package org.mineacademy.fo.collection;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * Strict map implementation which extends LinkedHashMap.
 */
public final class StrictLinkedHashMap<E, V> extends LinkedHashMap<E, V> implements
	StrictMap<E, V> {

	private final String cannotRemoveMessage, cannotAddMessage;
	private final Map<V, E> inverse = new LinkedHashMap<>();

	public StrictLinkedHashMap() {
		this("Cannot remove '%s' as it is not in the map!", "Key '%s' is already in the map --> '%s'");
	}

	public StrictLinkedHashMap(String removeMessage, String addMessage) {
		this.cannotRemoveMessage = removeMessage;
		this.cannotAddMessage = addMessage;
	}

	public StrictLinkedHashMap(Map<E, V> copyOf) {
		this();
		setAll(copyOf);
	}

	@Override public void setAll(Map<E, V> copyOf) {
		clear();

		for (final Map.Entry<E, V> e : copyOf.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public V remove(Object key) {
		final V removed = removeWeak(key);
		Valid.checkNotNull(removed, String.format(cannotRemoveMessage, key));

		return removed;
	}

	@Override public void removeByValue(V value) {
		E key = inverse.get(value);
		if (key != null) {
			remove(value);
		} else {
			throw new NullPointerException(String.format(cannotRemoveMessage, value));
		}
	}

	@Override public E getKeyFromValue(V value) {
		return inverse.get(value);
	}

	@Override public V removeWeak(Object key) {
		inverse.values().remove(key);
		return super.remove(key);
	}

	@Override public Object[] removeAll(Collection<E> keys) {
		final List<V> removedKeys = new ArrayList<>();

		for (final E key : keys)
			removedKeys.add(remove(key));
		inverse.keySet().removeAll(removedKeys);
		return removedKeys.toArray();
	}

	@Override
	public V put(E key, V value) {
		Valid.checkBoolean(!containsKey(key), String.format(cannotAddMessage, key, get(key)));
		inverse.put(value, key);
		return super.put(key, value);
	}

	@Override public void override(E key, V value) {
		super.put(key, value);
	}

	@Override public void override(Map<? extends E, ? extends V> m) {
		super.putAll(m);
	}

	public boolean contains(E key) {
		return containsKey(key);
	}

	@Override public Set<E> keySet() {
		return new StrictKeySet<>(super.keySet());
	}

	public void putAll(Map<? extends E, ? extends V> m) {
		for (final Map.Entry<? extends E, ? extends V> e : m.entrySet())
			Valid.checkBoolean(!containsKey(e.getKey()), String.format(cannotAddMessage, e.getKey(), get(e.getKey())));
		super.putAll(m);
		for (final Map.Entry<? extends E, ? extends V> e : m.entrySet())
			inverse.put(e.getValue(), e.getKey());
	}

	@Override public V getOrPut(E key, V defaultToPut) {
		if (containsKey(key))
			return get(key);

		put(key, defaultToPut);
		return defaultToPut;
	}

	@Override @Deprecated
	/**
	 * @deprecated As this map now extends {@link LinkedHashMap}
	 */
	public Map<E, V> getSource() {
		return this;
	}

	@Override public E firstKey() {
		return isEmpty() ? null : super.keySet().iterator().next();
	}

	@Override public void forEachIterate(BiConsumer<E, V> consumer) {
		for (final Entry<E, V> entry : entrySet()) {
			consumer.accept(entry.getKey(), entry.getValue());
		}
	}


	@Override
	public Object serialize() {
		if (!isEmpty()) {
			final Map<Object, Object> copy = new HashMap<>();

			for (final Entry<E, V> e : entrySet()) {
				final V val = e.getValue();

				if (val != null)
					copy.put(SerializeUtil.serialize(e.getKey()), SerializeUtil.serialize(val));
			}

			return copy;
		}

		return this;
	}

	class StrictKeySet<K> extends AbstractSet<K> implements StrictCollection {

		private final Set<K> set;

		StrictKeySet(Set<K> set) {
			this.set = set;
		}

		@Override @NotNull public Iterator<K> iterator() {
			return set.iterator();
		}

		@Override public boolean remove(final Object value) {
			if (!contains(value)) {
				throw new NullPointerException(String.format(cannotRemoveMessage, value));
			}
			return super.remove(value);
		}

		@Override public int size() {
			return set.size();
		}

		@Override public Object serialize() {
			if (size() == 0) {
				return this;
			}
			return set.stream().map(SerializeUtil::serialize).collect(Collectors.toCollection(HashSet::new));
		}
	}
}
