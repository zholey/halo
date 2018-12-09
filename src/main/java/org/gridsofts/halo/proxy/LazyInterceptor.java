/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.proxy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.gridsofts.halo.AbstractDAO;
import org.gridsofts.halo.annotation.ManyToOne;
import org.gridsofts.halo.annotation.OneToMany;
import org.gridsofts.halo.annotation.Table;
import org.gridsofts.halo.util.BeanUtil;

public class LazyInterceptor implements MethodInterceptor, Serializable {
	private static final long serialVersionUID = 1L;

	private static final Pattern GetMethodExp = Pattern.compile("^get");

	private AbstractDAO dao;
	private Class<?> cls;

	private Table tableAnnotation;

	private List<Field> otmFieldAry;
	private List<Field> mtoFieldAry;

	public LazyInterceptor(AbstractDAO dao, Class<?> cls, Table tableAnnotation) {

		this.dao = dao;
		this.cls = cls;

		this.tableAnnotation = tableAnnotation;

		this.otmFieldAry = new ArrayList<>();
		this.mtoFieldAry = new ArrayList<>();

		// 取出OneToMany、ManyToOne标注
		Field[] fields = cls.getDeclaredFields();
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
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
			throws Throwable {

		// 过滤掉非以“get”开头的方法
		if (!GetMethodExp.matcher(method.getName()).find()) {
			return proxy.invokeSuper(obj, args);
		}

		boolean finded = false;

		if (otmFieldAry != null) {
			Iterator<Field> iterator = otmFieldAry.iterator();

			while (iterator.hasNext()) {
				Field otmField = iterator.next();
				OneToMany otmAnnotation = otmField.getAnnotation(OneToMany.class);

				String[] primaryKeys = tableAnnotation.primaryKey();

				if (otmAnnotation.lazy()
						&& BeanUtil.getGetterMethodName(otmField.getName())
								.equals(method.getName()) && proxy.invokeSuper(obj, args) == null) {

					// 为了提高运行效率，如果在一对多里找到了对应的字段，则不再进行查找
					finded = true;

					try {
						BeanFactory.loadOneToMany(dao, cls, primaryKeys, otmField, obj);
					} catch (Throwable e) {
					}
				}
			}
		}

		if (!finded && mtoFieldAry != null) {
			Iterator<Field> iterator = mtoFieldAry.iterator();

			while (iterator.hasNext()) {
				Field mtoField = iterator.next();
				ManyToOne mtoAnnotation = mtoField.getAnnotation(ManyToOne.class);

				if (mtoAnnotation.lazy()
						&& BeanUtil.getGetterMethodName(mtoField.getName())
								.equals(method.getName()) && proxy.invokeSuper(obj, args) == null) {

					try {
						BeanFactory.loadManyToOne(dao, cls, mtoField, obj);
					} catch (Throwable e) {
					}
				}
			}
		}

		return proxy.invokeSuper(obj, args);
	}
}
