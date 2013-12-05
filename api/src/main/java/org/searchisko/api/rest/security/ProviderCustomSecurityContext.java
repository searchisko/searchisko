/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

/**
 * Custom security context used by {@link ProviderAuthenticationInterceptor} and {@link ProviderSecurityPreProcessInterceptor}
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ProviderCustomSecurityContext implements SecurityContext {

	public static final String SUPER_ADMIN_ROLE = "super_admin";

	private Principal userPrincipal;

	private boolean superAdmin;

	private boolean secure;

	private String authenticationScheme;

	public ProviderCustomSecurityContext(Principal userPrincipal, boolean superAdmin, boolean secure, String authenticationScheme) {
		this.userPrincipal = userPrincipal;
		this.superAdmin = superAdmin;
		this.secure = secure;
		this.authenticationScheme = authenticationScheme;
	}

	@Override
	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	@Override
	public boolean isUserInRole(String role) {
		if (SUPER_ADMIN_ROLE.equals(role) && superAdmin) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public String getAuthenticationScheme() {
		return authenticationScheme;
	}

}
