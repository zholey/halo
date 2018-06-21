/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.bean;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.gridsofts.halo.util.StringUtil;

/**
 * 基于内存的结果集缓存，此结果集与数据库脱离关联而存在。
 * 
 * @author zholey
 * @version 2.2
 */
public class RSMemoryCache implements Serializable {
	private static final long serialVersionUID = 1L;

	private ArrayList<ArrayList<Object>> row;
	private ArrayList<Object> col;
	private ArrayList<String> colNameAry;

	private int rowPoint;

	/**
	 * 通过ResultSet结果集构造单页结果集。
	 * 
	 * @param rs
	 *            结果集
	 * @throws SQLException
	 */
	public RSMemoryCache(ResultSet rs) throws SQLException {

		if (rs == null) {
			throw new NullPointerException();
		}

		ResultSetMetaData rsmd = rs.getMetaData();

		row = new ArrayList<>();
		colNameAry = new ArrayList<>();
		rowPoint = -1;

		int colCount = rsmd.getColumnCount();
		for (int i = 1; i <= colCount; i++) {
			this.colNameAry.add(rsmd.getColumnLabel(i).toUpperCase());
		}

		while (rs.next()) {
			this.col = new ArrayList<>();
			for (int i = 1; i <= colCount; i++) {
				this.col.add(rs.getObject(i));
			}
			this.row.add(this.col);
		}
	}

	/**
	 * @return 当前行记录ID
	 */
	public int getRowNum() {
		return rowPoint + 1;
	}

	/**
	 * @return 总记录行数
	 */
	public int getLength() {
		if (row == null) {
			return 0;
		}
		return row.size();
	}

	/**
	 * 返回总列数。
	 */
	public int getColumns() {
		if (colNameAry == null) {
			return 0;
		}
		return colNameAry.size();
	}

	/**
	 * 返回指定列的字段名。
	 * 
	 * @param index
	 *            指定列索引，从0开始
	 * @return 字段名
	 */
	public String getColName(int index) {
		if (colNameAry == null || index < 0 || index >= getColumns()) {
			return null;
		}
		return String.valueOf(colNameAry.get(index));
	}

	/**
	 * 将行记录指针指向结果集开始处。
	 */
	public void beforeFirst() {
		rowPoint = -1;
	}

	/**
	 * 将行记录指针指向指定位置。
	 * 
	 * @param rowPoint
	 *            指定位置
	 */
	public void seek(int rowPoint) {
		if (rowPoint < -1 || rowPoint >= getLength()) {
			return;
		}
		this.rowPoint = rowPoint;
	}

	/**
	 * 结果集指针向下移动一行。
	 * 
	 * @return 如果当前指针已指向最后一行，则返回假，否则返回真。
	 */
	public boolean next() {
		rowPoint++;
		return rowPoint >= row.size() ? false : true;
	}

	/**
	 * 获取当前行的值映射
	 * 
	 * @return
	 */
	public Map<String, Object> getRowValueMap() {

		Map<String, Object> valueMap = new HashMap<>();
		
		ArrayList<Object> temp = null;

		if (rowPoint >= 0 && rowPoint < row.size()) {
			temp = row.get(rowPoint);
		}

		if (temp != null && temp.size() > 0) {
			
			for (int i = 0, len = temp.size(); i < len; i++) {
				
				String name = colNameAry.get(i);
				Object value = temp.get(i);
				
				valueMap.put(name, value);
			}
		}
		
		return valueMap;
	}

	/**
	 * 返回当前行记录中的指定列的值，类型为Object。
	 * 
	 * @param index
	 *            指定列索引，从1开始
	 * @return 类型为Object的列值
	 */
	public Object getObject(int index) {
		ArrayList<Object> temp = null;

		if (rowPoint >= 0 && rowPoint < row.size()) {
			temp = row.get(rowPoint);
		}

		if (temp == null || temp.get(index - 1) == null || StringUtil.isNull(temp.get(index - 1).toString())) {
			return null;
		} else {
			return temp.get(index - 1);
		}
	}

