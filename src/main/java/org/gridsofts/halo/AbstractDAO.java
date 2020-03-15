/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gridsofts.halo.exception.ConnectionException;
import org.gridsofts.halo.exception.DAOException;
import org.gridsofts.halo.itf.IConnectionFactory;
import org.gridsofts.halo.itf.IDialect;
import org.gridsofts.halo.itf.ITypeConverter;
import org.gridsofts.halo.proxy.BeanFactory;
import org.gridsofts.halo.util.StringUtil;

/**
 * DAO基类，用于实现一些与 Connection & transaction 有关的方法<br>
 * <br>
 * 
 * 注意：<br>
 * 手动事务控制时，事务之间不能互相嵌套。<br>
 * 
 * 对连接池的实现在外部完成
 * 
 * @author Lei
 */
public abstract class AbstractDAO implements Serializable {
	private static final long serialVersionUID = 1L;

	protected IDialect dialect = null;
	protected List<ITypeConverter> typeConverters = null;

	// 标识事务状态（True表示已经手动开启，False表示未手动开启）
	protected boolean transactionOpenState = false;
	
	// 标识级联状态（True表示自动查询级联数据，False表示不级联）
	protected boolean cascading = false;

	protected IConnectionFactory factory;
	protected Connection conn;

	protected AbstractDAO() {
		this(null);
	}

	protected AbstractDAO(IConnectionFactory factory) {
		this.factory = factory;

		// 加载配置信息（如果有）
		Properties property = new Properties();
		try {
			property.load(AbstractDAO.class.getResourceAsStream("/halo.properties"));

			if (property.containsKey("dialect")) {
				Class<?> dialectCls = Class.forName(property.getProperty("dialect"));

				dialect = (IDialect) dialectCls.newInstance();
				dialect.setProperties(property);
			}

			if (property.containsKey("typeConverter")
					&& !StringUtil.isNull(property.getProperty("typeConverter"))) {

				String[] converterTypes = property.getProperty("typeConverter").trim()
						.split("(?m)\\s*,\\s*");

				if (converterTypes != null && converterTypes.length > 0) {

					for (String type : converterTypes) {
						Class<?> cls = Class.forName(type.trim());

						if (cls != null && ITypeConverter.class.isAssignableFrom(cls)) {
							addTypeConverter(ITypeConverter.class.cast(cls.newInstance()));
						}
					}
				}
			}
		} catch (Throwable e) {
		}
	}
	
	/**
	 * 获取注册于该DAO的所有类型转换器
	 * 
	 * @return 类型转换器列表
	 */
	public List<ITypeConverter> getTypeConverters() {
		return typeConverters;
	}
	
	/**
	 * 重置该DAO的类型转换器
	 * 
	 * @param typeConverters
	 *            类型转换器列表
	 */
	public void setTypeConverters(List<ITypeConverter> typeConverters) {
		this.typeConverters = typeConverters;
	}
	
	/**
	 * 向该DAO注册类型转换器
	 * 
	 * @param typeConverter
	 *            准备注册的类型转换器
	 */
	public void addTypeConverter(ITypeConverter typeConverter) {
		
		if (getTypeConverters() == null) {
			setTypeConverters(new ArrayList<ITypeConverter>());
		}
		
		getTypeConverters().add(typeConverter);
	}

	/**
	 * 允许注入不同的Factory
	 * 
	 * @param factory
	 */
	public synchronized void setFactory(IConnectionFactory factory) {
		this.factory = factory;
	}

	
	/**
	 * @return the cascading
	 */
	public boolean isCascading() {
		return cascading;
	}

	/**
	 * @param cascading the cascading to set
	 */
	public void setCascading(boolean cascading) {
		this.cascading = cascading;
	}

	/**
	 * 手动开启事务
	 * 
	 * @throws DAOException
	 */
	public synchronized void beginTransaction() throws DAOException {

		requestConnection();

		if (isConnectionValid() && !transactionOpenState) {

			try {
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				throw new DAOException("无法将该连接的自动提交模式设置为False，原始信息：" + e.getMessage());
			}

			transactionOpenState = true;
		} else {
			throw new ConnectionException("无法获取数据库连接，或事务已经开启！");
		}
	}

	/**
	 * 手动关闭事务（提交）
	 * 
	 * @throws DAOException
	 */
	public synchronized void endTransaction() throws DAOException {

		try {
			if (isConnectionValid() && transactionOpenState) {

				try {
					conn.commit();
					conn.setAutoCommit(true);
				} catch (SQLException e) {
					throw new DAOException("手动关闭事务时出现异常，原始信息：" + e.getMessage());
				}
			} else {
				throw new ConnectionException("无法获取数据库连接，或事务已经关闭！");
			}
		} finally {
			cleanTransaction();
		}
	}

