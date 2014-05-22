/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;

/**
 * Business logic for Audit Facility
 *
 * @author Libor Krzyzanek
 */
@Named
@Stateless
public class AuditService {

	public static final String TARGET_LOGGER_CLASS_SUFFIX = ".Audit";

	public static final Level DEFAULT_AUDIT_LOG_LEVEL = Level.INFO;

	public static final Level DEFAULT_AUDIT_LOG_LEVEL_DATA = Level.INFO;

	@Inject
	protected Logger log;

	@Inject
	protected Principal principal;

	/**
	 * Do the audit logic
	 *
	 * @param method
	 */
	public void auditMethod(Method method, Object[] parameters) {
		Object id = getAnnotatedParamValue(method, parameters, AuditId.class);
		Object content = getAnnotatedParamValue(method, parameters, AuditContent.class);
		auditToLog(method, id, content);
	}

	/**
	 * Helper method to get value of called method for desired annotation
	 *
	 * @param method
	 * @param parameters
	 * @param expectedAnnotation
	 * @return
	 */
	public static Object getAnnotatedParamValue(Method method, Object[] parameters, Class<?> expectedAnnotation) {
		Annotation[][] annotations = method.getParameterAnnotations();
		int parameterPosition = 0;
		for (Annotation[] paramAnnotations : annotations) {
			for (Annotation ann : paramAnnotations) {
				if (ann.annotationType().equals(expectedAnnotation)) {
					return parameters[parameterPosition];
				}
			}
			parameterPosition++;
		}
		return null;
	}

	/**
	 * Write audit information to the logging system
	 *
	 * @param method
	 * @param id
	 * @param content
	 */
	private void auditToLog(Method method, Object id, Object content) {

		// TODO: AUDIT: Move Audit Logger to separate bean and allows other implementations

		Level logLevel = DEFAULT_AUDIT_LOG_LEVEL;
		Level logForData = DEFAULT_AUDIT_LOG_LEVEL_DATA;

		if (log.isLoggable(logLevel)) {
			log.log(logLevel,
					"Audit ''{0}'',  Id: {2}, Caller: {1}",
					new Object[]{method.toString(), id, principal});
		}
		if (log.isLoggable(logForData)) {
			log.log(logLevel,
					"Audit data: ''{0}''",
					content);
		}

		// Logs as audited class
		Logger targetClassLogger = getTargetLogger(method);
		if (targetClassLogger.isLoggable(logLevel)) {
			targetClassLogger.log(logLevel,
					"Executing method ''{0}'' by {1}",
					new Object[]{method.getName(), principal});
		}
	}

	/**
	 * Get logger for audited method
	 *
	 * @param method
	 * @return
	 */
	protected Logger getTargetLogger(Method method) {
		return Logger.getLogger(method.getClass().getName() + TARGET_LOGGER_CLASS_SUFFIX);
	}


}
