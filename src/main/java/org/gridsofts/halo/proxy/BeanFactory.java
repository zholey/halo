/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.cglib.proxy.Enhancer;

import org.gridsofts.halo.AbstractDAO;
import org.gridsofts.halo.MetaInfo;
import org.gridsofts.halo.annotation.ManyToOne;
import org.gridsofts.halo.annotation.OneToMany;
import org.gridsofts.halo.annotation.Table;
import org.gridsofts.halo.exception.AnnotationException;
import org.gridsofts.halo.exception.DAOException;
import org.gridsofts.halo.itf.ITypeConverter;
import org.gridsofts.halo.util.BeanUtil;
import org.gridsofts.halo.util.StringUtil;

public class BeanFactory {

	private static final Pattern SimpleWordExp = Pattern.compile("^\\w+$");

	/**
	 * 根据给定的类信息及名值映射，构造Bean。<br/>
	 * 如果该类内配置了需要立即加载的关联Bean（或List），则在对Bean赋值后立即加载。
	 * 
	 * @param <T>
	 * @param dao
	 * @param t
	 * @param nameValueMap
	 *            存储数据名值映射
	 * @return
	 * @throws DAOException
	 */
	public static <T> T create(AbstractDAO dao, Class<T> t, Map<String, Object> nameValueMap)
			throws DAOException {

		MetaInfo tableAnnotationInfo = findTableAnnotation(t);
		if (tableAnnotationInfo == null) {
			throw new DAOException("该类未标注Table注解");
		}

		T bean = null;
		try {
			bean = t.newInstance();
		} catch (Exception e) {
			throw new DAOException("该类无法实例化，原始信息：" + e.getMessage());
		}

		if (bean == null) {
			throw new NullPointerException();
		}

		return fillBeanFields(dao, tableAnnotationInfo, bean, nameValueMap);
	}

	/**
	 * 根据给定的结果集信息，构造对应的JavaBean。
	 * 
	 * @param <T>
	 * @param dao
	 * @param t
	 * @param nameValueMap
	 *            存储数据名值映射
	 * @return
	 * @throws AnnotationException
	 */
	public static <T> T createProxyBean(AbstractDAO dao, Class<T> t, Map<String, Object> nameValueMap)
			throws AnnotationException {

		MetaInfo tableAnnotationInfo = findTableAnnotation(t);
		if (tableAnnotationInfo == null) {
			throw new DAOException("该类未标注Table注解");
		}

		// 创建代理
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(t);

		// 方法拦截器
		enhancer.setCallback(new LazyInterceptor(dao, t, tableAnnotationInfo.tableMetaInfo));

		T proxyBean = t.cast(enhancer.create());

		// 如果创建代理失败则直接返回
		if (proxyBean == null) {
			return null;
		}

		return fillBeanFields(dao, tableAnnotationInfo, proxyBean, nameValueMap);
	}

	/**
	 * 根据给定的Bean，构造对应的Clone Bean。
	 * 
	 * @param <T>
	 * @param t
	 * @param bean
	 * @return
	 * @throws DAOException
	 */
	public static <T> List<T> getCloneBean(Class<T> t, Object... beans) throws DAOException {

		List<T> list = new ArrayList<>();

		if (beans != null && beans.length > 0) {

			for (Object bean : beans) {
				list.add(getCloneBean(t, bean));
			}
		}

		return list;
	}

