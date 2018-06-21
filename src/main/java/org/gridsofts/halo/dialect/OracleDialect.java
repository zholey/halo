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
import org.gridsofts.halo.exception.DAOException;
import org.gridsofts.halo.itf.IDialect;
import org.gridsofts.halo.util.BeanUtil;
import org.gridsofts.halo.util.StringUtil;

public class OracleDialect implements Serializable, IDialect {
	private static final long serialVersionUID = 1L;

	private Properties property;

	@Override
	public void setProperties(Properties property) {
		this.property = property;
	}

	@Override
	public boolean isBatchInsertSupport() {
		return false;
	}

	/**
	 * 从配置文件中读取该实体Bean的主键策略
	 * 
	 * @param bean
	 * @return
	 */
	private String getPkPolicy(Object bean) {
		return property.getProperty(bean.getClass().getName() + ".PKPolicy");
	}

	@Override
	public String getInsertSQL(List<Object> returnValues, List<String> keyColumnNames,
			MetaInfo metaInfo, String tableName, Object... beans) {

		StringBuffer sqlBuffer = new StringBuffer();

		Field[] fields = metaInfo.clazz.getDeclaredFields();
		if (fields == null || fields.length == 0 || beans == null || beans.length == 0) {
			throw new NullPointerException();
		}

		if (beans.length > 1) {
			throw new DAOException("Oracle暂不支持批量插入操作");
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
					&& metaInfo.tableMetaInfo.IsGenerateKeys()) {

				keyColumnNames.add(BeanUtil.getColumnName(fields[i]).toUpperCase());
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
				String colName = field.getName();

				// 主键列
				// 如果需要自动生成主键，则加入序列
				if (BeanUtil.isPrimaryField(metaInfo.tableMetaInfo, colName)
						&& metaInfo.tableMetaInfo.IsGenerateKeys() && property != null
						&& !StringUtil.isNull(getPkPolicy(beans[i]))) {

					sqlBuffer.append(getPkPolicy(beans[i]));
				} else {

					sqlBuffer.append("?");

					// 保存列值
					returnValues.add(BeanUtil.getFieldValue(beans[i], colName));
				}

				if (j < fldCount - 1) {
					sqlBuffer.append(", ");
				}
			}

			sqlBuffer.append(" ) ");

			if (i < beans.length - 1) {
				sqlBuffer.append(", ");
			}
		}

		return sqlBuffer.toString();
	}

	@Override
	public String getPageSQL(String sql, int start, int limit) {
		String pageSql = sql;

		if (SelectHead.matcher(sql).find() && start != -1 && limit != -1) {

			String rownoName = "R_" + System.currentTimeMillis();
			String tableName = "T_" + System.currentTimeMillis();

			pageSql = "SELECT * FROM (SELECT ROWNUM AS " + rownoName + ", " + tableName
					+ ".* FROM (" + sql + " ) " + tableName + ") WHERE 1 = 1 ";

			if (start >= 0) {
				pageSql += " AND " + rownoName + " > " + start;
			}

			if (limit >= 0) {
				pageSql += " AND " + rownoName + " <= " + (start + limit);
			}
		}

		return pageSql;
	}
}
