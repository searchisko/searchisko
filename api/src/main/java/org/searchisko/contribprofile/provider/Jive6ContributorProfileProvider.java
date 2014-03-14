/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.model.AppConfiguration;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;

/**
 * Jive 6 implementation of Contributor Provider. <br/>
 * Documentation for Jive 6 REST API: https://developers.jivesoftware.com/api/v3/rest/PersonService.html
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@Stateless
@LocalBean
public class Jive6ContributorProfileProvider implements ContributorProfileProvider {

	@Inject
	protected Logger log;

	public static final String JIVE_PROFILE_REST_API = "/api/core/v3/people/username/";

	@Inject
	protected AppConfiguration appConfiguration;

	protected DefaultHttpClient httpClient;

	@PostConstruct
	public void init() {
		httpClient = new DefaultHttpClient();
	}

	@Override
	public ContributorProfile getProfile(String jbossorgUsername) {
		String username = appConfiguration.getContributorProfileProviderConfig().getUsername();
		String password = appConfiguration.getContributorProfileProviderConfig().getPassword();
		String jive6Url = appConfiguration.getContributorProfileProviderConfig().getUrlbase();

		HttpGet httpGet = new HttpGet(jive6Url + JIVE_PROFILE_REST_API + jbossorgUsername);

		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		httpGet.addHeader(BasicScheme.authenticate(credentials, "US-ASCII", false));

		try {
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() >= 300) {
				EntityUtils.consume(entity);
				log.log(Level.WARNING, "Cannot get profile data form Jive, reason: {0}", response);
				return null;
			}
			byte[] data = EntityUtils.toByteArray(entity);
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

	private static final byte FIRST_RESPONSE_BYTE = "{".getBytes()[0];

	@SuppressWarnings("unchecked")
	protected ContributorProfile mapRawJsonData(byte[] data) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map;
		try {
			// next code is used to remove weird first line of JIVE response. Simply we find first { which means begin of JSON
			// data.
			int startOffset = 0;
			for (byte b : data) {
				if (FIRST_RESPONSE_BYTE == b) {
					break;
				}
				startOffset++;
			}
			// TODO CONTRIBUTOR_PROFILE is encoding (UTF-8 or ISO-xx etc.) of profile data from JIVE server handled correctly
			// here?
			map = mapper.readValue(data, startOffset, data.length, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			String msg = "Cannot parse Jive 6 profile json data: " + e.getMessage();
			log.log(Level.WARNING, msg);
			throw new RuntimeException(msg);
		}
		Map<String, Object> jiveObject = (Map<String, Object>) map.get("jive");
		Map<String, Object> nameObject = (Map<String, Object>) map.get("name");
		List<Map<String, Object>> emailsObject = (List<Map<String, Object>>) map.get("emails");

		Map<String, List<String>> typeSpecificCodes = new HashMap<>();
		addTypeSpecificCode(typeSpecificCodes, ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
				(String) jiveObject.get("username"));
		addTypeSpecificCode(typeSpecificCodes, ContributorProfileService.FIELD_TSC_GITHUB_USERNAME,
				getProfileValue(jiveObject, "github Username"));

		Map<String, Object> profileData = mapProfileData(map, jiveObject);

		ContributorProfile profile = new ContributorProfile((String) profileData.get(ContentObjectFields.SYS_ID),
				(String) nameObject.get("formatted"), getPrimaryEmail(emailsObject), getEmails(emailsObject), typeSpecificCodes);

		profile.setProfileData(profileData);

		return profile;
	}

	protected final String JIVE_PROFILE_NAME_KEY = "jive_label";
	protected final String JIVE_PROFILE_VALUE_KEY = "value";

	@SuppressWarnings("unchecked")
	protected Map<String, Object> mapProfileData(Map<String, Object> map, Map<String, Object> jiveObject) {
		Map<String, Object> profileData = new LinkedHashMap<>();

		Object username = jiveObject.get("username");

		profileData.put(ContentObjectFields.SYS_CONTENT_PROVIDER, "jbossorg");
		profileData.put(ContentObjectFields.SYS_TYPE, "contributor_profile");
		profileData.put(ContentObjectFields.SYS_CONTENT_TYPE, "jbossorg_contributor_profile");

		profileData.put("id", "jbossorg_contributor_profile-" + username);
		profileData.put(ContentObjectFields.SYS_ID, "jbossorg_contributor_profile-" + username);

		profileData.put("name", map.get("name"));
		profileData.put("displayName", map.get("displayName"));
		profileData.put(ContentObjectFields.SYS_TITLE, map.get("displayName"));

		profileData.put(ContentObjectFields.TAGS, map.get("tags"));
		profileData.put(ContentObjectFields.SYS_TAGS, map.get("tags"));

		profileData.put("published", map.get("published"));
		profileData.put(ContentObjectFields.SYS_CREATED, map.get("published"));

		profileData.put("updated", map.get("updated"));
		profileData.put(ContentObjectFields.SYS_UPDATED, new Date());

		Map<String, Object> resourcesObject = (Map<String, Object>) map.get("resources");
		try {
			String profileUrl = ((Map<String, Object>) resourcesObject.get("html")).get("ref").toString();
			profileData.put("profileUrl", profileUrl);
			profileData.put("sys_url_view", profileUrl);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot get profile URL for usrname: {0}", username);
		}

		profileData.put("timeZone", jiveObject.get("timeZone"));

		profileData.put("thumbnailUrl", map.get("thumbnailUrl"));

		List<Map<String, Object>> accounts = new ArrayList<>();

		List<Map<String, Object>> jiveProfile = (List<Map<String, Object>>) jiveObject.get("profile");
		for (Map<String, Object> p : jiveProfile) {
			switch ((String) p.get(JIVE_PROFILE_NAME_KEY)) {
			case "Biography":
				profileData.put("aboutMe", p.get(JIVE_PROFILE_VALUE_KEY));
				profileData.put("sys_description", p.get(JIVE_PROFILE_VALUE_KEY));
				break;
			case "Twitter Username":
				accounts.add(getAccountObject("twitter.com", p.get(JIVE_PROFILE_VALUE_KEY)));
				break;
			case "github Username":
				accounts.add(getAccountObject("github.com", p.get(JIVE_PROFILE_VALUE_KEY)));
				break;
			case "Facebook Username":
				accounts.add(getAccountObject("facebook.com", p.get(JIVE_PROFILE_VALUE_KEY)));
				break;
			}
		}
		profileData.put("accounts", accounts);

		return profileData;
	}

	protected Map<String, Object> getAccountObject(String domainName, Object value) {
		Map<String, Object> a = new HashMap<>(2);
		a.put("domain", domainName);
		a.put("username", value);
		return a;
	}

	/**
	 * Safe getter for <code>jive.profile</code> field value.
	 * 
	 * @param jiveObject to get profile value from
	 * @param jiveLabel <code>jive_label</code> for profile field value we can obtain
	 * @return profile field value or null
	 */
	@SuppressWarnings("unchecked")
	protected String getProfileValue(Map<String, Object> jiveObject, String jiveLabel) {
		if (jiveObject == null)
			return null;
		try {
			List<Map<String, Object>> profileObject = (List<Map<String, Object>>) jiveObject.get("profile");
			if (profileObject != null) {
				for (Map<String, Object> profileItem : profileObject) {
					if (jiveLabel.equals(profileItem.get(JIVE_PROFILE_NAME_KEY))) {
						return (String) profileItem.get(JIVE_PROFILE_VALUE_KEY);
					}
				}
			}
		} catch (ClassCastException e) {
			log.warning("bad structure of jive profile data");
		}
		return null;
	}

	/**
	 * Get list of email addresses from JIVE profile <code>emails</code> structure.
	 * 
	 * @param emailsObject JIVE profile <code>emails</code> structure
	 * @return list of emails. never null.
	 */
	protected List<String> getEmails(List<Map<String, Object>> emailsObject) {
		List<String> ret = new ArrayList<>();
		if (emailsObject != null) {
			for (Map<String, Object> emailObject : emailsObject) {
				String email = SearchUtils.trimToNull((String) emailObject.get("value"));
				if (email != null && !ret.contains(email)) {
					ret.add(email);
				}
			}
		}
		return ret;
	}

	/**
	 * @param typeSpecificCodes structure to add code into
	 * @param fieldTcsName name of Type Specific Code to add
	 * @param value of code. May be null or empty - ignored in this case.
	 */

	protected void addTypeSpecificCode(Map<String, List<String>> typeSpecificCodes, String fieldTcsName, String value) {
		value = SearchUtils.trimToNull(value);
		if (value != null) {
			List<String> vl = typeSpecificCodes.get(fieldTcsName);
			if (vl == null) {
				vl = new ArrayList<>();
				typeSpecificCodes.put(fieldTcsName, vl);
			}
			if (!vl.contains(value))
				vl.add(value);
		}
	}

	/**
	 * Get primary email address from JIVE profile <code>emails</code> structure.
	 * 
	 * @param emailsObject JIVE profile <code>emails</code> structure.
	 * @return primary emaill address or null if not found
	 */
	protected String getPrimaryEmail(List<Map<String, Object>> emailsObject) {
		if (emailsObject == null) {
			log.log(Level.FINE, "Emails not returned in response from Jive. Probably bad authentication.");
			return null;
		}
		for (Map<String, Object> emailObject : emailsObject) {
			String email = (String) emailObject.get("value");
			if ((boolean) emailObject.get("primary")) {
				return email;
			}
		}
		return null;
	}
}
