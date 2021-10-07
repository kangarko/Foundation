package org.mineacademy.fo.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mineacademy.fo.command.PermsCommand;

/**
 * Annotation used in the {@link PermsCommand} command, for example usage, see
 * https://github.com/kangarko/PluginTemplate/blob/main/src/main/java/org/mineacademy/template/model/Permissions.java
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface PermissionGroup {

	public String value() default "";
}
