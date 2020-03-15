/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.dialect;

import java.sql.Blob;
import java.sql.SQLException;

import org.gridsofts.halo.itf.ITypeConverter;

/**
 * 本类实现将Blob类型转换为Byte[]
 * 
 * @author Lei
 */
public class Blob2ByteAry implements ITypeConverter {

	@Override
	public boolean accept(Object value, Class<?> targetCls) {

		if (value == null || targetCls == null) {
			return false;
		}

		return Blob.class.isAssignableFrom(value.getClass())
				&& byte[].class.isAssignableFrom(targetCls);
	}

	@Override
	public byte[] convert(Object value) {

		if (value == null) {
			return null;
		}
		
		Blob blob = Blob.class.cast(value);

		try {
			return blob.getBytes(1L, (int) blob.length());
		} catch (SQLException e) {
		}
		
		return null;
	}
}