	/**
	 * 回滚事务
	 * 
	 * @throws DAOException
	 */
	public synchronized void rollBackTransaction() throws DAOException {

		try {
			if (isConnectionValid() && transactionOpenState) {

				try {
					conn.rollback();
					conn.setAutoCommit(true);
				} catch (SQLException e) {
					throw new DAOException("回滚事务时出现异常，原始信息：" + e.getMessage());
				}
			} else {
				throw new ConnectionException("无法获取数据库连接，或事务已经关闭！");
			}
		} finally {
			cleanTransaction();
		}
	}

	/**
	 * 关闭事务，并关闭数据库连接
	 */
	protected synchronized void cleanTransaction() {

		transactionOpenState = false;

		if (isConnectionValid()) {
			factory.release(conn);
		}
	}

	/**
	 * 请求连接
	 * 
	 * @throws ConnectionException
	 */
	protected synchronized void requestConnection() throws ConnectionException {

		// 当连接无效，或未手动开启事务时请求连接
		if (!isConnectionValid() || !transactionOpenState) {
			conn = factory.getConnection();
		}

		if (conn == null) {
			throw new ConnectionException();
		}
	}

	/**
	 * 关闭连接
	 */
	protected synchronized void releaseConnection() {

		// 当连接有效，并且未手动开启事务时关闭连接
		if (isConnectionValid() && !transactionOpenState) {
			factory.release(conn);
		}
	}

	/**
	 * 测试连接的有效性
	 * 
	 * @return
	 */
	protected synchronized boolean isConnectionValid() {

		try {
			return conn != null && !conn.isClosed();
		} catch (Throwable e) {
		}

		return false;
	}

	/**
	 * 根据给定的类信息及名值映射，构造相应的Bean
	 * 
	 * @param <T>
	 * @param t
	 * @param nameValueMap
	 * @return
	 * @throws DAOException
	 */
	protected <T> T createBean(Class<T> t, Map<String, Object> nameValueMap) throws DAOException {
		
		try {
			if (cascading) {
				return BeanFactory.createProxyBean(this, t, nameValueMap);
			} else {
				return BeanFactory.create(this, t, nameValueMap);
			}
		} catch (Throwable e) {
			throw new DAOException("无法创建Bean实例");
		}
	}


	/**
	 * 根据主键，查询指定的Bean
	 * 
	 * @param <T>
	 * @param t
	 *            与准备要查询的表相映射的Bean的class对象
	 * @param key
	 *            任意数量的主键（至少一个）；如果有多个，需要与Table注解中指定的PrimaryKeys中的顺序保持一致
	 * @return 实际找到的Bean，如果未找到则返回null
	 * @throws DAOException
	 */
	public abstract <T> T find(Class<T> t, Object... key) throws DAOException;

	/**
	 * 获取总记录数； 该方法只是为用户在使用count查询时提供一点点便利，实际运行时自动在表名前添加 了select count(0)语句
	 * 
	 * @param <T>
	 * @param t
	 *            与准备要查询的表相映射的Bean的class对象
	 * @param condition
	 *            具体的查询条件，需要自行添加“Where”、“And”等关键词。<br>
	 *            根据实际需要还可以拼接其它合适的语句，如 order子句
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return 该SQL的返回结果中，第一行第一列的值
	 * @throws DAOException
	 */
	public abstract <T> long getTotalQuantity(Class<T> t, String condition, Object... param)
			throws DAOException;

	/**
	 * 获取指定的Bean所映射表的所有记录列表；
	 * 
	 * @param <T>
	 * @param t
	 *            List内将存放的Bean的class对象
	 * 
	 * @return 查询结果；根据SQL查询结果将每行记录创建为一个指定类型的对象； 注意：在与Bean属性对应时，使用的是SQL中AS子句指定的别名
	 *         ，如果未指定别名，则使用原始列名； 而Bean中的属性名称如果标注有Column注解
	 *         ，则使用该注解中声明的列名，否则使用Bean的属性名
	 * @throws DAOException
	 */
	public abstract <T> List<T> list(Class<T> t) throws DAOException;

