/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.api.util.Resources;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.contribprofile.provider.Jive6ContributorProfileProvider;

/**
 * Unit test for {@link ContributorProfileService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorProfileServiceTest extends ESRealClientTestBase {

	private static final String CODE_3 = "jan doe <test@test.org>";
	private static final String CODE_2 = "john doe 2 <test2@test.org>";
	private static final String CODE_1 = "john doe <test@test.org>";

	@Test
	public void isContributorCodeTypesSupported() {
		ContributorProfileService tested = getTested(null);
		Assert.assertFalse(tested.isContributorCodeTypesSupported(null));
		Assert.assertFalse(tested.isContributorCodeTypesSupported(""));
		Assert.assertFalse(tested.isContributorCodeTypesSupported("unknown"));
		Assert.assertTrue(tested.isContributorCodeTypesSupported(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME));

	}

	@Test
	public void takeProfileFromProvider() {
		ContributorProfileService tested = getTested(null);

		// case - supported profile provider - profile not loaded
		Mockito.when(tested.contributorProfileProvider.getProfile("aaa")).thenReturn(null);
		Assert.assertNull(tested.takeProfileFromProvider(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "aaa"));

		// case - supported profile provider - profile loaded
		Mockito.reset(tested.contributorProfileProvider);
		ContributorProfile pm = Mockito.mock(ContributorProfile.class);
		Mockito.when(tested.contributorProfileProvider.getProfile("aaa")).thenReturn(pm);
		Assert.assertEquals(pm,
				tested.takeProfileFromProvider(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "aaa"));

		// case - unsupported profile provider
		try {
			tested.takeProfileFromProvider(ContributorProfileService.FIELD_TSC_GITHUB_USERNAME, "aaa");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}

	}

	@Test
	public void findByContributorCode() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorProfileServiceTest");
		ContributorProfileService tested = getTested(client);
		try {

			// case - search from noexisting index
			{
				indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
				Assert.assertNull(tested.findByContributorCode(CODE_1));
				Assert.assertNull(tested.findByContributorCode(CODE_1));
			}

			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			initIndex(client);
			Thread.sleep(100);

			// case - search from empty index
			{
				SearchResponse sr = tested.findByContributorCode(CODE_1);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"10", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_2
							+ "\"],\"displayName\":\"John Doe 2\"}");
			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"20", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_1 + "\"],\"displayName\":\"John Doe\"}");
			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"30", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS
							+ "\":[\"paul doe <t@te.org>\"],\"displayName\":\"Paul Doe\"}");
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByContributorCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("20", sr.getHits().getHits()[0].getId());

				sr = tested.findByContributorCode(CODE_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("10", sr.getHits().getHits()[0].getId());

				sr = tested.findByContributorCode(CODE_3);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_update_delete() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorProfileServiceTest_2");
		ContributorProfileService tested = getTested(client);
		try {

			// case - delete from noexisting index
			{
				indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
				Assert.assertFalse(tested.deleteByContributorCode(CODE_1));
			}

			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			initIndex(client);
			Thread.sleep(100);

			// case - delete from empty index
			{
				Assert.assertFalse(tested.deleteByContributorCode(CODE_1));
			}

			// case - create
			String id1 = "profileid1";
			{
				tested.updateSearchIndex(id1,
						TestUtils.loadJSONFromClasspathFile("/org/searchisko/contribprofile/profile1.json"));
				tested.updateSearchIndex("10",
						TestUtils.loadJSONFromClasspathFile("/org/searchisko/contribprofile/profile2.json"));

				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);

				SearchResponse sr = tested.findByContributorCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(id1, sr.getHits().getHits()[0].getId());

				sr = tested.findByContributorCode(CODE_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("10", sr.getHits().getHits()[0].getId());

			}

			// case - delete
			{
				tested.deleteByContributorCode(CODE_2);
				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
				SearchResponse sr = tested.findByContributorCode(CODE_2);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			// case - update
			{
				tested.updateSearchIndex(id1,
						TestUtils.loadJSONFromClasspathFile("/org/searchisko/contribprofile/profile2.json"));
				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
				SearchResponse sr = tested.findByContributorCode(CODE_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(id1, sr.getHits().getHits()[0].getId());
			}

		} finally {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void updateContributorProfileInSearchIndex() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorProfileServiceTest_3");
		ContributorProfileService tested = getTested(client);
		try {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			initIndex(client);
			Thread.sleep(100);

			// case - create new profile in index if not present
			{
				ContributorProfile profile = Mockito.mock(ContributorProfile.class);
				Map<String, Object> pd = new HashMap<String, Object>();
				pd.put("displayName", "John Doe");
				Mockito.when(profile.getProfileData()).thenReturn(pd);
				tested.updateContributorProfileInSearchIndex(CODE_1, profile);

				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);

				SearchResponse sr = tested.findByContributorCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("John Doe", sr.getHits().getHits()[0].getSource().get("displayName"));
			}

			// case - update profile
			{
				ContributorProfile profile = Mockito.mock(ContributorProfile.class);
				Map<String, Object> pd = new HashMap<String, Object>();
				pd.put("displayName", "John Doe 2");
				Mockito.when(profile.getProfileData()).thenReturn(pd);
				tested.updateContributorProfileInSearchIndex(CODE_1, profile);

				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);

				SearchResponse sr = tested.findByContributorCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("John Doe 2", sr.getHits().getHits()[0].getSource().get("displayName"));
			}

			// case - update profile and delete duplicit records
			{

				// add duplicit record which should be removed
				indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
						"20", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_1
								+ "\"],\"displayName\":\"John Doe 3\"}");
				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);

				ContributorProfile profile = Mockito.mock(ContributorProfile.class);
				Map<String, Object> pd = new HashMap<String, Object>();
				pd.put("displayName", "John Doe 3");
				Mockito.when(profile.getProfileData()).thenReturn(pd);
				tested.updateContributorProfileInSearchIndex(CODE_1, profile);

				indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);

				SearchResponse sr = tested.findByContributorCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("John Doe 3", sr.getHits().getHits()[0].getSource().get("displayName"));
			}

		} finally {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void contributorDeletedEventHandler() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorProfileServiceTest_4");
		ContributorProfileService tested = getTested(client);
		try {
			initIndex(client);
			Thread.sleep(100);

			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"20", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_1 + "\"],\"displayName\":\"John Doe\"}");
			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"30", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_2
							+ "\"],\"displayName\":\"John Doe 2\"}");

			// case - no Exception for invalid events
			tested.contributorDeletedEventHandler(null);
			tested.contributorDeletedEventHandler(new ContributorDeletedEvent(null, null));
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			Assert.assertEquals(1, tested.findByContributorCode(CODE_1).getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByContributorCode(CODE_2).getHits().getTotalHits());

			// case - delete profile if event is valid
			tested.contributorDeletedEventHandler(new ContributorDeletedEvent("someid", CODE_1));
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			Assert.assertEquals(0, tested.findByContributorCode(CODE_1).getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByContributorCode(CODE_2).getHits().getTotalHits());

		} finally {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void contributorMergedEventHandler() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorProfileServiceTest_5");
		ContributorProfileService tested = getTested(client);
		try {
			initIndex(client);
			Thread.sleep(100);

			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"20", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_1 + "\"],\"displayName\":\"John Doe\"}");
			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"30", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_2
							+ "\"],\"displayName\":\"John Doe 2\"}");

			// case - no Exception for invalid events
			tested.contributorMergedEventHandler(null);
			tested.contributorMergedEventHandler(new ContributorMergedEvent(null, null));
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			Assert.assertEquals(1, tested.findByContributorCode(CODE_1).getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByContributorCode(CODE_2).getHits().getTotalHits());

			// case - delete profile if event is valid
			tested.contributorMergedEventHandler(new ContributorMergedEvent(CODE_1, CODE_2));
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			Assert.assertEquals(0, tested.findByContributorCode(CODE_1).getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByContributorCode(CODE_2).getHits().getTotalHits());

		} finally {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void contributorCodeChangedEventHandler() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorProfileServiceTest_6");
		ContributorProfileService tested = getTested(client);
		try {
			initIndex(client);
			Thread.sleep(100);

			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"20", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_1 + "\"],\"displayName\":\"John Doe\"}");
			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"30", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_2
							+ "\"],\"displayName\":\"John Doe 2\"}");
			indexInsertDocument(ContributorProfileService.SEARCH_INDEX_NAME, ContributorProfileService.SEARCH_INDEX_TYPE,
					"40", "{\"" + ContentObjectFields.SYS_CONTRIBUTORS + "\":[\"" + CODE_1
							+ "\"],\"displayName\":\"John Doe 3\"}");

			// case - no Exception for invalid events
			tested.contributorCodeChangedEventHandler(null);
			tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(null, null));
			tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(CODE_1, null));
			tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(null, CODE_2));
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			Assert.assertEquals(2, tested.findByContributorCode(CODE_1).getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByContributorCode(CODE_2).getHits().getTotalHits());

			// case - change code in profile if event is valid, remove all duplicit profiles (from old and new code)
			tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(CODE_1, CODE_2));
			indexFlushAndRefresh(ContributorProfileService.SEARCH_INDEX_NAME);
			Assert.assertEquals(0, tested.findByContributorCode(CODE_1).getHits().getTotalHits());
			SearchResponse sh = tested.findByContributorCode(CODE_2);
			Assert.assertEquals(1, sh.getHits().getTotalHits());
			Assert.assertEquals("20", sh.getHits().getHits()[0].getId());

		} finally {
			indexDelete(ContributorProfileService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private ContributorProfileService getTested(Client client) {
		if (client == null)
			client = Mockito.mock(Client.class);

		ContributorProfileService ret = new ContributorProfileService();
		ret.log = Logger.getLogger("testlogger");
		ret.contributorService = Mockito.mock(ContributorService.class);
		ret.contributorProfileProvider = Mockito.mock(Jive6ContributorProfileProvider.class);
		ret.searchClientService = new SearchClientService();
		ret.searchClientService.log = Logger.getLogger("testlogger");
		ret.searchClientService.client = client;

		return ret;
	}

	private void initIndex(Client client) throws ElasticsearchException, IOException {
		client.admin().indices().prepareCreate(ContributorProfileService.SEARCH_INDEX_NAME).execute().actionGet();
		client
				.admin()
				.indices()
				.preparePutMapping(ContributorProfileService.SEARCH_INDEX_NAME)
				.setType(ContributorProfileService.SEARCH_INDEX_TYPE)
				.setSource(
						Resources.readStringFromClasspathFile("/org/searchisko/contribprofile/mapping_contributor_profile.json"))
				.execute().actionGet();
	}

}
