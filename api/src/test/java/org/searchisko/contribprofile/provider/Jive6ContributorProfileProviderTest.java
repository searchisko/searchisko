/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.searchisko.contribprofile.model.ContributorProfile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Libor Krzyzanek
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


	@Test
	public void testMapRawJsonData() throws Exception {
		Jive6ContributorProfileProvider provider = new Jive6ContributorProfileProvider();

		InputStream is = Jive6ContributorProfileProviderTest.class.getResourceAsStream("Jive6ProfileData.json");
		ContributorProfile profile = provider.mapRawJsonData(IOUtils.toByteArray(is));
		Assert.assertEquals("lkrzyzanek", profile.getJbossorgUsername());
		Assert.assertEquals("Libor Krzyzanek <fake@fake.com>", profile.getContributorId());
		List<Map<String, Object>> jiveProfile = (List<Map<String, Object>>) profile.getJiveProfile().get("profile");
		for (Map<String, Object> p : jiveProfile) {
			if (p.get("jive_label").equals("Biography")) {
				Assert.assertEquals("BIO", p.get("value"));
			}
		}
	}
}
