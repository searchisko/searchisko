/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

/**
 * Custom security context base for our contexts. Handles common fields and user types.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class CustomSecurityContextBase implements SecurityContext {

	protected AuthenticatedUserType userType;

	protected Principal userPrincipal;

	protected boolean secure;

	protected String authenticationScheme;

	public CustomSecurityContextBase(AuthenticatedUserType userType, Principal userPrincipal, boolean secure,
			String authenticationScheme) {
		this.userType = userType;
		this.userPrincipal = userPrincipal;
		this.secure = secure;
		this.authenticationScheme = authenticationScheme;
	}

	/**
	 * Check if this context authenticates user of given type.
	 * 
	 * @param userType to check
	 * @return true if type matches
	 */
	public boolean isUserType(AuthenticatedUserType userType) {
		return this.userType.equals(userType);
	}

	@Override
	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	@Override
	public boolean isUserInRole(String role) {
		return userType.roleName().equals(role);
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
