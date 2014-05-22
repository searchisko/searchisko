/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.searchisko.api.audit.annotation.Audit;

/**
 * Interceptor handling {@link org.searchisko.api.audit.annotation.Audit} annotation
 *
 * @author Libor Krzyzanek
 */
@Interceptor
@Audit
public class AuditInterceptor {

	@Inject
	private AuditService auditService;

	@AroundInvoke
	public Object aroundInvoke(InvocationContext ic) throws Exception {
		auditService.auditMethod(ic.getMethod(), ic.getParameters());
		return ic.proceed();
	}

}
