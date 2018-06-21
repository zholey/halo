/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.util;

import java.util.List;
import java.util.regex.Pattern;

public class StringUtil {

	private static final Pattern IsNullExp = Pattern.compile("(?i)^\\s*(null)?\\s*$");
	private static final Pattern IsEmptyExp = Pattern.compile("(?i)^\\s*$");

	private static final Pattern IsNumberExp = Pattern.compile("(?i)^\\s*-?(\\d+\\.)?\\d+\\s*$");
	private static final Pattern IsIntegerExp = Pattern.compile("(?i)^\\s*-?\\d+\\s*$");

	private static final Pattern IsTrueExp = Pattern.compile("(?i)^\\s*true\\s*$");
	private static final Pattern IsFalseExp = Pattern.compile("(?i)^\\s*false\\s*$");

	private static final Pattern XmlTagExp = Pattern.compile("<[^>]+>");

	private static final Pattern DateTimeExp = Pattern.compile("(?i)^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}$");
	private static final Pattern DateTimeExp2 = Pattern.compile("(?i)^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}$");
	private static final Pattern DateExp = Pattern.compile("(?i)^\\d{4}-\\d{2}-\\d{2}$");
	private static final Pattern TimeExp = Pattern.compile("(?i)^\\d{2}:\\d{2}:\\d{2}$");

	public static boolean isNull(String str) {

		if (str == null) {
			return true;
		}

		return IsNullExp.matcher(str).find();
	}

	public static boolean isEmpty(String str) {

		if (str == null) {
			return true;
		}

		return IsEmptyExp.matcher(str).find();
	}

	/**
	 * 测试给定的字符串是否为日期时间格式
	 * 
	 * @param str
	 * @return 返回小于0则表示不格式不匹配; </br>
	 *         0-'yyyy-MM-dd HH:mm:ss'; </br>
	 *         1-'yyyy-MM-dd'; </br>
	 *         2-'HH:mm:ss'; </br>
	 *         3-'yyyy-MM-dd HH:mm'
	 */
	public static int isDateTime(String str) {

		if (str == null) {
			return -1;
		}

		if (DateTimeExp.matcher(str).find()) {
			return 0;
		} else if (DateExp.matcher(str).find()) {
			return 1;
		} else if (TimeExp.matcher(str).find()) {
			return 2;
		} else if (DateTimeExp2.matcher(str).find()) {
			return 3;
		}

		return -1;
	}

	public static boolean isNumber(String str) {

		if (str == null) {
			return false;
		}

		return IsNumberExp.matcher(str).find();
	}

	public static boolean isInteger(String str) {

		if (str == null) {
			return false;
		}

		return IsIntegerExp.matcher(str).find();
	}

	public static boolean isTrue(String str) {

		if (str == null) {
			return false;
		}

		return IsTrueExp.matcher(str).find();
	}

	public static boolean isFalse(String str) {

		if (str == null) {
			return true;
		}

		return IsFalseExp.matcher(str).find();
	}

	/**
	 * 将指定的字符串进行字符集转换
	 * 
	 * @param str
	 * @param desCharset
	 * @return
	 */
	public static String convertCharSet(String str, String desCharset) {
		return convertCharSet(str, null, desCharset);
	}

	/**
	 * 将指定的字符串进行字符集转换
	 * 
	 * @param str
	 * @param curCharset
	 * @param desCharset
	 * @return
	 */
	public static String convertCharSet(String str, String curCharset, String desCharset) {

		String desStr = null;

		try {
			if (str != null) {

				if (curCharset != null) {
					desStr = new String(str.getBytes(curCharset), desCharset);
				} else {
					desStr = new String(str.getBytes(), desCharset);
				}
			}
		} catch (Throwable e) {
		}

		return desStr;
	}
	
	/**
	 * 将回车换行符替换成Html标记
	 * 
	 * @return
	 */
	public static String filterBr(String str, String replacement) {
		
		if (!isEmpty(str)) {
			return str.replaceAll("\n", replacement);
		}
		
		return str;
	}

	/**
	 * 过滤所有的Html标记
	 * 
	 * @return
	 */
	public static String getPureText(String str) {

		if (!isEmpty(str)) {
			return str.replaceAll(XmlTagExp.pattern(), "");
		}

		return str;
	}

	/**
	 * 返回定长的字符串
	 * 
	 * @return
	 */
	public static String getFixedLength(String str, Integer length) {

		if (!isEmpty(str)) {
			str = str.replaceAll(XmlTagExp.pattern(), "");

			if (length < str.length()) {
				str = str.substring(0, length) + " ...";
			}
		}

		return str;
	}

	public static String getFixWithZero(Integer val, Integer minLength) {

		if (val >= Math.pow(10, minLength)) {
			return String.valueOf(val);
		}

		String fixValue = String.valueOf(val);

		while (fixValue.length() < minLength) {
			fixValue = "0" + fixValue;
		}

		return fixValue;
	}

	/**
	 * 将给定的列表中的元素转换为字符串，并拼接成一个字符串返回
	 * 
	 * @param list
	 * @return
	 */
	public static String joinToString(List<?> list) {

		StringBuffer strBuf = new StringBuffer();

		if (list != null) {
			for (Object item : list) {
				strBuf.append(item.toString());
			}
		}

		return strBuf.toString();
	}
}
