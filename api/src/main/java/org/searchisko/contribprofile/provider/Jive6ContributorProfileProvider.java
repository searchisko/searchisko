/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.common.settings.SettingsException;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.model.AppConfiguration;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;

/**
 * Jive 6 implementation of Contributor Provider. <br/>
 * Documentation for Jive 6 REST API: https://developers.jivesoftware.com/api/v3/rest/PersonService.html Access to Jive
 * 6 has to be authenticated. See AppConfiguration
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@Stateless
@LocalBean
public class Jive6ContributorProfileProvider implements ContributorProfileProvider {

	protected static final String DOMAIN_JBOSS_ORG = "jboss.org";
	protected static final String DOMAIN_GOOGLE_COM = "google.com";
	protected static final String DOMAIN_LINKEDIN_COM = "linkedin.com";
	protected static final String DOMAIN_FACEBOOK_COM = "facebook.com";
	protected static final String DOMAIN_GITHUB_COM = "github.com";
	protected static final String DOMAIN_TWITTER_COM = "twitter.com";

	protected static final String HIRE_DATE_KEY = "Hire Date";
	protected static final String LEAVE_DATE_KEY = "Leaving Date";
	protected static final String DATE_PATTERN = "MM/dd/yyyy";
	
	private static final Map<String,String> countryNameToCode;
	
	static {
		Map<String,String> tempCountryNameToCode = new HashMap<String, String>();
		for( String isoCountry : Locale.getISOCountries() ) {
			tempCountryNameToCode.put(new Locale("",isoCountry).getDisplayCountry(Locale.US), isoCountry);
		}
		countryNameToCode = Collections.unmodifiableMap(tempCountryNameToCode);
	}
	
	@Inject
	protected Logger log;

	public static final String JIVE_PROFILE_REST_API = "/api/core/v3/people/username/";

	public static final String JIVE_ALL_PROFILES_REST_API = "/api/core/v3/people/?fields=jive,username,name,emails,displayName,tags,updated,resources,thumbnailUrl,published,addresses";

	@Inject
	protected AppConfiguration appConfiguration;

	protected CloseableHttpClient httpClient;

	@PostConstruct
	public void init() {
		httpClient = HttpClients.custom().build();
	}

	@Override
	public List<ContributorProfile> getAllProfiles(Integer start, Integer size) {
		String jive6Url = appConfiguration.getContributorProfileProviderConfig().getUrlbase();

		String url = jive6Url + JIVE_ALL_PROFILES_REST_API;
		url = addPaginationToUrl(url, start, size);

		log.log(Level.FINE, "Get data from Jive using url: {0}", url);

		byte[] data = getData(url);
		if (data == null) {
			return null;
		}

		return convertToProfiles(data);
	}

	@SuppressWarnings("unchecked")
	protected List<ContributorProfile> convertToProfiles(byte[] data) {
		List<ContributorProfile> ret = new LinkedList<>();

		Map<String, Object> map = convertJSONMap(data);
		List<Map<String, Object>> profiles = (List<Map<String, Object>>) map.get("list");
		for (Map<String, Object> profile : profiles) {
			ContributorProfile contributorProfile = mapRawJsonData(profile);
			ret.add(contributorProfile);
		}
		return ret;
	}

	protected String addPaginationToUrl(String url, Integer start, Integer size) {
		if (start != null) {
			url += "&startIndex=" + start;
		}
		if (size != null) {
			url += "&count=" + size;
		}
		return url;
	}

	protected byte[] getData(String url) {
		String username = appConfiguration.getContributorProfileProviderConfig().getUsername();
		String password = appConfiguration.getContributorProfileProviderConfig().getPassword();

		return getData(url, username, password);
	}

	/**
	 * Get data from provider
	 *
	 * @param url
	 * @param username
	 * @param password
	 * @return data or null if something goes wrong.
	 */
	protected byte[] getData(String url, String username, String password) {
		if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
			log.log(Level.SEVERE, "Jive provider configuration has username and password blank.");
			return null;
		}

		HttpGet httpGet = new HttpGet(url);

		CloseableHttpResponse response = null;
		try {
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
			BasicScheme bs = new BasicScheme();
			httpGet.addHeader(bs.authenticate(credentials, httpGet, null));
			response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() >= 300) {
				String output = EntityUtils.toString(entity);
				log.log(Level.WARNING, "Cannot get data from Jive, response: {0}, code: {1}", new Object[] { output,
						response.getStatusLine().getStatusCode() });
				return null;
			}
			byte[] data = EntityUtils.toByteArray(entity);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "data from Jive: {0}", new String(data));
			}
			return data;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cannot get date from Jive", e);
			return null;
		} catch (AuthenticationException e) {
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				try {
					response.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	@Override
	public ContributorProfile getProfile(String jbossorgUsername) {
		String jive6Url = appConfiguration.getContributorProfileProviderConfig().getUrlbase();

		byte[] data = getData(jive6Url + JIVE_PROFILE_REST_API + jbossorgUsername);
		if (data == null) {
			return null;
		}
		return convertToProfile(data);
	}

	protected ContributorProfile convertToProfile(byte[] data) {
		Map<String, Object> map = convertJSONMap(data);
		return mapRawJsonData(map);
	}

	@PreDestroy
	public void destroy() {
		try {
			httpClient.close();
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

	private static final byte FIRST_RESPONSE_BYTE = "{".getBytes()[0];

	protected Map<String, Object> convertJSONMap(byte[] data) {
		ObjectMapper mapper = new ObjectMapper();
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
			return mapper.readValue(data, startOffset, data.length, new TypeReference<Map<String, Object>>() {
			});

		} catch (IOException e) {
			String msg = "Cannot parse Jive 6 profile json data: " + e.getMessage();
			log.log(Level.WARNING, msg);
			throw new RuntimeException(msg);
		}
	}

	@SuppressWarnings("unchecked")
	protected ContributorProfile mapRawJsonData(Map<String, Object> map) {
		Map<String, Object> jiveObject = (Map<String, Object>) map.get("jive");
		Map<String, Object> nameObject = (Map<String, Object>) map.get("name");
		List<Map<String, Object>> emailsObject = (List<Map<String, Object>>) map.get("emails");

		Map<String, List<String>> typeSpecificCodes = new HashMap<>();
		// TODO CONTRIBUTOR_PROFILE Jive6 provider make type_specific_code obtaining from jive data configurable
		addTypeSpecificCode(typeSpecificCodes, ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
				(String) jiveObject.get("username"));
		addTypeSpecificCode(typeSpecificCodes, ContributorProfileService.FIELD_TSC_GITHUB_USERNAME,
				getProfileValue(jiveObject, "github Username"));

		Map<String, Object> profileData = mapProfileData(map, jiveObject);

		String primaryEmail = SearchUtils.trimToNull(getPrimaryEmail(emailsObject));
		if (primaryEmail == null) {
			throw new SettingsException(
					"Jive Contributor Profile primary email is missing, probably due incorrect permissions in Jive.");
		}
		String fullName = (String) nameObject.get("formatted");
		if (StringUtils.isBlank(fullName)) {
			fullName = (String) map.get("displayName");
		}

		Long hireDate = (Long) profileData.remove(HIRE_DATE_KEY);
		Long leaveDate = (Long) profileData.remove(LEAVE_DATE_KEY);

		ContributorProfile profile = new ContributorProfile((String) profileData.get(ContentObjectFields.SYS_ID), fullName,
				primaryEmail, getEmails(emailsObject), typeSpecificCodes, hireDate, leaveDate);

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
		profileData.put(ContentObjectFields.SYS_CONTENT_ID, username);

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
		
		if( map.get("addresses")!=null) {
			
			List<Map<String,Object>> addressesList = (List<Map<String, Object>>) map.get("addresses");
			String country = null;
			for ( Map<String,Object> address : addressesList ) {
				
				Map<String,Object> addressFields = (Map<String,Object>)address.get(JIVE_PROFILE_VALUE_KEY);
				
				if( addressFields==null ) continue;
				
				if( addressFields.containsKey("country") && addressFields.get("country").toString().trim().length()>0 ) {
					
					country = addressFields.get("country").toString();
					
					// We prefer home address over other so if we found country in it we can finish searching.
					if( address.get(JIVE_PROFILE_NAME_KEY).toString().compareTo("Home Address")!=0 ) {
						break;
					}
				}
			}
			
			if( country!=null && countryNameToCode.containsKey(country) ) {
				profileData.put( "country" , countryNameToCode.get(country) );
			}
		}

		Map<String, Object> resourcesObject = (Map<String, Object>) map.get("resources");
		String profileUrl = null;
		try {
			profileUrl = ((Map<String, Object>) resourcesObject.get("html")).get("ref").toString();
			profileData.put("profileUrl", profileUrl);
			profileData.put(ContentObjectFields.SYS_URL_VIEW, profileUrl);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot get profile URL for username: {0}", username);
		}

		profileData.put("timeZone", jiveObject.get("timeZone"));

		profileData.put("thumbnailUrl", map.get("thumbnailUrl"));

		Map<String, Map<String, Object>> accounts = new LinkedHashMap<>();

		List<Map<String, Object>> jiveProfile = (List<Map<String, Object>>) jiveObject.get("profile");
		if (jiveProfile != null) {
			for (Map<String, Object> p : jiveProfile) {
				String profileNameKey = (String) p.get(JIVE_PROFILE_NAME_KEY);
				switch (profileNameKey) {
				case "Biography":
					profileData.put("aboutMe", p.get(JIVE_PROFILE_VALUE_KEY));
					profileData.put("sys_description", p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Twitter Username":
					storeAccountInfo(accounts, DOMAIN_TWITTER_COM, DCP_PROFILE_ACCOUNT_USERNAME, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Twitter URL":
					storeAccountInfo(accounts, DOMAIN_TWITTER_COM, DCP_PROFILE_ACCOUNT_LINK, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "github Username":
					storeAccountInfo(accounts, DOMAIN_GITHUB_COM, DCP_PROFILE_ACCOUNT_USERNAME, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Github Profile":
					storeAccountInfo(accounts, DOMAIN_GITHUB_COM, DCP_PROFILE_ACCOUNT_LINK, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Facebook Username":
					storeAccountInfo(accounts, DOMAIN_FACEBOOK_COM, DCP_PROFILE_ACCOUNT_USERNAME, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Facebook Profile":
					storeAccountInfo(accounts, DOMAIN_FACEBOOK_COM, DCP_PROFILE_ACCOUNT_LINK, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "LinkedIn Username":
					storeAccountInfo(accounts, DOMAIN_LINKEDIN_COM, DCP_PROFILE_ACCOUNT_USERNAME, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "LinkedIn Profile":
					storeAccountInfo(accounts, DOMAIN_LINKEDIN_COM, DCP_PROFILE_ACCOUNT_LINK, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Google Profile":
					storeAccountInfo(accounts, DOMAIN_GOOGLE_COM, DCP_PROFILE_ACCOUNT_LINK, p.get(JIVE_PROFILE_VALUE_KEY));
					break;
				case "Company name":
				    profileData.put("company", p.get(JIVE_PROFILE_VALUE_KEY).toString() );
				    break;
				case HIRE_DATE_KEY:
				case LEAVE_DATE_KEY:
					String rawValue = (String) p.get(JIVE_PROFILE_VALUE_KEY);
					SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
					sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
					sdf.setLenient(false);
					try {
						profileData.put(profileNameKey, sdf.parse(rawValue).getTime());
					} catch (ParseException e) {
						// nothing to set
					}
					break;
				}
			}
		} else {
			log.log(Level.WARNING, "Missing ''profile'' part of data for username: {0}", username);
		}
		storeAccountInfo(accounts, DOMAIN_JBOSS_ORG, DCP_PROFILE_ACCOUNT_USERNAME, username);
		storeAccountInfo(accounts, DOMAIN_JBOSS_ORG, DCP_PROFILE_ACCOUNT_LINK, profileUrl);
		if (!accounts.isEmpty()) {
			profileData.put(DCP_PROFILE_ACCOUNTS, new ArrayList<>(accounts.values()));
		}

		return profileData;
	}

	protected void storeAccountInfo(Map<String, Map<String, Object>> accounts, String domainName, String infoKey,
			Object infoValue) {

		if (infoValue == null)
			return;

		String valueString = infoValue.toString();

		if (SearchUtils.isBlank(valueString))
			return;

		Map<String, Object> a = accounts.get(domainName);
		if (a == null) {
			a = new HashMap<>(3);
			a.put(DCP_PROFILE_ACCOUNT_DOMAIN, domainName);
			accounts.put(domainName, a);
		}
		a.put(infoKey, valueString);
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
	 * @return primary email address or null if not found
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
