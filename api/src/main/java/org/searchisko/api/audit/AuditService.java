/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.audit.handler.AuditHandler;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.service.AuthenticationUtilService;

/**
 * Business logic for Audit Facility
 * 
 * @author Libor Krzyzanek
 */
@Named
@Stateless
public class AuditService {

	@Inject
	private HttpServletRequest httpRequest;

	@Inject
	@Any
	Instance<AuditHandler> auditHandlers;

	@Inject
	private AuthenticationUtilService authenticationUtilService;

	/**
	 * Do the audit logic
	 * 
	 * @param method
	 * @param parameters method's parameters
	 */
	public void auditMethod(Method method, Object[] parameters) {
		Object id = getAnnotatedParamValue(method, parameters, AuditId.class);
		Object content = getAnnotatedParamValue(method, parameters, AuditContent.class);

		String path = null;
		String operation = null;
		if (httpRequest != null) {
			path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
			operation = httpRequest.getMethod();
		}
		AuthenticatedUserType userType = authenticationUtilService.getAuthenticatedUserType();
		for (AuditHandler handler : auditHandlers) {
			handler.handle(method, operation, path, authenticationUtilService.getAuthenticatedUserPrincipal(), userType,
					content, id);
		}
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

}