	/**
	 * 通过列名，返回当前行记录中的指定列的值，类型为Object。
	 * 
	 * @param name
	 *            指定的列名称
	 * @return 类型为Object的列值
	 */
	public Object getObject(String name) {

		int index = colNameAry.indexOf(name.toUpperCase());

		if (index < 0) {
			return null;
		}

		ArrayList<Object> temp = null;

		if (rowPoint >= 0 && rowPoint < row.size()) {
			temp = row.get(rowPoint);
		}

		if (temp == null || temp.get(index) == null || StringUtil.isNull(temp.get(index).toString())) {
			return null;
		} else {
			return temp.get(index);
		}
	}

	/**
	 * 返回当前行记录中的指定列的值，类型为String。
	 * 
	 * @param index
	 *            指定列索引，从1开始
	 * @return 类型为String的列值
	 */
	public String getString(int index) {
		if (getObject(index) == null) {
			return null;
		}
		return String.valueOf(getObject(index));
	}

	/**
	 * 通过列名，返回当前行记录中的指定列的值，类型为String。
	 * 
	 * @param name
	 *            指定的列名称
	 * @return 类型为String的列值
	 */
	public String getString(String name) {
		if (getObject(name) == null) {
			return null;
		}
		return String.valueOf(getObject(name));
	}

	/**
	 * 返回当前行记录中的指定列的值，类型为int。
	 * 
	 * @param index
	 *            指定列索引，从1开始
	 * @return 类型为int的列值
	 */
	public int getInt(int index) {
		if (getString(index) == null) {
			return 0;
		}
		return Integer.valueOf(getString(index));
	}

	/**
	 * 通过列名，返回当前行记录中的指定列的值，类型为int。
	 * 
	 * @param name
	 *            指定的列名称
	 * @return 类型为int的列值
	 */
	public int getInt(String name) {
		if (getString(name) == null) {
			return 0;
		}
		return Integer.valueOf(getString(name));
	}

	/**
	 * 返回当前行记录中的指定列的值，类型为int。
	 * 
	 * @param index
	 *            指定列索引，从1开始
	 * @return 类型为int的列值
	 */
	public long getLong(int index) {
		if (getString(index) == null) {
			return 0;
		}
		return Long.valueOf(getString(index));
	}

	/**
	 * 通过列名，返回当前行记录中的指定列的值，类型为int。
	 * 
	 * @param name
	 *            指定的列名称
	 * @return 类型为int的列值
	 */
	public long getLong(String name) {
		if (getString(name) == null) {
			return 0;
		}
		return Long.valueOf(getString(name));
	}

	/**
	 * 返回当前行记录中的指定列的值，类型为float。
	 * 
	 * @param index
	 *            指定列索引，从1开始
	 * @return 类型为float的列值
	 */
	public float getFloat(int index) {
		if (getString(index) == null) {
			return 0;
		}
		return Float.valueOf(getString(index));
	}

	/**
	 * 通过列名，返回当前行记录中的指定列的值，类型为float。
	 * 
	 * @param name
	 *            指定的列名称
	 * @return 类型为float的列值
	 */
	public float getFloat(String name) {
		if (getString(name) == null) {
			return 0;
		}
		return Float.valueOf(getString(name));
	}

	/**
	 * 返回当前行记录中的指定列的值，类型为double。
	 * 
	 * @param index
	 *            指定列索引，从1开始
	 * @return 类型为double的列值
	 */
	public double getDouble(int index) {
		if (getString(index) == null) {
			return 0;
		}
		return Double.valueOf(getString(index));
	}

	/**
	 * 通过列名，返回当前行记录中的指定列的值，类型为double。
	 * 
	 * @param name
	 *            指定的列名称
	 * @return 类型为double的列值
	 */
	public double getDouble(String name) {
		if (getString(name) == null) {
			return 0;
		}
		return Double.valueOf(getString(name));
	}
}
