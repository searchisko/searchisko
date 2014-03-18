/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.service.ContributorService;
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
		provider.init();
		provider.log = Logger.getLogger(Jive6ContributorProfileProvider.class.getName());

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
	public void mapRawJsonData_jiveDataError1() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		provider.mapRawJsonData(IOUtils.toByteArray(""));

	}

	@Test
	public void mapRawJsonData() throws Exception {
		Jive6ContributorProfileProvider provider = getTested();

		InputStream is = Jive6ContributorProfileProviderTest.class.getResourceAsStream("Jive6ProfileData.json");
		ContributorProfile profile = provider.mapRawJsonData(IOUtils.toByteArray(is));
		Assert.assertEquals("lkrzyzanek",
				profile.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME).get(0));
		Assert.assertEquals("lkrzyzanek-ght",
				profile.getTypeSpecificCodes().get(ContributorProfileService.FIELD_TSC_GITHUB_USERNAME).get(0));

		Assert.assertEquals("Libor Krzyzanek", profile.getFullName());
		Assert.assertEquals("fake@fake.com", profile.getPrimaryEmail());

		Assert.assertEquals(2, profile.getEmails().size());
		Assert.assertTrue(profile.getEmails().contains("fake@fake.com"));
		Assert.assertTrue(profile.getEmails().contains("fake2@fake.com"));

		// TEST Contributor Profile
		Map<String, Object> contributorProfile = profile.getProfileData();

		// It's needed to manually add contributor ID because it's added in ContributorProfileService
		Map<String, Object> profileData = profile.getProfileData();
		List<String> contributors = new ArrayList<>(1);
		contributors.add(ContributorService.createContributorId(profile.getFullName(), profile.getPrimaryEmail()));

		profileData.put(ContentObjectFields.SYS_CONTRIBUTORS, contributors);

		// sys_updated is not tested because it contains current time
		contributorProfile.remove(ContentObjectFields.SYS_UPDATED);

		TestUtils.assertJsonContent(
				TestUtils.loadJSONFromClasspathFile("/org/searchisko/contribprofile/provider/Jive6ProfileDataConverted.json"),
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
}
