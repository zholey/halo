/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gridsofts.halo.annotation.Table;
import org.gridsofts.halo.exception.AnnotationException;
import org.gridsofts.halo.util.BeanUtil;

public class MetaInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	public Class<?> clazz;
	public Table tableMetaInfo;
	public List<Field> primaryKeys;
	public List<Field> fields;

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
		
		metaInfo.fields = Arrays.asList(BeanUtil.getDeclaredFields(clazz, true));

		// 如果字段为空，则抛出异常
		if (metaInfo.fields == null || metaInfo.fields.isEmpty()) {
			throw new AnnotationException("未定义列信息");
		}
		
		// 过滤出所有非静态/常量/瞬态字段
		metaInfo.fields = metaInfo.fields.stream().filter(field -> {
			return !BeanUtil.isConstField(field) && !BeanUtil.isTransient(field);
		}).collect(Collectors.toList());

		// 如果字段为空，则抛出异常
		if (metaInfo.fields == null || metaInfo.fields.isEmpty()) {
			throw new AnnotationException("未标注列信息");
		}

		// 如果类未标注主键，则抛出异常
		if (metaTable.primaryKey() == null || metaTable.primaryKey().length == 0) {
			throw new AnnotationException("未标注主键信息");
		}

		// 取主键
		List<String> primaryKeys = Arrays.asList(metaTable.primaryKey());
		
		metaInfo.primaryKeys = metaInfo.fields.stream().filter(field -> {
			return primaryKeys.contains(field.getName());
		}).collect(Collectors.toList());

		return metaInfo;
	}
}
