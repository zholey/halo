/*
 * 版权所有 ©2011-2013 格点软件(北京)有限公司 All rights reserved.
 * 
 * 未经书面授权，不得擅自复制、影印、储存或散播。
 */
package org.gridsofts.halo.exception;

public class AnnotationException extends DAOException {
	private static final long serialVersionUID = 1L;

	public AnnotationException() {
		super("Class annotation exception.");
	}
	
	public AnnotationException(String msg) {
		super("Class annotation exception:" + msg);
	}
}
