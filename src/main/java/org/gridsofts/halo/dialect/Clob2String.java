/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.dialect;

import java.sql.Clob;
import java.sql.SQLException;

import org.gridsofts.halo.itf.ITypeConverter;

/**
 * 本类实现将BigDecimal类型转换为Integer
 * 
 * @author Lei
 */
public class Clob2String implements ITypeConverter {

	@Override
	public boolean accept(Object value, Class<?> targetCls) {

		if (value == null || targetCls == null) {
			return false;
		}

		return Clob.class.isAssignableFrom(value.getClass())
				&& String.class.isAssignableFrom(targetCls);
	}

	@Override
	public String convert(Object value) {

		if (value == null) {
			return null;
		}
		
		Clob clob = Clob.class.cast(value);

		try {
			return clob.getSubString(1L, (int) clob.length());
		} catch (SQLException e) {
		}
		
		return value.toString();
	}
}
