package org.mineacademy.fo.collection.expiringmap;

import java.util.concurrent.TimeUnit;

/**
 * A value which should be stored in an {@link ExpiringMap} with optional control over its expiration.
 *
 * @param <V> the type of value being stored
 */
public final class ExpiringValue<V> {
	private static final long UNSET_DURATION = -1L;
	private final V value;
	private final ExpirationPolicy expirationPolicy;
	private final long duration;
	private final TimeUnit timeUnit;

	/**
	 * Creates an ExpiringValue to be stored in an {@link ExpiringMap}. The map's default values for
	 * {@link ExpirationPolicy expiration policy} and {@link ExpiringMap#getExpiration()} expiration} will be used.
	 *
	 * @param value the value to store
	 * @see ExpiringMap#put(Object, Object)
	 */
	public ExpiringValue(V value) {
		this(value, UNSET_DURATION, null, null);
	}

	/**
	 * Creates an ExpiringValue to be stored in an {@link ExpiringMap}. The map's default
	 * {@link ExpiringMap#getExpiration()} expiration} will be used.
	 *
	 * @param value            the value to store
	 * @param expirationPolicy the expiration policy for the value
	 * @see ExpiringMap#put(Object, Object, ExpirationPolicy)
	 */
	public ExpiringValue(V value, ExpirationPolicy expirationPolicy) {
		this(value, UNSET_DURATION, null, expirationPolicy);
	}

	/**
	 * Creates an ExpiringValue to be stored in an {@link ExpiringMap}. The map's default {@link ExpirationPolicy
	 * expiration policy} will be used.
	 *
	 * @param value    the value to store
	 * @param duration the length of time after an entry is created that it should be removed
	 * @param timeUnit the unit that {@code duration} is expressed in
	 * @throws NullPointerException on null timeUnit
	 * @see ExpiringMap#put(Object, Object, long, TimeUnit)
	 */
	public ExpiringValue(V value, long duration, TimeUnit timeUnit) {
		this(value, duration, timeUnit, null);
		if (timeUnit == null)
			throw new NullPointerException();
	}

	/**
	 * Creates an ExpiringValue to be stored in an {@link ExpiringMap}.
	 *
	 * @param value            the value to store
	 * @param duration         the length of time after an entry is created that it should be removed
	 * @param timeUnit         the unit that {@code duration} is expressed in
	 * @param expirationPolicy the expiration policy for the value
	 * @throws NullPointerException on null timeUnit
	 * @see ExpiringMap#put(Object, Object, ExpirationPolicy, long, TimeUnit)
	 */
	public ExpiringValue(V value, ExpirationPolicy expirationPolicy, long duration, TimeUnit timeUnit) {
		this(value, duration, timeUnit, expirationPolicy);
		if (timeUnit == null)
			throw new NullPointerException();
	}

	private ExpiringValue(V value, long duration, TimeUnit timeUnit, ExpirationPolicy expirationPolicy) {
		this.value = value;
		this.expirationPolicy = expirationPolicy;
		this.duration = duration;
		this.timeUnit = timeUnit;
	}

	public V getValue() {
		return this.value;
	}

	public ExpirationPolicy getExpirationPolicy() {
		return this.expirationPolicy;
	}

	public long getDuration() {
		return this.duration;
	}

	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	@Override
	public int hashCode() {
		return this.value != null ? this.value.hashCode() : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || this.getClass() != o.getClass())
			return false;

		final ExpiringValue<?> that = (ExpiringValue<?>) o;
		return !(this.value != null ? !this.value.equals(that.value) : that.value != null) && this.expirationPolicy == that.expirationPolicy && this.duration == that.duration && this.timeUnit == that.timeUnit;

	}

	@Override
	public String toString() {
		return "ExpiringValue{" + "value=" + this.value + ", expirationPolicy=" + this.expirationPolicy + ", duration=" + this.duration + ", timeUnit=" + this.timeUnit + '}';
	}
}
