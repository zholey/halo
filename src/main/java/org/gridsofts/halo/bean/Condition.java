/*
 * 版权所有 ©2011-2016 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 查询条件
 * 
 * @author lei
 */
public class Condition implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<Param> paramList = new ArrayList<>();
	
	public static Condition newInstance() {
		return new Condition();
	}
	
	public Condition put(Param param) {
		paramList.add(param);
		return this;
	}
	
	public Condition put(String paramName, Object paramValue) {
		paramList.add(new Param(paramName, paramValue));
		return this;
	}
	
	public Iterator<Param> iterator() {
		return paramList.iterator();
	}
	
	/**
	 * 条件参数；名值对
	 * 
	 * @author lei
	 */
	public static class Param implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public static enum Type {
			String, Number, DateTime
		}
		
		public static enum Association {
			Exact, Fuzzy
		}
		
		// 参数类型
		private Type type = Type.String;
		// 关联类型
		private Association association = Association.Exact;
		
		// 参数名；字段名
		private String name;
		// 参数值
		private Object value;
		
		public Param(Type type, String name, Object value, Association association) {
			this.type = type;
			this.name = name;
			this.value = value;
			this.association = association;
		}
		
		public Param(Type type, String name, Object value) {
			this(type, name, value, Association.Exact);
		}
		
		public Param(String name, Object value) {
			this(Type.String, name, value, Association.Exact);
		}

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public Association getAssociation() {
			return association;
		}

		public void setAssociation(Association association) {
			this.association = association;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
	}
}
