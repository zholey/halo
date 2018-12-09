/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.gridsofts.halo.annotation.Table;
import org.gridsofts.halo.exception.AnnotationException;

public class MetaInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	public Class<?> clazz;
	public Table tableMetaInfo;
	public List<Field> primaryKeys;

	/**
	 * 获取指定类型的标注信息组
	 * 
	 * @param t
	 * @return
	 * @throws AnnotationException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	public static MetaInfo get(Class<?> t) throws AnnotationException, SecurityException,
			NoSuchFieldException {

		MetaInfo metaInfo = new MetaInfo();

		Class<?> clazz = t;

		// 标注信息
		Table metaTable = null;

		metaTable = t.getAnnotation(Table.class);

		if (metaTable == null) {

			metaTable = t.getSuperclass().getAnnotation(Table.class);

			// 如果父类也未标注元数据，则抛出异常
			if (metaTable == null) {
				throw new AnnotationException("未找到Table标注");
			}

			clazz = t.getSuperclass();
		}

		// 记录相关内容
		metaInfo.clazz = clazz;
		metaInfo.tableMetaInfo = metaTable;

		// 取主键
		String[] primaryKeys = metaTable.primaryKey();

		// 如果类未标注主键，则抛出异常
		if (primaryKeys == null || primaryKeys.length == 0) {
			throw new AnnotationException("未标注主键信息");
		}

		List<Field> primaryKey = new ArrayList<>();
		for (String keyName : primaryKeys) {
			primaryKey.add(clazz.getDeclaredField(keyName));
		}

		// 记录相关内容
		metaInfo.primaryKeys = primaryKey;

		return metaInfo;
	}
}
