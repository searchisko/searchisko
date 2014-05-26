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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditIgnore;

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

	@Inject
	protected Logger log;

	@AroundInvoke
	public Object aroundInvoke(InvocationContext ic) throws Exception {
		AuditIgnore ignored = ic.getMethod().getAnnotation(AuditIgnore.class);
		Audit auditOnMethod = ic.getMethod().getAnnotation(Audit.class);

		boolean audit = true;
		if (ignored != null && auditOnMethod == null) {
			audit = false;
			log.log(Level.FINEST, "Skip audit because of @AuditIgnore");
		}

		if (audit) {
			auditService.auditMethod(ic.getMethod(), ic.getParameters());
		}

		return ic.proceed();
	}

}
