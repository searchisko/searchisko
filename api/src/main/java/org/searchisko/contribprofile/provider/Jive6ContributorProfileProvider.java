/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.searchisko.contribprofile.model.ContributorProfile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

	public static final String JIVE_PROFILE_REST_API = "https://community.jboss.org/api/core/v3/people/username/";

	protected DefaultHttpClient httpClient;

	@PostConstruct
	public void init() {
		httpClient = new DefaultHttpClient();
	}

	@Override
	public ContributorProfile getProfile(String jbossorgUsername) {
		HttpGet httpGet = new HttpGet(JIVE_PROFILE_REST_API + jbossorgUsername);

		// TODO: Set username/password for accessing Jive
		String username = "";
		String password = "";

		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		httpGet.addHeader(BasicScheme.authenticate(credentials, "US-ASCII", false));

		try {
			HttpResponse response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() >= 300) {
				log.log(Level.WARNING, "Cannot get profile data form Jive, reason: {0}", response.getStatusLine().getReasonPhrase());
				return null;
			}
			byte[] data = EntityUtils.toByteArray(response.getEntity());
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "data from Jive: {0}", new String(data));
			}
			return mapRawJsonData(data);
		} catch (IOException e) {
			return null;
		}
	}

	@PreDestroy
	public void destroy() {
		httpClient.getConnectionManager().shutdown();
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
		if (emails == null) {
			log.log(Level.FINE, "Emails not returned in response from Jive. Probably bad authentication");
			return email;
		}
		for (HashMap<String, Object> emailObject : emails) {
			email = (String) emailObject.get("value");
			if ((boolean) emailObject.get("primary")) {
				break;
			}
		}

		return email;

	}
}
