package org.searchisko.api.service;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling contributor profiles
 *
 * @author Libor Krzyzanek
 */
@Named
@Stateless
@LocalBean
public class ContributorProfileService {

	@Inject
	protected Logger log;

	public String getContributorId(String username) {
		return "";
	}

	public void createOrUpdateProfile(String username) {
		log.log(Level.FINE, "Create or update profile for username {0}", username);

		// TODO: call this method when user successfully log in.
		// TODO: Implement creating/updating profile based on data from Jive
	}

}
