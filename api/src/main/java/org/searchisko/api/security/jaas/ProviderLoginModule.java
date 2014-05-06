package org.searchisko.api.security.jaas;

import org.jasig.cas.client.authentication.SimplePrincipal;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.util.CdiHelper;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.security.acl.Group;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provider login module. Extension of Picketbox's UsernamePasswordLoginModule.
 * Authentication is done via providerService
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.service.ProviderService#authenticate(String, String)
 * @see org.searchisko.api.service.ProviderService#isSuperProvider(String)
 * @see org.searchisko.api.security.Role
 */
public class ProviderLoginModule extends UsernamePasswordLoginModule {

	@Inject
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		log.log(Level.INFO, "Initializing JAAS ProviderLoginModule");
		try {
			CdiHelper.programmaticInjection(ProviderLoginModule.class, this);
		} catch (NamingException e) {
			throw new RuntimeException("Cannot initialize Login module", e);
		}
		super.initialize(subject, callbackHandler, sharedState, options);
	}

	@Override
	public boolean login() throws LoginException {
		log.log(Level.INFO, "ProviderLoginModule.Login called");
		return super.login();
	}

	@Override
	protected boolean validatePassword(String inputPassword, String expectedPassword) {
		log.log(Level.FINE, "Validate password for username: {0}", getUsername());
		return providerService.authenticate(getUsername(), inputPassword);
	}


	/**
	 * Always return null because overwritten validatePassword doesn't use it
	 *
	 * @return
	 * @throws LoginException
	 * @see #validatePassword(String, String)
	 */
	@Override
	protected String getUsersPassword() throws LoginException {
		return null;
	}

	@Override
	protected Group[] getRoleSets() throws LoginException {
		log.log(Level.FINE, "Get RoleSets for username: {0}", getUsername());

		Group[] groups = new Group[1];
		// RoleGroup
		groups[0] = new SimpleGroup("Roles");
		groups[0].addMember(new SimplePrincipal(Role.PROVIDER));

		if (providerService.isSuperProvider(getUsername())) {
			groups[0].addMember(new SimplePrincipal(Role.ADMIN));
		}

		return groups;
	}
}
