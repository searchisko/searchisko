package org.searchisko.api.security.jaas;

import org.searchisko.api.service.ProviderService;

import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * @author Libor Krzyzanek
 * @see org.searchisko.api.security.Role
 */
public class ContributorCasLoginModule {

	public static final String AUTH_METHOD_CAS = "CAS_SSO";

	@Inject
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	//TODO: Implement CAS Login Module

}
