package org.mineacademy.fo.menu.button.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;

/**
 * Place this {@link Position} over a menu {@link Button}
 * and we will render the button at the given position in
 * the {@link Menu} automatically.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Position {

	/**
	 * The slot number where the item should be placed.
	 *
	 * @return
	 */
	int value() default 0;

	/**
	 * The offset to the value. See {@link StartPosition}.
	 *
	 * @return
	 */
	StartPosition start() default StartPosition.TOP_LEFT;
}