	/**
	 * 获取符合条件的Bean列表；
	 * 
	 * @param <T>
	 * @param t
	 *            List内将存放的Bean的class对象
	 * @param condition
	 *            具体的查询条件，需要自行添加“Where”、“And”等关键词。<br>
	 *            根据实际需要还可以拼接其它合适的语句，如 order子句
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return 查询结果；根据SQL查询结果将每行记录创建为一个指定类型的对象； 注意：在与Bean属性对应时，使用的是SQL中AS子句指定的别名
	 *         ，如果未指定别名，则使用原始列名； 而Bean中的属性名称如果标注有Column注解
	 *         ，则使用该注解中声明的列名，否则使用Bean的属性名
	 * @throws DAOException
	 */
	public abstract <T> List<T> list(Class<T> t, String condition, Object... param) throws DAOException;

	/**
	 * 获取符合条件的Bean列表；支持分页查询
	 * 
	 * @param <T>
	 * @param t
	 *            List内将存放的Bean的class对象
	 * @param start
	 *            分页查询记录起始行数
	 * @param limit
	 *            分页查询记录数
	 * @param condition
	 *            具体的查询条件，需要自行添加“Where”、“And”等关键词。<br>
	 *            根据实际需要还可以拼接其它合适的语句，如 order子句
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return 查询结果；根据SQL查询结果将每行记录创建为一个指定类型的对象； 注意：在与Bean属性对应时，使用的是SQL中AS子句指定的别名
	 *         ，如果未指定别名，则使用原始列名； 而Bean中的属性名称如果标注有Column注解
	 *         ，则使用该注解中声明的列名，否则使用Bean的属性名
	 * @throws DAOException
	 */
	public abstract <T> List<T> list(Class<T> t, int start, int limit, String condition, Object... param)
			throws DAOException;

	/**
	 * 保存指定Bean(新增)
	 * 
	 * @param <T>
	 * @param t
	 *            准备要保存的Bean的class对象
	 * @param bean
	 *            准备要保存的Bean
	 * @return 实际影响的行记录数
	 * @throws DAOException
	 */
	public abstract <T> T save(Class<T> t, T bean) throws DAOException;

	/**
	 * 批量保存指定的Bean(新增)；注意：在需要同时保存多个同类型的Bean时，此方法的执行效率理论上要高于多次调用save方法。
	 * 
	 * @param <T>
	 * @param t
	 *            准备要保存的Bean的class对象
	 * @param beans
	 *            准备要保存的Bean，可以任意多个，但至少也要有一个
	 * @return 实际影响的行记录数
	 * @throws DAOException
	 */
	@SuppressWarnings("unchecked")
	public abstract <T> int batchSave(Class<T> t, T... beans) throws DAOException;

	/**
	 * 更新给定的Bean，更新过程不修改主键值。
	 * 
	 * @param <T>
	 * @param bean
	 *            准备要保存的Bean
	 * @return 实际影响的行记录数
	 * @throws DAOException
	 */
	public abstract <T> int update(T bean) throws DAOException;

	/**
	 * 保存给定的Bean。如果已经存在则更新，否则新增
	 * 
	 * @param <T>
	 * @param t
	 *            准备要保存的Bean的class对象
	 * @param bean
	 *            准备要保存的Bean
	 * @return 实际影响的行记录数
	 * @throws DAOException
	 */
	public abstract <T> int saveOrUpdate(Class<T> t, T bean) throws DAOException;

	/**
	 * 删除指定的Bean
	 * 
	 * @param <T>
	 * @param bean
	 *            准备删除的Bean
	 * @return 实际影响的行记录数
	 * @throws DAOException
	 */
	public abstract <T> int delete(T bean) throws DAOException;

	/**
	 * 删除该类型所映射到的表内的全部记录
	 * 
	 * @param <T>
	 * @param t
	 *            与准备要清空的表相映射的Bean的class对象
	 * @return 实际影响的行记录数
	 * @throws DAOException
	 */
	public abstract <T> int deleteAll(Class<T> t) throws DAOException;

	/*************************************************************************************************/
	/**************** 以下方法为SQL支持 ***************************************************************/
	/*************************************************************************************************/

	/**
	 * 获取包含聚合函数的SQL返回值； 该方法只是为用户在使用count/max/min等聚合函数查询时提供一点点便利，
	 * 实际上executeUniqueQuery方法也可以达到类似的目的。
	 * 
	 * @param sql
	 *            准备执行查询的包含聚合函数的SQL，可以包含用“?”代表的参数；
	 *            注意：该SQL的返回结果中，只有第一行的第一列被当作本方法的返回值，其它行 /列均忽略
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return 该SQL的返回结果中，第一行第一列的值
	 * @throws DAOException
	 */
	public abstract long getUniqueValue(String sql, Object... param) throws DAOException;

