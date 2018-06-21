/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.dialect;

import java.math.BigDecimal;

import org.gridsofts.halo.itf.ITypeConverter;

/**
 * 本类实现将BigDecimal类型转换为Integer
 * 
 * @author Lei
 */
public class BigDecimal2Double implements ITypeConverter {

	@Override
	public boolean accept(Object value, Class<?> targetCls) {

		if (value == null || targetCls == null) {
			return false;
		}

		return BigDecimal.class.isAssignableFrom(value.getClass())
				&& Double.class.isAssignableFrom(targetCls);
	}

	@Override
	public Double convert(Object value) {

		if (value == null) {
			return null;
		}

		return BigDecimal.class.cast(value).doubleValue();
	}
}
