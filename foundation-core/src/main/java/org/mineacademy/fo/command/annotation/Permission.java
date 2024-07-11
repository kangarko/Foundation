package org.mineacademy.fo.command.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mineacademy.fo.command.PermsCommand;

/**
 * Annotation that the {@link PermsCommand} command scans for in your class
 * to generate permission messages.
 *
 * Example:
 * <pre>
 * public final class Permissions {
 *
 *   &#64;PermissionGroup("Permissions related to Boss commands.")
 *   public static final class Command {
 *
 *     &#64;Permission("Open Boss menu by clicking with a Boss egg or use /boss menu command.")
 *     public static final String MENU = "boss.command.menu";
 *   }
 * }
 * </pre>
 *
 * @see PermissionGroup
 * @see PermsCommand
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Permission {

	public String value() default "";

	public boolean def() default false;
}
