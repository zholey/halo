/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gridsofts.halo.annotation.Table;
import org.gridsofts.halo.bean.RSMemoryCache;
import org.gridsofts.halo.exception.DAOException;
import org.gridsofts.halo.itf.IConnectionFactory;
import org.gridsofts.halo.itf.IWritebackKeys;
import org.gridsofts.halo.util.BeanUtil;

/**
 * 数据访问对象，将数据操作转化为对对象的操作，可以方便编程人员，减少SQL工作量。<br/>
 * 通过本类相关方法返回的JavaBean为原生Bean（非代理Bean），不包含回调拦截方法。<br/>
 * 如果需要产生代理JavaBean，可以使用{@link CascaedDAO}。
 * 
 * @author Lei
 */
public class SuperDAO extends AbstractDAO {
	private static final long serialVersionUID = 1L;

	public SuperDAO() {
	}

	public SuperDAO(IConnectionFactory factory) {
		super(factory);
	}

	/**
	 * 获取表名。子类通过覆盖此方法，可以对表名做最后的处理
	 * 
	 * @param metaTable
	 * @return
	 */
	protected String getTableName(Table metaTable) {
		return metaTable.value();
	}

	@Override
	public synchronized <T> T find(Class<T> t, Object... key) throws DAOException {

		// 如果主键为空，则返回Null
		if (key == null || key.length == 0) {
			return null;
		}

		PreparedStatement stat = null;
		RSMemoryCache rs = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(t);
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			String tableName = getTableName(metaInfo.tableMetaInfo);

			// 拼接SQL
			StringBuffer sql = null;

			for (Field primaryKey : metaInfo.primaryKeys) {
				String priColName = BeanUtil.getColumnName(primaryKey);

				if (sql == null) {
					sql = new StringBuffer();

					sql.append("select * from " + tableName);
					sql.append(" where " + priColName + " = ? ");
				} else {
					sql.append(" and " + priColName + " = ? ");
				}
			}

			try {

				stat = conn.prepareStatement(sql.toString());

				// SQL赋值
				for (int i = 0; i < key.length; i++) {
					stat.setObject(i + 1, key[i]);
				}

				// 执行SQL
				rs = new RSMemoryCache(stat.executeQuery());

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		// 如果记录不唯一则返回NULL
		if (rs == null || rs.getLength() != 1) {
			return null;
		}

		// 构造Bean
		if (rs.next()) {
			return createBean(t, rs.getRowValueMap());
		}

		return null;
	}

	@Override
	public synchronized <T> long getTotalQuantity(Class<T> t, String condition, Object... param)
			throws DAOException {

		PreparedStatement stat = null;
		RSMemoryCache rs = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(t);
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			String tableName = getTableName(metaInfo.tableMetaInfo);

			// 拼接SQL
			StringBuffer sql = new StringBuffer();

			sql.append("select count(0) from " + tableName + " ");

			if (condition != null) {
				sql.append(condition);
			}

			try {

				stat = conn.prepareStatement(sql.toString());

				if (param != null) {
					for (int i = 0; i < param.length; i++) {
						stat.setObject(i + 1, param[i]);
					}
				}

				// 执行SQL
				rs = new RSMemoryCache(stat.executeQuery());

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		//
		if (rs != null && rs.next()) {
			return rs.getLong(1);
		}

		return 0;
	}

	@Override
	public synchronized <T> List<T> list(Class<T> t) throws DAOException {
		return list(t, -1, -1, null);
	}

	@Override
	public synchronized <T> List<T> list(Class<T> t, String condition, Object... param)
			throws DAOException {
		return list(t, -1, -1, condition, param);
	}

	@Override
	public synchronized <T> List<T> list(Class<T> t, int start, int limit, String condition,
			Object... param) throws DAOException {

		List<T> list = new ArrayList<>();

		PreparedStatement stat = null;
		RSMemoryCache rs = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(t);
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			String tableName = getTableName(metaInfo.tableMetaInfo);

			// 拼接SQL
			StringBuffer sql = new StringBuffer();

			sql.append("select " + tableName + ".* from " + tableName + " ");

			if (condition != null) {
				sql.append(condition);
			}

			// 查找方言
			String dialectSql = sql.toString();

			if (dialect != null) {
				dialectSql = dialect.getPageSQL(sql.toString(), start, limit);
			}

			try {

				stat = conn.prepareStatement(dialectSql);

				if (param != null) {
					for (int i = 0; i < param.length; i++) {
						stat.setObject(i + 1, param[i]);
					}
				}

				// 执行SQL
				rs = new RSMemoryCache(stat.executeQuery());

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		// 遍历结果集，构造对象
		while (rs != null && rs.next()) {
			list.add(createBean(t, rs.getRowValueMap()));
		}

		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T> int batchSave(Class<T> t, T... beans) throws DAOException {

		int uptRresult = 0;

		if (beans == null || beans.length == 0) {
			throw new NullPointerException();
		}

		PreparedStatement statement = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(t);
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			String tableName = getTableName(metaInfo.tableMetaInfo);

			try {
				List<Object> values = new ArrayList<>();
				List<String> keyColumnNames = new ArrayList<>();

				// 测试当前环境是否支持批量插入操作
				if (dialect.isBatchInsertSupport()) {
					values.clear();
					keyColumnNames.clear();

					String batchSQL = dialect.getInsertSQL(values, keyColumnNames, metaInfo,
							tableName, beans);

					try {
						if (keyColumnNames.size() > 0) {
							statement = conn.prepareStatement(batchSQL,
									keyColumnNames.toArray(new String[0]));
						} else {
							statement = conn.prepareStatement(batchSQL);
						}

						for (int i = 0, vLength = values.size(); i < vLength; i++) {
							statement.setObject(i + 1, values.get(i));
						}
					} catch (Throwable e) {
					}

					// 插入数据库
					uptRresult += statement.executeUpdate();
				} else {

					// 伪批量插入
					for (T bean : beans) {
						values.clear();
						keyColumnNames.clear();

						String batchSQL = dialect.getInsertSQL(values, keyColumnNames, metaInfo,
								tableName, bean);

						try {
							if (keyColumnNames.size() > 0) {
								statement = conn.prepareStatement(batchSQL,
										keyColumnNames.toArray(new String[0]));
							} else {
								statement = conn.prepareStatement(batchSQL);
							}

							for (int i = 0, vLength = values.size(); i < vLength; i++) {
								statement.setObject(i + 1, values.get(i));
							}
						} catch (Throwable e) {
						}

						// 插入数据库
						uptRresult += statement.executeUpdate();

						try {
							if (statement != null && !statement.isClosed()) {
								statement.close();
							}
						} catch (SQLException e) {
						}
					}
				}

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		return uptRresult;
	}

	@Override
	public synchronized <T> T save(Class<T> t, T bean) throws DAOException {

		if (bean == null) {
			throw new NullPointerException();
		}

		List<Object> priKeyValue = new ArrayList<>();

		T resultObject = bean;
		PreparedStatement statement = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(t);
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			String tableName = getTableName(metaInfo.tableMetaInfo);

			try {
				List<Object> values = new ArrayList<>();
				List<String> keyColumnNames = new ArrayList<>();

				String insertSQL = dialect.getInsertSQL(values, keyColumnNames, metaInfo,
						tableName, bean);

				try {
					if (keyColumnNames.size() > 0) {
						statement = conn.prepareStatement(insertSQL,
								keyColumnNames.toArray(new String[0]));
					} else {
						statement = conn.prepareStatement(insertSQL);
					}

					for (int i = 0, vLength = values.size(); i < vLength; i++) {
						statement.setObject(i + 1, values.get(i));
					}
				} catch (Throwable e) {
				}

				// 插入数据库
				statement.executeUpdate();

				// 准备返回自动生成的主键
				if (metaInfo.tableMetaInfo.autoGenerateKeys() && keyColumnNames.size() > 0) {

					priKeyValue.clear();

					ResultSet keys = statement.getGeneratedKeys();
					while (keys != null && keys.next()) {
						priKeyValue.add(keys.getObject(1));
					}

					// 回写自动生成的主键
					if (bean instanceof IWritebackKeys) {
						((IWritebackKeys) bean).setGeneratedKey(priKeyValue.toArray());
					}

					// 重新查找一次，以便创建代理
					resultObject = find(t, priKeyValue.toArray());
				}

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		return resultObject;
	}

	@Override
	public synchronized <T> int update(T bean) throws DAOException {

		int uptRresult = 0;

		PreparedStatement saveStat = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(bean.getClass());
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			Field[] fields = metaInfo.clazz.getDeclaredFields();
			if (fields == null || fields.length == 0) {
				throw new NullPointerException();
			}

			// 拼接SQL
			StringBuffer sql = new StringBuffer();

			StringBuffer uptParamSql = new StringBuffer("update "
					+ getTableName(metaInfo.tableMetaInfo) + " set ");
			List<Object> colValues = new ArrayList<>();

			// 拼接SQL
			for (int i = 0, count = fields.length; i < count; i++) {
				String colName = fields[i].getName();

				// 如果此字段不是数据表中的列，则跳过
				if (BeanUtil.isTransient(fields[i])) {
					continue;
				}

				// 跳过主键列的赋值，不允许修改主键值
				if (BeanUtil.isPrimaryField(metaInfo.tableMetaInfo, colName)) {
					continue;
				}

				//
				uptParamSql.append(BeanUtil.getColumnName(fields[i]) + " = ?, ");

				// 保存列值
				colValues.add(BeanUtil.getFieldValue(bean, colName));
			}

			sql.append(uptParamSql.toString().replaceAll(",\\s*$", " "));

			// 拼接 主键关联条件
			StringBuffer uptWhereSql = null;
			for (Field key : metaInfo.primaryKeys) {
				String colName = BeanUtil.getColumnName(key);

				if (uptWhereSql == null) {
					uptWhereSql = new StringBuffer(" where " + colName + " = ?");
				} else {
					uptWhereSql.append(" and " + colName + " = ?");
				}

				// 保存主键值
				colValues.add(BeanUtil.getFieldValue(bean, key.getName()));
			}

			sql.append(uptWhereSql.toString());

			try {

				// 修改记录
				saveStat = conn.prepareStatement(sql.toString());

				// 赋值
				for (int i = 0, count = colValues.size(); i < count; i++) {
					saveStat.setObject(i + 1, colValues.get(i));
				}

				uptRresult = saveStat.executeUpdate();

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		return uptRresult;
	}

	@Override
	public synchronized <T> int saveOrUpdate(Class<T> t, T bean) throws DAOException {

		int uptRresult = 0;

		// 获取元信息
		MetaInfo metaInfo = null;
		try {
			metaInfo = MetaInfo.get(t);
		} catch (Exception e) {
			throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
		}

		// 判断主键列值是否为空
		boolean keyNotNull = true;
		List<Object> primaryValue = new ArrayList<>();

		for (Field key : metaInfo.primaryKeys) {
			Object keyValue = BeanUtil.getFieldValue(bean, key.getName());

			if (keyValue == null) {
				keyNotNull = false;
				break;
			}

			primaryValue.add(keyValue);
		}

		if (keyNotNull && find(t, primaryValue.toArray()) != null) {
			uptRresult = update(bean);
		} else {

			if (save(t, bean) != null) {
				uptRresult = 1;
			}
		}

		return uptRresult;
	}

	@Override
	public synchronized <T> int deleteAll(Class<T> t) throws DAOException {

		int uptRresult = 0;

		PreparedStatement delStat = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(t);
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			try {

				delStat = conn.prepareStatement("delete from "
						+ getTableName(metaInfo.tableMetaInfo));

				uptRresult = delStat.executeUpdate();

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		return uptRresult;
	}

	@Override
	public synchronized <T> int delete(T bean) throws DAOException {

		int uptRresult = 0;

		PreparedStatement delStat = null;

		try {
			// 连接数据库
			requestConnection();

			// 获取元信息
			MetaInfo metaInfo = null;
			try {
				metaInfo = MetaInfo.get(bean.getClass());
			} catch (Exception e) {
				throw new DAOException("查找类描述元信息时出现异常，原始信息：" + e.getMessage());
			}

			String tableName = getTableName(metaInfo.tableMetaInfo);

			// 拼接SQL
			StringBuffer sql = new StringBuffer("delete from " + tableName);

			StringBuffer delWhereSql = null;
			List<Object> colValues = new ArrayList<>();

			for (Field key : metaInfo.primaryKeys) {
				String colName = BeanUtil.getColumnName(key);

				if (delWhereSql == null) {
					delWhereSql = new StringBuffer(" where " + colName + " = ? ");
				} else {
					delWhereSql.append(" and " + colName + " = ? ");
				}

				// 保存列值
				colValues.add(BeanUtil.getFieldValue(bean, key.getName()));
			}

			sql.append(delWhereSql.toString());

			try {

				delStat = conn.prepareStatement(sql.toString());

				// 赋值
				for (int i = 0, count = colValues.size(); i < count; i++) {
					delStat.setObject(i + 1, colValues.get(i));
				}

				uptRresult = delStat.executeUpdate();

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		return uptRresult;
	}
	
	
	@Override
	public long getUniqueValue(String sql, Object... param) throws DAOException {
		
		PreparedStatement stat = null;
		RSMemoryCache rs = null;

		try {
			// 连接数据库
			requestConnection();

			try {

				stat = conn.prepareStatement(sql.toString());

				if (param != null) {
					for (int i = 0; i < param.length; i++) {
						stat.setObject(i + 1, param[i]);
					}
				}

				// 执行SQL
				rs = new RSMemoryCache(stat.executeQuery());

			} catch (SQLException e) {
				throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
			}

		} finally {
			releaseConnection();
		}

		//
		if (rs != null && rs.next()) {
			return rs.getLong(1);
		}

		return 0;
	}

	@Override
	public synchronized <T> T executeUniqueQuery(Class<T> t, String sql, Object... param) throws DAOException {

		List<T> list = executeQuery(t, -1, -1, sql, param);

		if (list.size() > 1) {
			throw new DAOException("结果不唯一");
		}

		if (list.size() == 1) {
			return list.get(0);
		}

		return null;
	}

	@Override
	public synchronized <T> List<T> executeQuery(Class<T> t, String sql, Object... param) throws DAOException {
		return executeQuery(t, -1, -1, sql, param);
	}

	@Override
	public synchronized <T> List<T> executeQuery(Class<T> t, int start, int limit, String sql, Object... param)
			throws DAOException {

		List<T> list = new ArrayList<>();

		PreparedStatement stat = null;
		RSMemoryCache rs = null;

		requestConnection();

		try {

			// 查找方言
			String dialectSql = sql;

			if (dialect != null) {
				dialectSql = dialect.getPageSQL(dialectSql, start, limit);
			}

			stat = conn.prepareStatement(dialectSql);

			if (param != null) {
				for (int i = 0; i < param.length; i++) {
					stat.setObject(i + 1, param[i]);
				}
			}

			// 执行SQL
			rs = new RSMemoryCache(stat.executeQuery());

		} catch (SQLException e) {
			throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
		} finally {
			releaseConnection();
		}

		// 遍历结果集，构造对象
		while (rs != null && rs.next()) {
			list.add(createBean(t, rs.getRowValueMap()));
		}

		return list;
	}

	@Override
	public synchronized int executeUpdate(String sql, Object... param) throws DAOException {

		int uptRresult = 0;

		PreparedStatement stat = null;

		requestConnection();

		try {

			stat = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			if (param != null) {
				for (int i = 0; i < param.length; i++) {
					stat.setObject(i + 1, param[i]);
				}
			}

			uptRresult = stat.executeUpdate();

		} catch (SQLException e) {
			throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
		} finally {
			releaseConnection();
		}

		return uptRresult;
	}

	@Override
	public synchronized Map<String, Object> executeUniqueQuery(String sql, Object... param) throws DAOException {

		List<Map<String, Object>> list = executeQuery(-1, -1, sql, param);

		if (list.size() > 1) {
			throw new DAOException("结果不唯一");
		}

		if (list.size() == 1) {
			return list.get(0);
		}

		return null;
	}

	@Override
	public synchronized List<Map<String, Object>> executeQuery(String sql, Object... param) throws DAOException {
		return executeQuery(-1, -1, sql, param);
	}

	@Override
	public synchronized List<Map<String, Object>> executeQuery(int start, int limit, String sql, Object... param)
			throws DAOException {

		List<Map<String, Object>> list = new ArrayList<>();

		PreparedStatement stat = null;
		ResultSet rs = null;

		requestConnection();

		try {

			// 查找方言
			String dialectSql = sql;

			if (dialect != null) {
				dialectSql = dialect.getPageSQL(dialectSql, start, limit);
			}

			stat = conn.prepareStatement(dialectSql);

			if (param != null) {
				for (int i = 0; i < param.length; i++) {
					stat.setObject(i + 1, param[i]);
				}
			}

			// 执行SQL
			rs = stat.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();

			// 遍历结果集，构造Map
			while (rs != null && rs.next()) {
				list.add(BeanUtil.getEntityMap(rs, rsmd));
			}

		} catch (SQLException e) {
			throw new DAOException("执行数据库查询时出现异常，原始信息：" + e.getMessage());
		} finally {
			releaseConnection();
		}

		return list;
	}
}
