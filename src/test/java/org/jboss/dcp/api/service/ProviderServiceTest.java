/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.persistence.service.ElasticsearchEntityService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ProviderService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProviderServiceTest extends ESRealClientTestBase {

	private static final String INDEX_TYPE = "index_type";
	private static final String INDEX_NAME = "index_name";

	@Test
	public void getEntityService() {
		ProviderService tested = new ProviderService();
		tested.entityService = Mockito.mock(ElasticsearchEntityService.class);
		Assert.assertEquals(tested.entityService, tested.getEntityService());
	}

	@Test
	public void generateDcpId() {
		ProviderService tested = new ProviderService();
		Assert.assertEquals("mytype-myid", tested.generateDcpId("mytype", "myid"));
		Assert.assertEquals("mytype-null", tested.generateDcpId("mytype", null));
		Assert.assertEquals("null-myid", tested.generateDcpId(null, "myid"));
		Assert.assertEquals("null-null", tested.generateDcpId(null, null));
	}

	@Test
	public void extractContentType() {
		Map<String, Object> providerDef = new HashMap<String, Object>();

		// case - type field not defined
		Assert.assertNull(ProviderService.extractContentType(providerDef, "mytype"));

		Map<String, Object> types = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, types);
		types.put("typeOk", new HashMap<String, Object>());
		types.put("typeBadStructure", "baad");

		// case - type not defined in type field
		Assert.assertNull(ProviderService.extractContentType(providerDef, "mytype"));

		// case - type found
		Assert.assertEquals(types.get("typeOk"), ProviderService.extractContentType(providerDef, "typeOk"));

		// case - bad configuration of concrete type
		try {
			ProviderService.extractContentType(providerDef, "typeBadStructure");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad configuration of type field
		providerDef.put(ProviderService.TYPE, "baaad");
		try {
			ProviderService.extractContentType(providerDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

	}

	@Test
	public void extractPreprocessors() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - preprocessors field not defined
		Assert.assertNull(ProviderService.extractPreprocessors(typeDef, "mytype"));

		// case - preprocessors field OK
		typeDef.put("input_preprocessors", new ArrayList<Map<String, Object>>());
		Assert.assertEquals(typeDef.get("input_preprocessors"), ProviderService.extractPreprocessors(typeDef, "mytype"));

		// case - preprocessors field with bad element type
		try {
			typeDef.put("input_preprocessors", "badstructureelement");
			ProviderService.extractPreprocessors(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			typeDef.put("input_preprocessors", new HashMap<String, Object>());
			ProviderService.extractPreprocessors(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void extractIndexName() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - index field not defined
		try {
			ProviderService.extractIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		Map<String, Object> indexElement = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, indexElement);
		// case - index field defined but name field is empty
		try {
			ProviderService.extractIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		indexElement.put(ProviderService.NAME, "myindex");
		// case - index name found correct found
		Assert.assertEquals(indexElement.get(ProviderService.NAME), ProviderService.extractIndexName(typeDef, "mytype"));

		// case - empty value for name element
		indexElement.put(ProviderService.NAME, "");
		try {
			ProviderService.extractIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - empty value for name element
		indexElement.put(ProviderService.NAME, "   ");
		try {
			ProviderService.extractIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for name element
		indexElement.put(ProviderService.NAME, new Integer(10));
		try {
			ProviderService.extractIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for index element
		typeDef.put(ProviderService.INDEX, "baaad");
		try {
			ProviderService.extractIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void extractSearchIndices() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - index field not defined
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for index element
		typeDef.put(ProviderService.INDEX, "baaad");
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		Map<String, Object> indexElement = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, indexElement);
		// case - index field defined but search_indices nor name field is empty
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		indexElement.put(ProviderService.NAME, "myindex");
		// case - search_indices not found but name found correct
		Assert.assertArrayEquals(new String[] { "myindex" }, ProviderService.extractSearchIndices(typeDef, "mytype"));

		// case - search_indices not found, empty value for name element
		indexElement.put(ProviderService.NAME, "");
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - search_indices not found, empty value for name element
		indexElement.put(ProviderService.NAME, "   ");
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - search_indices not found, bad type of value for name element
		indexElement.put(ProviderService.NAME, new Integer(10));
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		indexElement.put(ProviderService.NAME, "myindex");
		// case - search_indices contains one String
		indexElement.put(ProviderService.SEARCH_INDICES, "mysearchindex");
		Assert.assertArrayEquals(new String[] { "mysearchindex" }, ProviderService.extractSearchIndices(typeDef, "mytype"));

		// case - search_indices contains list of Strings
		List<String> lis = new ArrayList<String>();
		lis.add("mysearchindex");
		lis.add("mysearchindex2");
		indexElement.put(ProviderService.SEARCH_INDICES, lis);
		Assert.assertArrayEquals(new String[] { "mysearchindex", "mysearchindex2" },
				ProviderService.extractSearchIndices(typeDef, "mytype"));

		// case - search_indices with bad type of value
		indexElement.put(ProviderService.SEARCH_INDICES, new Integer(10));
		try {
			ProviderService.extractSearchIndices(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void extractSearchAllExcluded() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - search_all_excluded field not defined
		Assert.assertFalse(ProviderService.extractSearchAllExcluded(typeDef));

		// case - search_all_excluded field defined but empty
		typeDef.put(ProviderService.SEARCH_ALL_EXCLUDED, "");
		Assert.assertFalse(ProviderService.extractSearchAllExcluded(typeDef));

		// case - dcp_type field defined but invalid data type
		typeDef.put(ProviderService.SEARCH_ALL_EXCLUDED, new Integer(10));
		Assert.assertFalse(ProviderService.extractSearchAllExcluded(typeDef));

		// case - search_all_excluded correct found
		typeDef.put(ProviderService.SEARCH_ALL_EXCLUDED, "false");
		Assert.assertFalse(ProviderService.extractSearchAllExcluded(typeDef));
		typeDef.put(ProviderService.SEARCH_ALL_EXCLUDED, "False");
		Assert.assertFalse(ProviderService.extractSearchAllExcluded(typeDef));

		typeDef.put(ProviderService.SEARCH_ALL_EXCLUDED, "true");
		Assert.assertTrue(ProviderService.extractSearchAllExcluded(typeDef));
		typeDef.put(ProviderService.SEARCH_ALL_EXCLUDED, "True");
		Assert.assertTrue(ProviderService.extractSearchAllExcluded(typeDef));
	}

	@Test
	public void extractIndexType() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - index field not defined
		try {
			ProviderService.extractIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		Map<String, Object> indexElement = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, indexElement);
		// case - index field defined but type field is empty
		try {
			ProviderService.extractIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - index type found correct found
		indexElement.put(ProviderService.TYPE, "myidxtype");
		Assert.assertEquals(indexElement.get(ProviderService.TYPE), ProviderService.extractIndexType(typeDef, "mytype"));

		// case - empty value for type element
		indexElement.put(ProviderService.TYPE, "");
		try {
			ProviderService.extractIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - empty value for type element
		indexElement.put(ProviderService.TYPE, "  ");
		try {
			ProviderService.extractIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for type element
		indexElement.put(ProviderService.TYPE, new Integer(10));
		try {
			ProviderService.extractIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for index element
		typeDef.put(ProviderService.INDEX, "baaad");
		try {
			ProviderService.extractIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void extractDcpType() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - dcp_type field not defined
		try {
			ProviderService.extractDcpType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - dcp_type field defined but empty
		typeDef.put(ProviderService.DCP_TYPE, "");
		try {
			ProviderService.extractDcpType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - dcp_type field defined but empty
		typeDef.put(ProviderService.DCP_TYPE, "  ");
		try {
			ProviderService.extractDcpType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - index type found correct found
		typeDef.put(ProviderService.DCP_TYPE, "mydcptype");
		Assert.assertEquals(typeDef.get(ProviderService.DCP_TYPE), ProviderService.extractDcpType(typeDef, "mytype"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void runPreprocessors() {
		ProviderService tested = new ProviderService();
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Client client = Mockito.mock(Client.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(client);

		// case - no NPE on empty both preprocessors defs and data
		tested.runPreprocessors("mytype", null, null);
		tested.runPreprocessors("mytype", new ArrayList<Map<String, Object>>(), null);
		tested.runPreprocessors("mytype", null, new HashMap<String, Object>());

		// case - exception on bad preprocessor configuration element
		{
			List preprocessorsDef = new ArrayList<String>();
			preprocessorsDef.add("aa");
			try {
				tested.runPreprocessors("mytype", preprocessorsDef, new HashMap<String, Object>());
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}

		// case - exception on bad preprocessor configuration structure
		{
			List<Map<String, Object>> preprocessorsDef = new ArrayList<Map<String, Object>>();
			preprocessorsDef.add(new HashMap<String, Object>());
			try {
				tested.runPreprocessors("mytype", preprocessorsDef, new HashMap<String, Object>());
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}

		// case - preprocessors run OK
		{
			List<Map<String, Object>> preprocessorsDef = ProviderService.extractPreprocessors(
					(Map<String, Object>) ((Map<String, Object>) TestUtils.loadJSONFromClasspathFile("/provider/provider_1.json")
							.get("type")).get("provider1_mailing"), "provider1_mailing");
			Map<String, Object> data = new HashMap<String, Object>();
			tested.runPreprocessors("mytype", preprocessorsDef, data);
			Assert.assertEquals("value1", data.get("name1"));
			Assert.assertEquals("value2", data.get("name2"));
		}

		// case - preprocessors run OK when data is null
		{
			List<Map<String, Object>> preprocessorsDef = ProviderService.extractPreprocessors(
					(Map<String, Object>) ((Map<String, Object>) TestUtils.loadJSONFromClasspathFile("/provider/provider_1.json")
							.get("type")).get("provider1_mailing"), "provider1_mailing");
			Map<String, Object> data = null;
			tested.runPreprocessors("mytype", preprocessorsDef, data);
		}

	}

	@Test
	public void findContentType() throws IOException {
		ProviderService tested = getTestedWithEmbeddedClient();
		try {

			// case - missing index
			Assert.assertNull(tested.findContentType("unknown"));

			indexCreate(INDEX_NAME);
			// case - empty index
			Assert.assertNull(tested.findContentType("unknown"));

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_1",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_2",
					TestUtils.readStringFromClasspathFile("/provider/provider_2.json"));
			indexFlush(INDEX_NAME);

			// case - unknown type
			Assert.assertNull(tested.findContentType("unknown"));

			// case - found type
			{
				Map<String, Object> ret = tested.findContentType("provider1_mailing");
				Assert.assertNotNull(ret);
				Assert.assertEquals("mailing", ret.get(ProviderService.DCP_TYPE));
			}
			{
				Map<String, Object> ret = tested.findContentType("provider1_issue");
				Assert.assertNotNull(ret);
				Assert.assertEquals("issue", ret.get(ProviderService.DCP_TYPE));
			}
			{
				Map<String, Object> ret = tested.findContentType("provider2_mailing");
				Assert.assertNotNull(ret);
				Assert.assertEquals("mailing2", ret.get(ProviderService.DCP_TYPE));
			}

			// case - exception if type name is not unique
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_3",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexFlush(INDEX_NAME);
			try {
				tested.findContentType("provider1_mailing");
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findProvider() throws IOException {
		ProviderService tested = getTestedWithEmbeddedClient();
		try {

			// case - missing index
			Assert.assertNull(tested.findProvider("unknown"));

			indexCreate(INDEX_NAME);
			// case - empty index
			Assert.assertNull(tested.findProvider("provider1"));

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_1",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_2",
					TestUtils.readStringFromClasspathFile("/provider/provider_2.json"));
			indexFlush(INDEX_NAME);

			// case - unknown type
			Assert.assertNull(tested.findProvider("unknown"));

			// case - found type
			{
				Map<String, Object> ret = tested.findProvider("provider1");
				Assert.assertNotNull(ret);
				Assert.assertEquals("provider1", ret.get(ProviderService.NAME));
			}
			{
				Map<String, Object> ret = tested.findProvider("provider2");
				Assert.assertNotNull(ret);
				Assert.assertEquals("provider2", ret.get(ProviderService.NAME));
			}

			// case - exception if provider name is not unique
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_3",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexFlush(INDEX_NAME);
			try {
				tested.findProvider("provider1");
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void isSuperProvider() throws IOException {
		ProviderService tested = getTestedWithEmbeddedClient();
		try {

			// case - missing index
			Assert.assertFalse(tested.isSuperProvider("unknown"));

			indexCreate(INDEX_NAME);
			// case - empty index
			Assert.assertFalse(tested.isSuperProvider("provider1"));

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_1",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_2",
					TestUtils.readStringFromClasspathFile("/provider/provider_2.json"));
			indexFlush(INDEX_NAME);

			// case - unknown type
			Assert.assertFalse(tested.isSuperProvider("unknown"));

			// case - found type
			{
				Assert.assertFalse(tested.isSuperProvider("provider1"));
			}
			{
				Assert.assertTrue(tested.isSuperProvider("provider2"));
			}

			// case - exception if provider name is not unique
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_3",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexFlush(INDEX_NAME);
			try {
				tested.isSuperProvider("provider1");
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void authenticate() throws IOException {
		ProviderService tested = getTestedWithEmbeddedClient();
		try {

			// case - missing index
			Assert.assertFalse(tested.authenticate("unknown", "pwd"));

			indexCreate(INDEX_NAME);
			// case - empty index
			Assert.assertFalse(tested.authenticate("provider1", "pwd"));

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_1",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_2",
					TestUtils.readStringFromClasspathFile("/provider/provider_2.json"));
			indexFlush(INDEX_NAME);

			// case - unknown type
			Assert.assertFalse(tested.authenticate("unknown", "pwd"));

			// case - found type
			{
				Assert.assertFalse(tested.authenticate("provider1", "badpwd"));
			}
			{
				Assert.assertTrue(tested.authenticate("provider1", "pwd"));
			}

			// case - exception if provider name is not unique
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "provider_3",
					TestUtils.readStringFromClasspathFile("/provider/provider_1.json"));
			indexFlush(INDEX_NAME);
			try {
				tested.authenticate("provider1", "pwd");
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private ProviderService getTestedWithEmbeddedClient() {
		SearchClientService scs = new SearchClientService();
		scs.client = prepareESClientForUnitTest();
		ProviderService tested = new ProviderService();
		tested.searchClientService = scs;
		tested.securityService = new SecurityService();
		tested.entityService = new ElasticsearchEntityService(scs.client, INDEX_NAME, INDEX_TYPE, false);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

}
