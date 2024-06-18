package org.mineacademy.fo.remain.nbt;

import org.mineacademy.fo.exception.FoException;

/**
 * A generic {@link RuntimeException} that can be thrown by most methods in the
 * NBTAPI.
 *
 * @author tr7zw
 *
 */
public class NbtApiException extends FoException {

	/**
	 *
	 */
	private static final long serialVersionUID = -993309714559452334L;

	/**
	 *
	 */
	public NbtApiException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NbtApiException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public NbtApiException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public NbtApiException(Throwable cause) {
		super(cause == null ? null : cause.toString(), cause);
	}
}
