/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来表示“多对一”的映射关联关系
 * 
 * @author Lei
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToOne {

	/**
	 * 外键
	 * 
	 * @return
	 */
	String[] foreignKey();

	/**
	 * 延迟加载
	 * 
	 * @return
	 */
	boolean lazy() default false;
}
