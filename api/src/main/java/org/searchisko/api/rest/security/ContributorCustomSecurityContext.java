/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

/**
 * Custom security context used by {@link ContributorAuthenticationInterceptor} and
 * {@link ContributorSecurityPreProcessInterceptor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorCustomSecurityContext implements SecurityContext {

	private Principal userPrincipal;

	private boolean secure;

	private String authenticationScheme;

	/**
	 * Used to cache contributor id not to call backend service more times during request.
	 */
	private String cachedContributorId;

	public ContributorCustomSecurityContext(Principal userPrincipal, boolean secure, String authenticationScheme) {
		this.userPrincipal = userPrincipal;
		this.secure = secure;
		this.authenticationScheme = authenticationScheme;
	}

	@Override
	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	@Override
	public boolean isUserInRole(String role) {
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

	public String getCachedContributorId() {
		return cachedContributorId;
	}

	public void setCachedContributorId(String cachedContributorId) {
		this.cachedContributorId = cachedContributorId;
	}

	@Override
	public String toString() {
		return "ContributorCustomSecurityContext [userPrincipal=" + userPrincipal + ", secure=" + secure
				+ ", authenticationScheme=" + authenticationScheme + ", cachedContributorId=" + cachedContributorId + "]";
	}

}
