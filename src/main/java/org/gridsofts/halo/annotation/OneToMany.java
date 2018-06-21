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
 * 用来表示“一对多”的映射关联关系
 * 
 * @author Lei
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {

	// 常量表
	public static final String ASC = "asc";
	public static final String DESC = "desc";

	/**
	 * 元素类型
	 * 
	 * @return
	 */
	Class<?> elementType();

	/**
	 * 外键
	 * 
	 * @return
	 */
	String[] foreignKey();

	/**
	 * 附加的关联条件
	 * 
	 * @return
	 */
	String condition() default "";

	/**
	 * 排序字段
	 * 
	 * @return
	 */
	String orderBy() default "";

	/**
	 * 排序顺序
	 * 
	 * @return
	 */
	String sort() default ASC;

	/**
	 * 延迟加载
	 * 
	 * @return
	 */
	boolean lazy() default false;
}
