/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.searchisko.api.annotations.security.ContributorAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.util.SearchUtils;

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
	 * request scoped cache.
	 */
	private String cachedContributorId;

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
		if (!isAuthenticatedUserOfType(AuthenticatedUserType.PROVIDER)) {
			throw new NotAuthenticatedException(AuthenticatedUserType.PROVIDER);
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
	 * @return contributor id - can be null if <code><forceCreate</code> is false and contributor record do not exists yet
	 *         for current user.
	 * @throws NotAuthenticatedException in case contributor is not authenticated/logged in. This should never happen if
	 *           security interceptor is correctly implemented and configured for this class and
	 *           {@link ContributorAllowed} is used without <code>optional</code>.
	 */
	public String getAuthenticatedContributor(boolean forceCreate) throws NotAuthenticatedException {
		if (!isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR)) {
			cachedContributorId = null;
			throw new NotAuthenticatedException(AuthenticatedUserType.CONTRIBUTOR);
		}

		// cache contributor id in request not to call backend service too often
		if (cachedContributorId != null)
			return cachedContributorId;

		String cid = SearchUtils.trimToNull(contributorProfileService.getContributorId(
				securityContext.getAuthenticationScheme(), securityContext.getUserPrincipal().getName(), forceCreate));
		cachedContributorId = cid;

		return cid;
	}

	/**
	 * Force update of currently logged in contributor profile. No any exception is thrown. Should be called after
	 * contributor authentication.
	 */
	public void updateAuthenticatedContributorProfile() {
		if (isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR)) {
			try {
				String uname = SearchUtils.trimToNull(securityContext.getUserPrincipal().getName());
				if (uname != null) {
					// TODO CONTRIBUTOR_PROFILE we should consider to run update in another thread not to block caller
					contributorProfileService.createOrUpdateProfile(securityContext.getAuthenticationScheme(), uname);
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Contributor profile update failed", e);
			}
		}
	}

	/**
	 * Check if user of given type is authenticated.
	 * 
	 * @param userType to check
	 * @return true if user of given type is authenticated.
	 */
	public boolean isAuthenticatedUserOfType(AuthenticatedUserType userType) {
		return securityContext != null && securityContext.getUserPrincipal() != null
				&& securityContext.isUserInRole(userType.roleName());
	};

}
