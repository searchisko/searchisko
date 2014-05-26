/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit.handler;

import java.lang.reflect.Method;
import java.security.Principal;

import org.searchisko.api.security.AuthenticatedUserType;

/**
 * Base interface for Audit Handler. Any CDI implementation of this interface is processed in {@link org.searchisko.api.audit.AuditService}
 *
 * @author Libor Krzyzanek
 */
public interface AuditHandler {

	/**
	 * Handle audit message
	 *
	 * @param method    Audited Java method
	 * @param operation HTTP Operation e.g. GET, POST
	 * @param path      Audited URL
	 * @param principal Logged in user, can be null
	 * @param userType  User type, can be null
	 * @param content   Content, can be null
	 * @param id        Content ID, can be null
	 */
	public void handle(Method method, String operation, String path, Principal principal, AuthenticatedUserType userType, Object content, Object id);

}
