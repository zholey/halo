package org.gridsofts.halo.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 将字段标记为“禁止修改”
 * 
 * @author lei
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface DontModify {

}
