/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.searchisko.contribprofile.model.ContributorProfile;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jive 6 implementation of Contributor Provider. <br/>
 * Documentation for Jive 6 REST API: https://developers.jivesoftware.com/api/v3/rest/PersonService.html
 *
 * @author Libor Krzyzanek
 */
@Named
@Stateless
@LocalBean
public class Jive6ContributorProfileProvider implements ContributorProfileProvider {

	@Inject
	protected Logger log;

	@Override
	public ContributorProfile getProfile(String jbossorgUsername) {
		// TODO: Implement logic of getting profile from https://community.jboss.org/api/core/v3/people/username/%7Busername%7D

		ContributorProfile profile = new ContributorProfile("John Doe", "john.doe@doe.com");
		return profile;
	}

	protected ContributorProfile mapRawJsonData(byte[] data) {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> map;
		try {
			// it's needed to remove weird first line:
			// throw 'allowIllegalResourceCall is false.';
			map = mapper.readValue(data, 44, data.length, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot parse Jive 6 profile json data" + e);
			return null;
		}
		HashMap<String, Object> jiveObject = (HashMap<String, Object>) map.get("jive");
		HashMap<String, Object> nameObject = (HashMap<String, Object>) map.get("name");
		List<HashMap<String, Object>> emails = (List<HashMap<String, Object>>) map.get("emails");


		String contributorId = getContributorId((String) nameObject.get("formatted"), getPrimaryEmail(emails));
		ContributorProfile profile = new ContributorProfile(contributorId, (String) jiveObject.get("username"));

		profile.setJiveProfile(jiveObject);

		return profile;
	}

	protected String getContributorId(String name, String email) {
		return name + " <" + email + ">";
	}

	protected String getPrimaryEmail(List<HashMap<String, Object>> emails) {
		String email = "";
		for (HashMap<String, Object> emailObject : emails) {
			email = (String) emailObject.get("value");
			if ((boolean) emailObject.get("primary")) {
				break;
			}
		}

		return email;

	}
}
