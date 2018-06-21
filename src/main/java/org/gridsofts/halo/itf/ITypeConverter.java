/* 
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.itf;

/**
 * 类型转换器接口
 * 
 * @author Lei
 */
public interface ITypeConverter {
	
	public boolean accept(Object value, Class<?> targetCls);

	public Object convert(Object value);
}
