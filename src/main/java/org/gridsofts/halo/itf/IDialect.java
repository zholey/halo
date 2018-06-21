/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.itf;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.gridsofts.halo.MetaInfo;

public interface IDialect {

	public static final Pattern SelectHead = Pattern.compile("^select", Pattern.CASE_INSENSITIVE);

	public void setProperties(Properties property);

	public boolean isBatchInsertSupport();

	public String getInsertSQL(List<Object> returnValues, List<String> keyColumnNames,
			MetaInfo metaInfo, String tableName, Object... beans);

	public String getPageSQL(String sql, int start, int limit);
}