	/**
	 * 根据给定的Bean，构造对应的Clone Bean。
	 * 
	 * @param <T>
	 * @param t
	 * @param bean
	 * @return
	 * @throws DAOException
	 */
	public static <T> T getCloneBean(Class<T> t, Object bean) throws DAOException {

		T cloneBean = null;

		try {
			cloneBean = t.newInstance();
		} catch (Exception e) {
			throw new DAOException("该类无法实例化，原始信息：" + e.getMessage());
		}

		if (cloneBean == null) {
			throw new NullPointerException();
		}

		Field[] fields = t.getDeclaredFields();

		if (fields == null || fields.length == 0) {
			return cloneBean;
		}

		// 为clone bean的各字段赋值
		for (int i = 0, fieldCount = fields.length; i < fieldCount; i++) {
			String fieldName = fields[i].getName();

			// 跳过静态、常量字段
			if (BeanUtil.isConstField(fields[i])) {
				continue;
			}

			ManyToOne mtoAnnotation = fields[i].getAnnotation(ManyToOne.class);
			OneToMany otmAnnotation = fields[i].getAnnotation(OneToMany.class);

			// getter
			Method getterMethod = null;
			try {
				getterMethod = bean.getClass().getMethod(BeanUtil.getGetterMethodName(fieldName));
			} catch (Throwable e) {
			}

			// setter
			Method setterMethod = null;
			try {
				setterMethod = t.getMethod(BeanUtil.getSetterMethodName(fieldName),
						fields[i].getType());
			} catch (Throwable e) {
			}

			// 如果找不到指定字段的setter方法或getter方法，则忽略
			if (getterMethod == null || setterMethod == null) {
				continue;
			}

			// ManyToOne
			if (mtoAnnotation != null && !mtoAnnotation.lazy()) {
				try {
					setterMethod.invoke(cloneBean,
							getCloneBean(fields[i].getType(), getterMethod.invoke(bean)));
				} catch (Throwable e) {
				}
			}
			// OneToMany
			else if (otmAnnotation != null && !otmAnnotation.lazy()) {

				List<Object> cloneList = new ArrayList<>();

				List<?> list = null;

				try {
					list = (List<?>) getterMethod.invoke(bean);
				} catch (Throwable e) {
				}

				for (int j = 0; list != null && j < list.size(); j++) {
					cloneList.add(getCloneBean(otmAnnotation.elementType(), list.get(j)));
				}

				try {
					setterMethod.invoke(cloneBean, cloneList);
				} catch (Throwable e) {
				}
			}
			// Normal
			else {

				try {
					setterMethod.invoke(cloneBean, getterMethod.invoke(bean));
				} catch (Throwable e) {
				}
			}
		}

		return cloneBean;
	}

