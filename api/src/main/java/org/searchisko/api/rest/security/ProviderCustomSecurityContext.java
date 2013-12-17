/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;

/**
 * Custom security context used by {@link ProviderAuthenticationInterceptor} and
 * {@link ProviderSecurityPreProcessInterceptor}
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class ProviderCustomSecurityContext extends CustomSecurityContextBase {

	public static final String SUPER_ADMIN_ROLE = "super_admin";

	private boolean superAdmin;

	public ProviderCustomSecurityContext(Principal userPrincipal, boolean superAdmin, boolean secure,
			String authenticationScheme) {
		super(AuthenticatedUserType.PROVIDER, userPrincipal, secure, authenticationScheme);
		this.superAdmin = superAdmin;
	}

	@Override
	public boolean isUserInRole(String role) {
		if (SUPER_ADMIN_ROLE.equals(role)) {
			return superAdmin;
		}
		return super.isUserInRole(role);
	}

	@Override
	public String toString() {
		return "ProviderCustomSecurityContext [userType=" + userType + ", userPrincipal=" + userPrincipal + ", secure="
				+ secure + ", authenticationScheme=" + authenticationScheme + ", superAdmin=" + superAdmin + "]";
	}

}
