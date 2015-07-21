/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit.handler;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.security.AuthenticatedUserType;

/**
 * Simple implementation of {@link AuditHandler} which logs audit information to {@link java.util.logging.Logger}
 *
 * @author Libor Krzyzanek
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class AuditHandlerLogging implements AuditHandler {

	public static final String TARGET_LOGGER_CLASS_SUFFIX = ".Audit";

	/**
	 * Default audit log level
	 */
	public static final Level DEFAULT_AUDIT_LOG_LEVEL = Level.INFO;

	@Inject
	protected Logger log;

	@Override
	public void handle(Method method, String operation, String path, Principal principal, AuthenticatedUserType userType, Object content, Object id) {
		Level logLevel = DEFAULT_AUDIT_LOG_LEVEL;

		if (log.isLoggable(logLevel)) {
			Object[] params = new Object[]{
					operation != null ? operation : "",
					path != null ? path : "",
					principal != null ? principal.getName() : "",
					userType != null ? userType : "",
					id != null ? id : "",
					content != null ? content : ""};
			log.log(logLevel, "operation: ''{0}'', path: ''{1}'', username: ''{2}'', userType: ''{3}'', id: ''{4}'', content: ''{5}''", params);
		}

		// Logs as audited class on FINE level
		Logger targetClassLogger = getTargetLogger(method);
		if (targetClassLogger.isLoggable(Level.FINE)) {
			targetClassLogger.log(logLevel,
					"Executing method: ''{0}'', by: {1}",
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
