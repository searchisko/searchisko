/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit.handler;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.service.StatsClientService;
import org.searchisko.api.service.StatsRecordType;

/**
 * Simple implementation of {@link org.searchisko.api.audit.handler.AuditHandler} which logs audit information to {@link java.util.logging.Logger}
 *
 * @author Libor Krzyzanek
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class AuditHandlerStatsClient implements AuditHandler {

	@Inject
	protected Logger log;

	@Inject
	protected StatsClientService statsClientService;

	@Override
	public void handle(Method method, String operation, String path, Principal principal, AuthenticatedUserType userType, Object content, Object id) {
		log.log(Level.FINE, "Write audit to stats client");

		long now = System.currentTimeMillis();
		Map<String, Object> data = new HashMap<>();
		data.put("operation", operation);
		data.put("path", path);
		data.put("username", principal != null ? principal.getName() : null);
		data.put("user_type", userType);
		data.put("id", id);
		data.put("content", content);

		statsClientService.writeStatisticsRecord(StatsRecordType.AUDIT, now, data);
	}

}
