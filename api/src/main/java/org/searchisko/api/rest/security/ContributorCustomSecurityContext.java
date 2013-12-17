/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;

/**
 * Custom security context used by {@link ContributorAuthenticationInterceptor} and
 * {@link ContributorSecurityPreProcessInterceptor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorCustomSecurityContext extends CustomSecurityContextBase {

	public ContributorCustomSecurityContext(Principal userPrincipal, boolean secure, String authenticationScheme) {
		super(AuthenticatedUserType.CONTRIBUTOR, userPrincipal, secure, authenticationScheme);
	}

	@Override
	public String toString() {
		return "ContributorCustomSecurityContext [userType=" + userType + ", userPrincipal=" + userPrincipal + ", secure="
				+ secure + ", authenticationScheme=" + authenticationScheme + "]";
	}

}
