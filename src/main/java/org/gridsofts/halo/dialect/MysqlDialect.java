/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.dialect;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.gridsofts.halo.MetaInfo;
import org.gridsofts.halo.itf.IDialect;
import org.gridsofts.halo.util.BeanUtil;

public class MysqlDialect implements Serializable, IDialect {
	private static final long serialVersionUID = 1L;

	@Override
	public void setProperties(Properties property) {
	}

	@Override
	public boolean isBatchInsertSupport() {
		return true;
	}

	@Override
	public String getInsertSQL(List<Object> returnValues, List<String> keyColumnNames,
			MetaInfo metaInfo, String tableName, Object... beans) {
		
		StringBuffer sqlBuffer = new StringBuffer();

		Field[] fields = metaInfo.clazz.getDeclaredFields();

		if (fields == null || fields.length == 0 || beans == null || beans.length == 0) {
			throw new NullPointerException();
		}

		// 取出需要处理的字段
		List<Field> validFields = new ArrayList<>();
		for (int i = 0; i < fields.length; i++) {
			String colName = fields[i].getName();

			// 如果此字段不是数据表中的列，则跳过
			if (BeanUtil.isTransient(fields[i])) {
				continue;
			}

			// 主键列
			// 如果需要自动生成主键，则跳过
			if (BeanUtil.isPrimaryField(metaInfo.tableMetaInfo, colName)
					&& metaInfo.tableMetaInfo.autoGenerateKeys()) {
				
				keyColumnNames.add(BeanUtil.getColumnName(fields[i]).toUpperCase());

				continue;
			}

			validFields.add(fields[i]);
		}

		// 拼接插入语句
		sqlBuffer.append("INSERT INTO " + tableName + " (");

		for (int i = 0, fldCount = validFields.size(); i < fldCount; i++) {
			Field field = validFields.get(i);

			sqlBuffer.append(BeanUtil.getColumnName(field).toUpperCase());

			if (i < fldCount - 1) {
				sqlBuffer.append(", ");
			}
		}

		sqlBuffer.append(") VALUES ");

		// 拼接插入值
		for (int i = 0; i < beans.length; i++) {

			sqlBuffer.append(" ( ");

			for (int j = 0, fldCount = validFields.size(); j < fldCount; j++) {
				Field field = validFields.get(j);

				sqlBuffer.append("?");

				// 保存列值
				returnValues.add(BeanUtil.getFieldValue(beans[i], field.getName()));

				if (j < fldCount - 1) {
					sqlBuffer.append(", ");
				}
			}

			sqlBuffer.append(" ) ");

			if (i < beans.length - 1) {
				sqlBuffer.append(", ");
			}
		}

		sqlBuffer.append("; ");

		return sqlBuffer.toString();
	}

	@Override
	public synchronized String getPageSQL(String sql, int start, int limit) {

		String pageSql = sql;

		if (start < 0) {
			start = 0;
		}

		if (limit > 0) {
			pageSql += " LIMIT " + start + ", " + limit;
		}

		return pageSql;
	}
}
