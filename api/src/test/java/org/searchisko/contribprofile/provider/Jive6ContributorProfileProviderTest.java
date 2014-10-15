/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.SettingsException;
import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.model.AppConfiguration;
import org.searchisko.api.model.AppConfiguration.ContributorProfileProviderConfig;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.contribprofile.model.ContributorProfile;

/**
 * Unit test for {@link Jive6ContributorProfileProvider}.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class Jive6ContributorProfileProviderTest {

	public static void main(String[] args) {
		Jive6ContributorProfileProvider provider = new Jive6ContributorProfileProvider();
		provider.appConfiguration = new AppConfiguration("adp");
		String providerUsername = "";
		String providerPassword = "";

		ContributorProfileProviderConfig contributorProfileProviderConfig = new ContributorProfileProviderConfig(
				"https://developer.jboss.org", providerUsername, providerPassword);
		provider.appConfiguration.setContributorProfileProviderConfig(contributorProfileProviderConfig);

		provider.init();

		// Set up logging
		provider.log = Logger.getLogger(Jive6ContributorProfileProvider.class.getName());
		provider.log.setLevel(Level.FINEST);

		Logger rootLogger = Logger.getLogger("");
		StreamHandler handler = new StreamHandler(System.out, new SimpleFormatter());
		handler.setLevel(Level.FINEST);
		rootLogger.addHandler(handler);

		ContributorProfile profile = provider.getProfile("lkrzyzanek");
		provider.destroy();

		System.out.println("profile: " + profile);
	}

	public Jive6ContributorProfileProvider getTested() {
		Jive6ContributorProfileProvider tested = new Jive6ContributorProfileProvider();
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	@SuppressWarnings("deprecation")
	@Test(expected = RuntimeException.class)
	public void convertJSONMap_jiveDataError1() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		provider.convertJSONMap(IOUtils.toByteArray(""));
	}

	@Test
	public void testConvertToProfile() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		InputStream is = Jive6ContributorProfileProviderTest.class.getResourceAsStream("Jive6ProfileData.json");
		ContributorProfile profile = provider.convertToProfile(IOUtils.toByteArray(is));
		Assert.assertEquals("lkrzyzanek",
				profile.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME).get(0));
		Assert.assertEquals("lkrzyzanek-ght",
				profile.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_GITHUB_USERNAME).get(0));

		Assert.assertEquals("Libor Krzyzanek", profile.getFullName());
		Assert.assertEquals("fake@fake.com", profile.getPrimaryEmail());

		Assert.assertEquals(2, profile.getEmails().size());
		Assert.assertTrue(profile.getEmails().contains("fake@fake.com"));
		Assert.assertTrue(profile.getEmails().contains("fake2@fake.com"));

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		calendar.set(Calendar.MILLISECOND, 0);

		calendar.set(2014, Calendar.OCTOBER, 25, 0, 0, 0);
		Assert.assertEquals(new Long(calendar.getTimeInMillis()), profile.getHireDate());
		calendar.set(2014, Calendar.NOVEMBER, 19, 0, 0, 0);
		Assert.assertEquals(new Long(calendar.getTimeInMillis()), profile.getLeaveDate());

		Map<String, Object> contributorProfile = profile.getProfileData();

		// sys_updated is not exactly tested because it contains current time. We test only presence in data.
		Assert.assertNotNull(contributorProfile.remove(ContentObjectFields.SYS_UPDATED));

		TestUtils.assertJsonContent(
				TestUtils.loadJSONFromClasspathFile("/org/searchisko/contribprofile/provider/Jive6ProfileDataConverted.json"),
				contributorProfile);
	}

	@Test(expected = SettingsException.class)
	public void testConvertToProfile_emptyEmail() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		InputStream is = Jive6ContributorProfileProviderTest.class.getResourceAsStream("Jive6ProfileData_emptyEmail.json");
		provider.convertToProfile(IOUtils.toByteArray(is));
	}

	@Test
	public void testConvertToProfile_emptyUsernames() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		InputStream is = Jive6ContributorProfileProviderTest.class
				.getResourceAsStream("Jive6ProfileData_emptyUsernames.json");
		ContributorProfile profile = provider.convertToProfile(IOUtils.toByteArray(is));
		Assert.assertEquals("lkrzyzanek",
				profile.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME).get(0));
		Assert.assertNull(profile.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_GITHUB_USERNAME));

		Assert.assertEquals("Libor Krzyzanek", profile.getFullName());
		Assert.assertEquals("fake@fake.com", profile.getPrimaryEmail());

		Assert.assertEquals(2, profile.getEmails().size());
		Assert.assertTrue(profile.getEmails().contains("fake@fake.com"));
		Assert.assertTrue(profile.getEmails().contains("fake2@fake.com"));

		Map<String, Object> contributorProfile = profile.getProfileData();

		// sys_updated is not exactly tested because it contains current time. We test only presence in data.
		Assert.assertNotNull(contributorProfile.remove(ContentObjectFields.SYS_UPDATED));

		TestUtils
				.assertJsonContent(
						TestUtils
								.loadJSONFromClasspathFile("/org/searchisko/contribprofile/provider/Jive6ProfileDataConverted_emptyUsernames.json"),
						contributorProfile);
	}

	@Test
	public void getEmails() {
		Jive6ContributorProfileProvider tested = getTested();

		// case - never null returned!
		Assert.assertEquals(0, tested.getEmails(null).size());

		List<Map<String, Object>> emailsObject = new ArrayList<>();
		Assert.assertEquals(0, tested.getEmails(emailsObject).size());

		emailsObject.add(createEmailStructure("test@test.com", false));
		Assert.assertTrue(tested.getEmails(emailsObject).contains("test@test.com"));

		// case - test multiple emails, email value trim and duplicity remove
		emailsObject.add(createEmailStructure("", false));
		emailsObject.add(createEmailStructure("test@test.com ", false));
		emailsObject.add(createEmailStructure(" test2@test.com ", false));
		Assert.assertEquals(2, tested.getEmails(emailsObject).size());
		Assert.assertTrue(tested.getEmails(emailsObject).contains("test@test.com"));
		Assert.assertTrue(tested.getEmails(emailsObject).contains("test2@test.com"));

	}

	@Test
	public void getPrimaryEmail() {
		Jive6ContributorProfileProvider tested = getTested();

		Assert.assertNull(tested.getPrimaryEmail(null));

		List<Map<String, Object>> emailsObject = new ArrayList<>();
		Assert.assertNull(tested.getPrimaryEmail(emailsObject));

		emailsObject.add(createEmailStructure("test@test.com", false));
		Assert.assertNull(tested.getPrimaryEmail(emailsObject));

		emailsObject.add(createEmailStructure("test2@test.com", true));
		Assert.assertEquals("test2@test.com", tested.getPrimaryEmail(emailsObject));

	}

	private Map<String, Object> createEmailStructure(String email, boolean primary) {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("jive_label", "Email");
		ret.put("type", "work");
		ret.put("value", email);
		ret.put("primary", primary);
		return ret;
	}

	@Test
	public void getProfileValue() {
		Jive6ContributorProfileProvider tested = getTested();

		Assert.assertNull(tested.getProfileValue(null, "testlabel"));

		Map<String, Object> jiveObject = new HashMap<>();
		Assert.assertNull(tested.getProfileValue(jiveObject, "testlabel"));

		jiveObject.put("profile", "badtype");
		Assert.assertNull(tested.getProfileValue(jiveObject, "testlabel"));

		List<Map<String, Object>> profileObject = new ArrayList<Map<String, Object>>();
		jiveObject.put("profile", profileObject);
		Assert.assertNull(tested.getProfileValue(jiveObject, "testlabel"));

		profileObject.add(createProfileValueStructure("field1", "value1"));
		Assert.assertNull(tested.getProfileValue(jiveObject, "testlabel"));

		profileObject.add(createProfileValueStructure("testlabel", "testvalue"));
		Assert.assertEquals("testvalue", tested.getProfileValue(jiveObject, "testlabel"));

	}

	private Map<String, Object> createProfileValueStructure(String itemLabel, Object itemValue) {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("jive_label", itemLabel);
		ret.put("value", itemValue);
		return ret;
	}

	@Test
	public void addTypeSpecificCode() {
		Jive6ContributorProfileProvider tested = getTested();

		Map<String, List<String>> typeSpecificCodes = new HashMap<>();

		// case - null and empty values ignored (even no list created in map!)
		tested.addTypeSpecificCode(typeSpecificCodes, "a", null);
		Assert.assertTrue(typeSpecificCodes.isEmpty());
		tested.addTypeSpecificCode(typeSpecificCodes, "a", " ");
		Assert.assertTrue(typeSpecificCodes.isEmpty());

		// case - values added, trimmed, same values deduplicated
		tested.addTypeSpecificCode(typeSpecificCodes, "a", "v1");
		tested.addTypeSpecificCode(typeSpecificCodes, "a", " v2 ");
		tested.addTypeSpecificCode(typeSpecificCodes, "a", "v1");

		tested.addTypeSpecificCode(typeSpecificCodes, "b", "v1");
		tested.addTypeSpecificCode(typeSpecificCodes, "b", "v1 ");

		Assert.assertEquals(2, typeSpecificCodes.size());
		Assert.assertEquals(2, typeSpecificCodes.get("a").size());
		Assert.assertTrue(typeSpecificCodes.get("a").contains("v1"));
		Assert.assertTrue(typeSpecificCodes.get("a").contains("v2"));

		Assert.assertEquals(1, typeSpecificCodes.get("b").size());
		Assert.assertTrue(typeSpecificCodes.get("b").contains("v1"));

	}

	@Test
	public void testConvertToProfiles() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		InputStream is = Jive6ContributorProfileProviderTest.class.getResourceAsStream("Jive6AllProfilesData.json");

		List<ContributorProfile> profiles = provider.convertToProfiles(IOUtils.toByteArray(is));
		ContributorProfile profile1 = profiles.get(0);

		Assert.assertEquals("Danielsds",
				profile1.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME).get(0));

		Assert.assertEquals("Danielsds", profile1.getFullName());
		Assert.assertEquals("danielsds@fake.com", profile1.getPrimaryEmail());

		ContributorProfile profile2 = profiles.get(1);

		Assert.assertEquals("dppsp",
				profile2.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME).get(0));

		Assert.assertEquals("dppsp", profile2.getFullName());
		Assert.assertEquals("patrick@fake.com", profile2.getPrimaryEmail());
	}
}
