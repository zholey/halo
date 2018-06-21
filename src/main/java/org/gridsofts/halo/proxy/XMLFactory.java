/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.gridsofts.halo.annotation.ManyToOne;
import org.gridsofts.halo.annotation.OneToMany;
import org.gridsofts.halo.util.BeanUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 提供与XML相关的Bean处理方法
 * 
 * @author Lei
 * 
 */
public class XMLFactory {

	/**
	 * 根据给定的Bean，构造对应的XML Document。
	 * 
	 * @param <T>
	 * @param t
	 * @param bean
	 * @return
	 * @throws ParserConfigurationException
	 */
	public static <T> Document createDocument(Class<T> t, Object bean)
			throws ParserConfigurationException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDocument = dbf.newDocumentBuilder().newDocument();

		xmlDocument.appendChild(createElement(t, bean, xmlDocument));

		return xmlDocument;
	}

	/**
	 * 根据给定的Bean，构造对应的Element。
	 * 
	 * @param <T>
	 * @param t
	 * @param bean
	 * @param ownerDoc
	 * @return
	 */
	public static <T> Element createElement(Class<T> t, Object bean, Document doc) {

		if (t == null || bean == null) {
			return null;
		}

		Field[] fields = t.getDeclaredFields();

		// 创建节点
		Element element = doc.createElement(t.getName());

		// 为node创建属性
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

			// 如果找不到指定字段的getter方法，则忽略
			if (getterMethod == null) {
				continue;
			}

			// ManyToOne
			if (mtoAnnotation != null && !mtoAnnotation.lazy()) {
				try {
					element.appendChild(createElement(fields[i].getType(),
							getterMethod.invoke(bean), doc));
				} catch (Throwable e) {
				}
			}
			// OneToMany
			else if (otmAnnotation != null && !otmAnnotation.lazy()) {

				List<?> list = null;

				try {
					list = (List<?>) getterMethod.invoke(bean);
				} catch (Throwable e) {
				}

				if (list != null && list.size() > 0) {

					Element childElement = doc.createElement("children");
					element.appendChild(childElement);

					for (int j = 0; list != null && j < list.size(); j++) {
						childElement.appendChild(createElement(otmAnnotation.elementType(),
								list.get(j), doc));
					}
				}
			}
			// Normal
			else if (mtoAnnotation == null && otmAnnotation == null) {
				try {
					element.setAttribute(fieldName, getterMethod.invoke(bean).toString());
				} catch (Throwable e) {
				}
			}
		}

		return element;
	}
}
