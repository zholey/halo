/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.exception;

/**
 * DAOException是对JdbcSupportDAO可能会发生的所有异常信息的概述，
 * 包括Exception类信息以及RuntimeException信息。
 * 
 * @author Lei
 * 
 */
public class DAOException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DAOException(String msg) {
		super(msg);
	}
}
