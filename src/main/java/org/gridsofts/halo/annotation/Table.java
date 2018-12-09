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
 * 数据表标记。<br/>
 * 字段名称及意义：Name（表名）, PrimaryKey（主键字段）, IsGenerateKeys（是否配置为自动生成主键）<br/>
 * 注：PrimaryKey支持多主键
 * 
 * @author Lei
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

	String value();

	String[] primaryKey();

	boolean autoGenerateKeys() default true;
}
