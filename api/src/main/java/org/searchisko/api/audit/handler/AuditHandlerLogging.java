/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit.handler;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple implementation of {@link AuditHandler} which logs audit information to {@link java.util.logging.Logger}
 *
 * @author Libor Krzyzanek
 */
@Named
@ApplicationScoped
@Singleton
public class AuditHandlerLogging implements AuditHandler {

	public static final String TARGET_LOGGER_CLASS_SUFFIX = ".Audit";

	/**
	 * Default audit log level
	 */
	public static final Level DEFAULT_AUDIT_LOG_LEVEL = Level.INFO;

	@Inject
	protected Logger log;

	@Override
	public void handle(Method method, String path, Principal principal, Object content, Object id) {
		Level logLevel = DEFAULT_AUDIT_LOG_LEVEL;

		if (log.isLoggable(logLevel)) {
			log.log(logLevel,
					"path: ''{0}'', user: ''{1}'', id: ''{2}'', content: ''{3}''",
					new Object[]{path, principal, id, content});
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
