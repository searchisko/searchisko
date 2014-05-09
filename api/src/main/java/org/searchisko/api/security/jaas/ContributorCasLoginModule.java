package org.searchisko.api.security.jaas;

import org.jasig.cas.client.authentication.SimpleGroup;
import org.jasig.cas.client.jaas.CasLoginModule;
import org.jasig.cas.client.util.CommonUtils;
import org.searchisko.api.service.AppConfigurationService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.util.CdiHelper;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CAS JAAS Login Module
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.security.Role
 */
public class ContributorCasLoginModule extends CasLoginModule {

	@Inject
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected AppConfigurationService appConfigurationService;

	@Override
	public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> state, Map<String, ?> options) {
		try {
			CdiHelper.programmaticInjection(this);
		} catch (NamingException e) {
			throw new RuntimeException("Cannot initialize Login module", e);
		}
		log.log(Level.FINE, "Initializing JAAS ContributorCasLoginModule");

		HashMap<String, Object> ops = new HashMap(options);

		String casServerUrl = appConfigurationService.getAppConfiguration().getCasConfig().getServerUrl();
		ops.put("casServerUrlPrefix", casServerUrl);
		super.initialize(subject, handler, state, ops);
	}

	@Override
	public boolean login() throws LoginException {
		log.log(Level.FINEST, "Check if service is http(s)");

		final NameCallback serviceCallback = new NameCallback("service");
		final PasswordCallback ticketCallback = new PasswordCallback("ticket", false);
		try {
			this.callbackHandler.handle(new Callback[]{ticketCallback, serviceCallback});
		} catch (final IOException e) {
			log.log(Level.SEVERE, "Login failed due to IO exception in callback handler: {0}", e);
			throw (LoginException) new LoginException("IO exception in callback handler: " + e).initCause(e);
		} catch (final UnsupportedCallbackException e) {
			log.log(Level.SEVERE, "Login failed due to unsupported callback: {0}", e);
			throw (LoginException) new LoginException("Callback handler does not support PasswordCallback and TextInputCallback.").initCause(e);
		}

		if (CommonUtils.isBlank(serviceCallback.getName())) {
			log.log(Level.FINE, "No Service passed. No check performed");
			return super.login();
		}

		if (serviceCallback.getName().startsWith("http://") || serviceCallback.getName().startsWith("https://")) {
			return super.login();
		} else {
			log.log(Level.FINE, "Service (username) {0} does not start with http(s)://. Skip this login module", serviceCallback.getName());
			return false;
		}
	}

	@Override
	public boolean commit() throws LoginException {
		boolean success = super.commit();

		if (!success) {
			return false;
		}


		//TODO: Set roles to CAS authenticated contributor
		Set<SimpleGroup> groups = subject.getPrincipals(org.jasig.cas.client.authentication.SimpleGroup.class);
		log.log(Level.FINE, "Add Roles to authenticated contributor, default roles: {0}", groups);

//		for (SimpleGroup g : groups) {
//			g.addMember(new SimplePrincipal("test_role"));
//		}

		return success;
	}
}
