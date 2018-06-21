/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.itf;

import java.io.Serializable;

/**
 * 需要回写自动生成的主键的实体类，要实现此接口
 * 
 * @author lei
 */
public interface IWritebackKeys extends Serializable {
	
	public void setGeneratedKey(Object[] key);
}
