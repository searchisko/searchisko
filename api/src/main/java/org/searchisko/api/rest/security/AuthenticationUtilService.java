/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import org.searchisko.api.annotations.security.ContributorAllowed;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.security.Role;
import org.searchisko.api.security.jaas.ContributorCasLoginModule;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.util.SearchUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication utility service. Use it in your RestServices if you need info about currently logged in user! Also
 * contains methods for fine grained permission checks.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@RequestScoped
public class AuthenticationUtilService {

	//TODO: Move to services package

	@Inject
	protected Logger log;

	@Inject
	protected ContributorProfileService contributorProfileService;

	/**
	 * request scoped cache.
	 */
	private String cachedContributorId;

	/**
	 * Get name of authenticated/logged in 'provider' based on security user principal. Can be used only in methods where
	 * roles PROVIDER or ADMIN is applied.
	 *
	 * @return name of authenticated provider
	 * @throws NotAuthenticatedException if Provider is not authenticated. Use roles PROVIDER, ADMIN annotation to your
	 *                                   REST service to prevent this exception.
	 */
	public String getAuthenticatedProvider(SecurityContext securityContext) throws NotAuthenticatedException {
		if (!isAuthenticatedUserOfType(securityContext, AuthenticatedUserType.PROVIDER)) {
			throw new NotAuthenticatedException(AuthenticatedUserType.PROVIDER);
		}
		return securityContext.getUserPrincipal().getName();
	}

	/**
	 * Check if logged in user has management permission for passed in provider.
	 *
	 * @param securityContext to look for currently logged in user in
	 * @param providerName    to check permission for
	 * @throws NotAuthorizedException if user has not the permission
	 */
	public void checkProviderManagementPermission(SecurityContext securityContext, String providerName)
			throws NotAuthorizedException {
		if (log.isLoggable(Level.FINE))
			log.fine("Going to check ProviderManage permission for provider " + providerName + " and securityContext "
					+ securityContext);
		if (securityContext != null
				&& providerName != null
				&& (securityContext.isUserInRole(Role.ADMIN) || providerName
				.equalsIgnoreCase(getAuthenticatedProvider(securityContext)))) {
			return;
		}
		throw new NotAuthorizedException("management permission for content provider " + providerName);
	}

	/**
	 * Get 'contributor id' for currently authenticated/logged in user based on security user principal. Can be used only
	 * in methods where {@link ContributorAllowed} annotation is applied.
	 *
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *                    not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *                    return null instead.
	 * @return contributor id - can be null if <code><forceCreate</code> is false and contributor record do not exists yet
	 * for current user.
	 * @throws NotAuthenticatedException in case contributor is not authenticated/logged in. This should never happen if
	 *                                   security interceptor is correctly implemented and configured for this class and
	 *                                   {@link ContributorAllowed} is used without <code>optional</code>.
	 */
	public String getAuthenticatedContributor(SecurityContext securityContext, boolean forceCreate)
			throws NotAuthenticatedException {
		log.log(Level.FINEST, "Get Authenticated Contributor, forceCreate: {0}", forceCreate);

		if (!isAuthenticatedUserOfType(securityContext, AuthenticatedUserType.CONTRIBUTOR)) {
			log.fine("User is not authenticated");
			cachedContributorId = null;
			throw new NotAuthenticatedException(AuthenticatedUserType.CONTRIBUTOR);
		}

		// cache contributor id in request not to call backend service too often
		if (cachedContributorId != null)
			return cachedContributorId;

		String cid = SearchUtils.trimToNull(contributorProfileService.getContributorId(
				mapAuthenticationSchemeToContrinutorCodeType(securityContext.getAuthenticationScheme()), securityContext
						.getUserPrincipal().getName(), forceCreate
		));
		cachedContributorId = cid;

		log.log(Level.FINE, "Contributor ID for authenticated user: {0}", cid);

		return cid;
	}

	/**
	 * Force update of currently logged in contributor profile. No any exception is thrown. Should be called after
	 * contributor authentication.
	 */
	public void updateAuthenticatedContributorProfile(SecurityContext securityContext) {
		if (isAuthenticatedUserOfType(securityContext, AuthenticatedUserType.CONTRIBUTOR)) {
			try {
				String uname = SearchUtils.trimToNull(securityContext.getUserPrincipal().getName());
				if (uname != null) {
					// TODO CONTRIBUTOR_PROFILE we should consider to run update in another thread not to block caller
					contributorProfileService.createOrUpdateProfile(
							mapAuthenticationSchemeToContrinutorCodeType(securityContext.getAuthenticationScheme()), uname, false);
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Contributor profile update failed: " + e.getMessage(), e);
			}
		}
	}

	protected String mapAuthenticationSchemeToContrinutorCodeType(String authenticationScheme) {
		if (ContributorCasLoginModule.AUTH_METHOD_CAS.equals(authenticationScheme)) {
			return ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME;
		} else {
			throw new UnsupportedOperationException("Unsupported authentication scheme " + authenticationScheme);
		}
	}

	/**
	 * Check if user of given type is authenticated.
	 *
	 * @param userType to check
	 * @return true if user of given type is authenticated.
	 */
	public boolean isAuthenticatedUserOfType(SecurityContext securityContext, AuthenticatedUserType userType) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Security Context: {0}, role to check: {1}",
					new Object[]{securityContext, userType});
		}
		if (securityContext == null) {
			return false;
		}
		switch (userType) {
			case PROVIDER:
				return securityContext.isUserInRole(Role.ADMIN) || securityContext.isUserInRole(Role.PROVIDER);
			case CONTRIBUTOR:
				return securityContext.isUserInRole(Role.CONTRIBUTOR);
		}
		return false;
	}

}
