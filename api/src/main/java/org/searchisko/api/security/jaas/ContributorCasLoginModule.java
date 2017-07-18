package org.searchisko.api.security.jaas;

import java.io.IOException;
import java.security.acl.Group;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.jasig.cas.client.authentication.SimpleGroup;
import org.jasig.cas.client.authentication.SimplePrincipal;
import org.jasig.cas.client.jaas.AssertionPrincipal;
import org.jasig.cas.client.jaas.CasLoginModule;
import org.jasig.cas.client.util.CommonUtils;
import org.searchisko.api.service.AppConfigurationService;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.service.ProviderService;

/**
 * CAS JAAS Login Module
 * <br/>
 * Configuration:<br/>
 * <li>
 * <ul>contributorTypeSpecificCodeIdentifier - Name of type specific field in Contributor document used for exact match for finding corresponding contributor </ul>
 * </li>
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.security.Role
 */
public class ContributorCasLoginModule extends CasLoginModule {

	protected Logger log = Logger.getLogger(getClass().getName());;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected AppConfigurationService appConfigurationService;

	@Inject
	protected ContributorService contributorService;

	/**
	 * Name of type specific field in Contributor document used for exact match for finding corresponding contributor
	 */
	protected String contributorTypeSpecificCodeIdentifier = ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME;

	@Override
	public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> state, Map<String, ?> options) {
		try {
			
			InitialContext initialContext = new InitialContext();
			Object lookup = initialContext.lookup("java:module/ProviderService");
			this.providerService = (ProviderService) lookup;
			lookup = initialContext.lookup("java:module/AppConfigurationService");
			this.appConfigurationService = (AppConfigurationService) lookup;
			lookup = initialContext.lookup("java:module/ContributorService");
			this.contributorService = (ContributorService) lookup;
			
		} catch (NamingException e) {
			throw new RuntimeException("Cannot initialize Login module", e);
		}
		log.log(Level.FINE, "Initializing JAAS ContributorCasLoginModule");

		if (options.containsKey("contributorTypeSpecificCodeIdentifier")) {
			contributorTypeSpecificCodeIdentifier = options.get("contributorTypeSpecificCodeIdentifier").toString();
		}
		log.log(Level.FINE, "contributorTypeSpecificCodeIdentifier: " + contributorTypeSpecificCodeIdentifier);

		HashMap<String, Object> ops = new HashMap<>(options);

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


		ContributorPrincipal contributorPrincipal = fixPrincipal();

		Set<SimpleGroup> groups = subject.getPrincipals(org.jasig.cas.client.authentication.SimpleGroup.class);
		log.log(Level.FINE, "Add Roles to authenticated contributor, default roles: {0}", groups);

		Set<String> roles = contributorPrincipal.getRoles();
		for (final String defaultRole : defaultRoles) {
			roles.add(defaultRole);
		}

		for (SimpleGroup g : groups) {
			if (this.roleGroupName.equals(g.getName())) {
				Set<String> contributorRoles = getContributorRoles(this.assertion.getPrincipal().getName());
				if (contributorRoles != null) {
					for (String role : contributorRoles) {
						g.addMember(new SimplePrincipal(role));
						contributorPrincipal.getRoles().add(role);
					}
				}
				log.log(Level.FINE, "Actual roles in role group: {0}", g);
				log.log(Level.FINE, "Actual roles in principal: {0}", roles);
				break;
			}
		}

		return success;
	}

	protected Set<String> getContributorRoles(String username) {
		return contributorService.getRolesByTypeSpecificCode(contributorTypeSpecificCodeIdentifier, username);
	}

	protected ContributorPrincipal fixPrincipal() {
		log.log(Level.FINEST, "Remove CAS principal and default group. Assertion name: {0}", this.assertion.getPrincipal().getName());
		this.subject.getPrincipals().remove(new AssertionPrincipal(this.assertion.getPrincipal().getName(), this.assertion));
		this.subject.getPrincipals().remove(new SimpleGroup(this.principalGroupName));


		log.log(Level.FINEST, "Add ContributorPrincipal");
		final ContributorPrincipal contributorPrincipal = new ContributorPrincipal(this.assertion.getPrincipal().getName(), this.assertion);
		this.subject.getPrincipals().add(contributorPrincipal);

		final Group principalGroup = new SimpleGroup(this.principalGroupName);
		principalGroup.addMember(contributorPrincipal);
		this.subject.getPrincipals().add(principalGroup);

		return contributorPrincipal;
	}
}
