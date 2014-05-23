/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit.handler;

import java.lang.reflect.Method;
import java.security.Principal;

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
	 * @param path      Audited URL
	 * @param principal
	 * @param content   Content
	 * @param id        Content ID
	 */
	public void handle(Method method, String path, Principal principal, Object content, Object id);

}
