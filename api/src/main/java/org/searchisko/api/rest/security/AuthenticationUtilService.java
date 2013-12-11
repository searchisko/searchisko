/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.searchisko.api.annotations.security.ContributorAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.service.ContributorProfileService;

/**
 * Authentication utility service. Use it in your RestServices if you need info about currently logged in user!
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
public class AuthenticationUtilService {

	@Inject
	protected Logger log;

	@Inject
	protected ContributorProfileService contributorProfileService;

	@Context
	protected SecurityContext securityContext;

	/**
	 * Get name of authenticated/logged in 'provider' based on security user principal. Can be used only in methods where
	 * {@link ProviderAllowed} annotation is applied.
	 * 
	 * @return name of authenticated provider
	 * @throws NotAuthenticatedException if Provider is not authenticated. Use {@link ProviderAllowed} annotation to your
	 *           REST service to prevent this exception.
	 * 
	 * @see ProviderSecurityPreProcessInterceptor
	 */
	public String getAuthenticatedProvider() throws NotAuthenticatedException {
		if (securityContext == null || securityContext.getUserPrincipal() == null
				|| !(securityContext instanceof ProviderCustomSecurityContext)) {
			throw new NotAuthenticatedException(AuthenticatedUserTypes.PROVIDER);
		}
		return securityContext.getUserPrincipal().getName();
	}

	/**
	 * Get 'contributor id' for currently authenticated/logged in user based on security user principal. Can be used only
	 * in methods where {@link ContributorAllowed} annotation is applied.
	 * 
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *          not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *          return null instead.
	 * @return contributor id - can be null if
	 *         <code><forceCreate/code> is false and contributor record do not exists yet for current user.
	 * @throws NotAuthenticatedException in case contributor is not authenticated/logged in. This should never happen if
	 *           security interceptor is correctly implemented and configured for this class and
	 *           {@link ContributorAllowed} is used without <code>optional</code>.
	 */
	public String getAuthenticatedContributor(boolean forceCreate) throws NotAuthenticatedException {
		if (securityContext == null || securityContext.getUserPrincipal() == null
				|| !(securityContext instanceof ContributorCustomSecurityContext)) {
			throw new NotAuthenticatedException(AuthenticatedUserTypes.CONTRIBUTOR);
		}
		// TODO _RATING propagate forceCreate parameter into contributorProfileService.getContributorId()
		return contributorProfileService.getContributorId(securityContext.getUserPrincipal().getName());
	}

}