	/**
	 * 加载“一对多”集合
	 * 
	 * @param dao
	 * @param t
	 * @param primaryKeys
	 * @param otmField
	 * @param proxy
	 * @throws DAOException
	 */
	public static void loadOneToMany(AbstractDAO dao, Class<?> t, String[] primaryKeys, Field otmField,
			Object proxy) throws DAOException {

		OneToMany otmAnnotation = otmField.getAnnotation(OneToMany.class);

		Method otmFieldSetter = null;
		try {
			otmFieldSetter = t.getMethod(BeanUtil.getSetterMethodName(otmField.getName()),
					List.class);
		} catch (Exception e) {
			throw new DAOException("查找该类的setter方法时出现异常，原始信息：" + e.getMessage());
		}

		Class<?> type = otmAnnotation.elementType();
		String sort = otmAnnotation.sort();

		// 判断是否标注了与字段名不同的数据列名
		String[] foreignKeys = otmAnnotation.foreignKey();

		if (primaryKeys == null || primaryKeys.length == 0 || foreignKeys == null
				|| foreignKeys.length == 0) {
			throw new AnnotationException();
		}

		// 拼接SQL
		StringBuffer condition = null;

		for (String keyName : foreignKeys) {

			Field foreignKey = null;
			try {
				foreignKey = type.getDeclaredField(keyName);
			} catch (Exception e) {
				throw new DAOException("查找该类的字段“" + keyName + "”时出现异常，原始信息：" + e.getMessage());
			}

			String foreignName = BeanUtil.getColumnName(foreignKey);

			if (condition == null) {
				condition = new StringBuffer("where " + foreignName + " = ?");
			} else {
				condition.append(" and " + foreignName + " = ?");
			}
		}

		// 附加的关联条件
		if (!StringUtil.isNull(otmAnnotation.condition())) {
			condition.append(" and " + otmAnnotation.condition());
		}

		if (!StringUtil.isNull(otmAnnotation.orderBy())) {

			if (SimpleWordExp.matcher(otmAnnotation.orderBy()).find()) {

				// 判断是否标注了与字段名不同的数据列名
				Field orderBy = null;
				try {
					orderBy = type.getDeclaredField(otmAnnotation.orderBy());
				} catch (Exception e) {
					throw new DAOException("查找该类的排序字段“" + otmAnnotation.orderBy() + "”时出现异常，原始信息："
							+ e.getMessage());
				}

				String orderColName = BeanUtil.getColumnName(orderBy);

				condition.append(" order by " + orderColName + " " + sort);
			} else {

				condition.append(" order by " + otmAnnotation.orderBy());
			}
		}

		// 取值
		List<Object> priValue = new ArrayList<>();
		for (String keyName : primaryKeys) {

			Method priKeyGetter = null;
			try {
				priKeyGetter = t.getMethod(BeanUtil.getGetterMethodName(keyName));
			} catch (Exception e) {
				throw new DAOException("查找该类的getter方法时出现异常，原始信息：" + e.getMessage());
			}

			try {
				priValue.add(priKeyGetter.invoke(proxy));
			} catch (Exception e) {
				throw new DAOException("调用该类的getter方法时出现异常，原始信息：" + e.getMessage());
			}
		}

		try {
			otmFieldSetter.invoke(proxy, dao.list(type, condition.toString(), priValue.toArray()));
		} catch (Exception e) {
			throw new DAOException("查询“一对多”关联对象时出现异常，原始信息：" + e.getMessage());
		}
	}

	/**
	 * 加载“多对一”对象
	 * 
	 * @param dao
	 * @param t
	 * @param mtoField
	 * @param proxy
	 * @throws DAOException
	 */
	public static void loadManyToOne(AbstractDAO dao, Class<?> t, Field mtoField, Object proxy)
			throws DAOException {

		Class<?> type = mtoField.getType();

		Method mtoFieldSetter = null;
		try {
			mtoFieldSetter = t.getMethod(BeanUtil.getSetterMethodName(mtoField.getName()), type);
		} catch (Exception e) {
			throw new DAOException("查找该类的setter方法时出现异常，原始信息：" + e.getMessage());
		}

		ManyToOne mtoAnnotation = mtoField.getAnnotation(ManyToOne.class);
		String[] foreignKeys = mtoAnnotation.foreignKey();

		if (foreignKeys == null || foreignKeys.length == 0) {
			throw new AnnotationException();
		}

		// 取值
		List<Object> foreignValue = new ArrayList<>();
		for (String keyName : foreignKeys) {

			Method foreKeyGetter = null;
			try {
				foreKeyGetter = t.getMethod(BeanUtil.getGetterMethodName(keyName));
				foreignValue.add(foreKeyGetter.invoke(proxy));
			} catch (Exception e) {
				throw new DAOException("查找关调用该类的getter方法时出现异常，原始信息：" + e.getMessage());
			}
		}

		try {
			mtoFieldSetter.invoke(proxy, dao.find(type, foreignValue.toArray()));
		} catch (Exception e) {
			throw new DAOException("查询“多对一”关联对象时出现异常，原始信息：" + e.getMessage());
		}
	}

	/**
	 * 向上查找标注有Table注解的类
	 * 
	 * @param t
	 * @return
	 */
	private static <T> MetaInfo findTableAnnotation(Class<T> t) {

		if (t.getAnnotation(Table.class) != null) {
			MetaInfo tableAnnotationInfo = new MetaInfo();

			tableAnnotationInfo.clazz = t;
			tableAnnotationInfo.tableMetaInfo = t.getAnnotation(Table.class);

			return tableAnnotationInfo;
		}

		if (t.getSuperclass() != null) {
			return findTableAnnotation(t.getSuperclass());
		}

		return null;
	}