	/**
	 * 执行SQL查询，返回结果对象；此方法期望给定的SQL只返回一行记录，如果结果不唯一则抛出异常
	 * 
	 * @param <T>
	 * @param t
	 *            期望返回的Bean的class对象
	 * @param sql
	 *            准备执行查询的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * 
	 * @return SQL查询结果；根据SQL查询结果将每行记录创建为一个指定类型的对象；
	 *         注意：在与Bean属性对应时，使用的是SQL中AS子句指定的别名 ，如果未指定别名，则使用原始列名；
	 *         而Bean中的属性名称如果标注有Column注解 ，则使用该注解中声明的列名，否则使用Bean的属性名
	 * @throws DAOException
	 */
	public abstract <T> T executeUniqueQuery(Class<T> t, String sql, Object... param) throws DAOException;

	/**
	 * 执行SQL查询，返回结果列表；
	 * 
	 * @param <T>
	 * @param t
	 *            List内将存放的Bean的class对象
	 * @param sql
	 *            准备执行查询的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * 
	 * @return SQL查询结果；根据SQL查询结果将每行记录创建为一个指定类型的对象；
	 *         注意：在与Bean属性对应时，使用的是SQL中AS子句指定的别名 ，如果未指定别名，则使用原始列名；
	 *         而Bean中的属性名称如果标注有Column注解 ，则使用该注解中声明的列名，否则使用Bean的属性名
	 * @throws DAOException
	 */
	public abstract <T> List<T> executeQuery(Class<T> t, String sql, Object... param) throws DAOException;

	/**
	 * 执行SQL查询，返回结果列表；支持分页查询
	 * 
	 * @param <T>
	 * @param t
	 *            List内将存放的Bean的class对象
	 * @param start
	 *            分页查询记录起始行数
	 * @param limit
	 *            分页查询记录数
	 * @param sql
	 *            准备执行查询的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * 
	 * @return SQL查询结果；根据SQL查询结果将每行记录创建为一个指定类型的对象；
	 *         注意：在与Bean属性对应时，使用的是SQL中AS子句指定的别名 ，如果未指定别名，则使用原始列名；
	 *         而Bean中的属性名称如果标注有Column注解 ，则使用该注解中声明的列名，否则使用Bean的属性名
	 * @throws DAOException
	 */
	public abstract <T> List<T> executeQuery(Class<T> t, int start, int limit, String sql, Object... param)
			throws DAOException;

	/**
	 * 执行SQL修改操作，返回实际修改的行记录数
	 * 
	 * @param sql
	 *            准备执行修改操作的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return 修改操作（INSERT/UPDATE/DELETE）实际影响的行记录数
	 * @throws DAOException
	 */
	public abstract int executeUpdate(String sql, Object... param) throws DAOException;

	/**
	 * 执行SQL查询，返回结果。此方法期望给定的SQL只返回一行记录，如果结果不唯一则抛出异常
	 * 
	 * @param sql
	 *            准备执行查询的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return SQL查询结果； 其中Map的键值是SQL中AS子句指定的别名（全大写）， 如果未使用AS子句指定别名，则使用原始列名 （全大写）
	 * @throws DAOException
	 */
	public abstract Map<String, Object> executeUniqueQuery(String sql, Object... param) throws DAOException;

	/**
	 * 执行SQL查询，返回结果列表
	 * 
	 * @param sql
	 *            准备执行查询的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return SQL查询结果，以Map来保存每行记录； 其中Map的键值是SQL中AS子句指定的别名（全大写），
	 *         如果未使用AS子句指定别名，则使用原始列名 （全大写）
	 * @throws DAOException
	 */
	public abstract List<Map<String, Object>> executeQuery(String sql, Object... param) throws DAOException;

	/**
	 * 执行SQL查询，返回结果列表；支持分页查询
	 * 
	 * @param start
	 *            分页查询记录起始行数
	 * @param limit
	 *            分页查询记录数
	 * @param sql
	 *            准备执行查询的SQL，可以包含用“?”代表的参数
	 * @param param
	 *            任意数量的参数（如果有）
	 * @return SQL查询结果，以Map来保存每行记录； 其中Map的键值是SQL中AS子句指定的别名（全大写），
	 *         如果未使用AS子句指定别名，则使用原始列名 （全大写）
	 * @throws DAOException
	 */
	public abstract List<Map<String, Object>> executeQuery(int start, int limit, String sql, Object... param)
			throws DAOException;

}