	/**
	 * 向上查找标注有Table注解的类
	 * 
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unused")
	private static <T> Class<? super T> findClassAnnotatedWithTable(Class<T> t) {

		if (t.getAnnotation(Table.class) != null) {
			return t;
		}

		if (t.getSuperclass() != null) {
			return findClassAnnotatedWithTable(t.getSuperclass());
		}

		return null;
	}

	/**
	 * 填充Bean的所有属性
	 * 
	 * @param dao
	 * @param bean
	 * @param nameValueMap
	 * @return
	 */
	private static <T> T fillBeanFields(AbstractDAO dao, MetaInfo metaInfo, T bean,
			Map<String, Object> nameValueMap) {

		Table tableAnnotation = metaInfo.tableMetaInfo;
		Field[] fields = metaInfo.clazz.getDeclaredFields();

		if (fields == null || fields.length == 0) {
			return bean;
		}

		// 取出OneToMany、ManyToOne标注
		List<Field> otmFieldAry = new ArrayList<>();
		List<Field> mtoFieldAry = new ArrayList<>();

		for (int i = 0, fieldCount = fields.length; i < fieldCount; i++) {
			OneToMany otmAnnotation = fields[i].getAnnotation(OneToMany.class);
			ManyToOne mtoAnnotation = fields[i].getAnnotation(ManyToOne.class);

			if (otmAnnotation != null) {
				otmFieldAry.add(fields[i]);
			}

			if (mtoAnnotation != null) {
				mtoFieldAry.add(fields[i]);
			}
		}

		// 为clone bean的各字段赋值
		for (int i = 0, fieldCount = fields.length; i < fieldCount; i++) {

			// 跳过静态、常量字段
			if (BeanUtil.isConstField(fields[i])) {
				continue;
			}

			String upperCaseColName = BeanUtil.getColumnName(fields[i]).toUpperCase();

			if (nameValueMap.containsKey(upperCaseColName)) {

				try {
					// setter
					String setterName = BeanUtil.getSetterMethodName(fields[i].getName());
					Method setterMethod = bean.getClass()
							.getMethod(setterName, fields[i].getType());

					// 如果找不到指定字段的setter方法，则忽略
					if (setterMethod == null) {
						continue;
					}

					Object fldValue = nameValueMap.get(upperCaseColName);
					ITypeConverter[] typeConverters = dao.getTypeConverters() == null ? null : dao
							.getTypeConverters().toArray(new ITypeConverter[0]);

					setterMethod.invoke(bean,
							BeanUtil.convert(fldValue, fields[i].getType(), typeConverters));
				} catch (Throwable e) {
				}
			}
		}

		// 需要立即加载的内容
		if (otmFieldAry != null) {
			Iterator<Field> iterator = otmFieldAry.iterator();

			while (iterator.hasNext()) {
				Field otmField = iterator.next();
				OneToMany otmAnnotation = otmField.getAnnotation(OneToMany.class);

				if (!otmAnnotation.lazy()) {
					try {
						loadOneToMany(dao, bean.getClass(), tableAnnotation.PrimaryKey(), otmField,
								bean);
					} catch (Throwable e) {
					}
				}
			}
		}
		if (mtoFieldAry != null) {
			Iterator<Field> iterator = mtoFieldAry.iterator();

			while (iterator.hasNext()) {
				Field mtoField = iterator.next();
				ManyToOne mtoAnnotation = mtoField.getAnnotation(ManyToOne.class);

				if (!mtoAnnotation.lazy()) {
					try {
						loadManyToOne(dao, bean.getClass(), mtoField, bean);
					} catch (Throwable e) {
					}
				}
			}
		}

		return bean;
	}
}
