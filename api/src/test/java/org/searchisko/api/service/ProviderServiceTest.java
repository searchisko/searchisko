/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.cache.IndexNamesCache;
import org.searchisko.api.cache.ProviderCache;
import org.searchisko.api.service.ProviderService.PreprocessChainContextImpl;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;

/**
 * Unit test for {@link ProviderService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProviderServiceTest extends ESRealClientTestBase {

	@Test
	public void generateSysId() {
		ProviderService tested = new ProviderService();
		Assert.assertEquals("mytype-myid", tested.generateSysId("mytype", "myid"));
		Assert.assertEquals("mytype-null", tested.generateSysId("mytype", null));
		Assert.assertEquals("null-myid", tested.generateSysId(null, "myid"));
		Assert.assertEquals("null-null", tested.generateSysId(null, null));
	}

	@Test
	public void parseTypeNameFromSysId() {

		// cases negative
		parseTypeNameFromSysIdExceptionAsert(null);
		parseTypeNameFromSysIdExceptionAsert("");
		parseTypeNameFromSysIdExceptionAsert("-d");
		parseTypeNameFromSysIdExceptionAsert("-dsda");
		parseTypeNameFromSysIdExceptionAsert(" -dsda");
		parseTypeNameFromSysIdExceptionAsert("   -dsda");
		parseTypeNameFromSysIdExceptionAsert("d-");
		parseTypeNameFromSysIdExceptionAsert("d-  ");
		parseTypeNameFromSysIdExceptionAsert("dfsdf-");
		parseTypeNameFromSysIdExceptionAsert("asdasda");
		parseTypeNameFromSysIdExceptionAsert("adfafa_.*/");

		// cases positive
		ProviderService tested = new ProviderService();
		Assert.assertEquals("b", tested.parseTypeNameFromSysId("b-a"));
		Assert.assertEquals("jborg_issue", tested.parseTypeNameFromSysId("jborg_issue-a"));
		Assert.assertEquals("jborg_issue", tested.parseTypeNameFromSysId("jborg_issue-5"));
		Assert.assertEquals("jborg_issue", tested.parseTypeNameFromSysId("jborg_issue-25"));
		Assert.assertEquals("jborg_issue", tested.parseTypeNameFromSysId("jborg_issue-ORG-565"));
	}

	private void parseTypeNameFromSysIdExceptionAsert(String value) {

		try {
			ProviderService tested = new ProviderService();
			tested.parseTypeNameFromSysId(value);
			Assert.fail("IllegalArgumentException must be thrown");
		} catch (IllegalArgumentException e) {
			// OK
		}

	}

	@Test
	public void extractAllContentTypes() {
		Map<String, Object> providerDef = new HashMap<String, Object>();

		// case - type field not defined
		Assert.assertNull(ProviderService.extractAllContentTypes(providerDef));

		Map<String, Object> types = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, types);

		// case - type field defined
		Assert.assertEquals(types, ProviderService.extractAllContentTypes(providerDef));

		// case - bad class of type field
		providerDef.put(ProviderService.TYPE, "baaad");
		try {
			ProviderService.extractAllContentTypes(providerDef);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
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

		// case - search_all_excluded field defined but invalid data type
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
	public void extractPersist() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - persist field not defined
		Assert.assertFalse(ProviderService.extractPersist(typeDef));

		// case - persist field defined but empty
		typeDef.put(ProviderService.PERSIST, "");
		Assert.assertFalse(ProviderService.extractPersist(typeDef));

		// case - persist field defined but invalid data type
		typeDef.put(ProviderService.PERSIST, new Integer(10));
		Assert.assertFalse(ProviderService.extractPersist(typeDef));

		typeDef.put(ProviderService.PERSIST, new Boolean(true));
		Assert.assertTrue(ProviderService.extractPersist(typeDef));
		typeDef.put(ProviderService.PERSIST, new Boolean(false));
		Assert.assertFalse(ProviderService.extractPersist(typeDef));

		// case - persist correct found
		typeDef.put(ProviderService.PERSIST, "false");
		Assert.assertFalse(ProviderService.extractPersist(typeDef));
		typeDef.put(ProviderService.PERSIST, "False");
		Assert.assertFalse(ProviderService.extractPersist(typeDef));

		typeDef.put(ProviderService.PERSIST, "true");
		Assert.assertTrue(ProviderService.extractPersist(typeDef));
		typeDef.put(ProviderService.PERSIST, "True");
		Assert.assertTrue(ProviderService.extractPersist(typeDef));
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
	public void extractSysType() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - sys_type field not defined
		try {
			ProviderService.extractSysType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - sys_type field defined but empty
		typeDef.put(ProviderService.SYS_TYPE, "");
		try {
			ProviderService.extractSysType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - sys_type field defined but empty
		typeDef.put(ProviderService.SYS_TYPE, "  ");
		try {
			ProviderService.extractSysType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - index type found correct found
		typeDef.put(ProviderService.SYS_TYPE, "mysystype");
		Assert.assertEquals(typeDef.get(ProviderService.SYS_TYPE), ProviderService.extractSysType(typeDef, "mytype"));
	}

	@Test
	public void extractSysContentContentType() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - sys_type field not defined
		try {
			ProviderService.extractSysContentContentType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - sys_type field defined but empty
		typeDef.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "");
		try {
			ProviderService.extractSysContentContentType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - sys_type field defined but empty
		typeDef.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "  ");
		try {
			ProviderService.extractSysContentContentType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - index type found correct found
		typeDef.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "mysystype");
		Assert.assertEquals(typeDef.get(ProviderService.SYS_CONTENT_CONTENT_TYPE),
				ProviderService.extractSysContentContentType(typeDef, "mytype"));
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
			List<Map<String, String>> warnings = tested.runPreprocessors("mytype", preprocessorsDef, data);
			Assert.assertNull(warnings);
			Assert.assertEquals("value1", data.get("name1"));
			Assert.assertEquals("value2", data.get("name2"));
		}

		// case - preprocessors run OK when data is null, warnings returned
		{
			List<Map<String, Object>> preprocessorsDef = ProviderService.extractPreprocessors(
					(Map<String, Object>) ((Map<String, Object>) TestUtils.loadJSONFromClasspathFile("/provider/provider_1.json")
							.get("type")).get("provider1_mailing"), "provider1_mailing");
			Map<String, Object> data = null;
			List<Map<String, String>> warnings = tested.runPreprocessors("mytype", preprocessorsDef, data);
			Assert.assertNotNull(warnings);
			Assert.assertEquals(2, warnings.size());
			Assert.assertEquals("warning preprocessor", warnings.get(0).get(PreprocessChainContextImpl.WD_PREPROC_NAME));
			Assert.assertEquals("warning message because null data",
					warnings.get(0).get(PreprocessChainContextImpl.WD_WARNING));
			Assert.assertEquals("warning preprocessor 2", warnings.get(1).get(PreprocessChainContextImpl.WD_PREPROC_NAME));
			Assert.assertEquals("warning message because null data",
					warnings.get(1).get(PreprocessChainContextImpl.WD_WARNING));
		}

	}

	@Test
	public void findContentType() throws IOException {
		ProviderService tested = getTested();
		tested.providerCache = Mockito.mock(ProviderCache.class);

		List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();
		all.add(TestUtils.loadJSONFromClasspathFile("/provider/provider_1.json"));
		all.add(TestUtils.loadJSONFromClasspathFile("/provider/provider_2.json"));
		Mockito.when(tested.entityService.getAll()).thenReturn(all);

		// case - unknown type
		Assert.assertNull(tested.findContentType("unknown"));
		tested.flushCaches();

		// case - found type
		{
			Map<String, Object> ret = tested.findContentType("provider1_mailing");
			Assert.assertNotNull(ret);
			Assert.assertEquals("mailing", ret.get(ProviderService.SYS_TYPE));
		}
		{
			Map<String, Object> ret = tested.findContentType("provider1_issue");
			Assert.assertNotNull(ret);
			Assert.assertEquals("issue", ret.get(ProviderService.SYS_TYPE));
		}
		{
			Map<String, Object> ret = tested.findContentType("provider2_mailing");
			Assert.assertNotNull(ret);
			Assert.assertEquals("mailing2", ret.get(ProviderService.SYS_TYPE));
		}
	}

	@Test
	public void flushCaches() {
		ProviderService tested = getTested();
		tested.cacheAllProvidersTTL = 200000;

		// case - tested.indexNamesCache is null
		List<Map<String, Object>> allList = new ArrayList<Map<String, Object>>();
		Mockito.when(tested.entityService.getAll()).thenReturn(allList);
		Assert.assertEquals(allList, tested.getAll());
		tested.flushCaches();
		Assert.assertEquals(allList, tested.getAll());
		Mockito.verify(tested.entityService, Mockito.times(2)).getAll();

		// case - tested.indexNamesCache is not null so mut be flushed too
		tested.flushCaches();
		Mockito.reset(tested.entityService);
		tested.indexNamesCache = Mockito.mock(IndexNamesCache.class);
		tested.providerCache = Mockito.mock(ProviderCache.class);
		Mockito.when(tested.entityService.getAll()).thenReturn(allList);
		Assert.assertEquals(allList, tested.getAll());
		tested.flushCaches();
		Assert.assertEquals(allList, tested.getAll());
		Mockito.verify(tested.entityService, Mockito.times(2)).getAll();
		Mockito.verify(tested.indexNamesCache).flush();
	}

	@Test
	public void getAll() throws InterruptedException {
		ProviderService tested = getTested();

		// case - return value is propagated, cache works
		tested.cacheAllProvidersTTL = 400L;
		List<Map<String, Object>> allList = new ArrayList<Map<String, Object>>();
		Mockito.when(tested.entityService.getAll()).thenReturn(allList);
		Assert.assertEquals(allList, tested.getAll());
		Assert.assertEquals(allList, tested.getAll());
		Assert.assertEquals(allList, tested.getAll());
		Mockito.verify(tested.entityService, Mockito.times(1)).getAll();
		// cache timeout
		Thread.sleep(500);
		tested.cacheAllProvidersTTL = 50000L;
		Mockito.reset(tested.entityService);
		List<Map<String, Object>> allList2 = new ArrayList<Map<String, Object>>();
		Mockito.when(tested.entityService.getAll()).thenReturn(allList2);
		Assert.assertEquals(allList2, tested.getAll());
		Assert.assertEquals(allList2, tested.getAll());
		Mockito.verify(tested.entityService, Mockito.times(1)).getAll();

	}

	@Test
	public void findProvider() throws IOException {
		ProviderService tested = getTested();
		tested.providerCache = Mockito.mock(ProviderCache.class);

		// case - unknown
		Mockito.when(tested.entityService.get("unknown")).thenReturn(null);
		Mockito.when(tested.providerCache.get("unknown")).thenReturn(null);
		Assert.assertNull(tested.findProvider("unknown"));
		Mockito.verify(tested.entityService).get("unknown");
		Mockito.verifyNoMoreInteractions(tested.entityService);
		Mockito.verify(tested.providerCache).get("unknown");
		Mockito.verifyNoMoreInteractions(tested.providerCache);

		// case - existing but not in cache
		Mockito.reset(tested.entityService, tested.providerCache);
		Map<String, Object> providerLoaded = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("aa")).thenReturn(providerLoaded);
		Mockito.when(tested.providerCache.get("aa")).thenReturn(null);
		Assert.assertEquals(providerLoaded, tested.findProvider("aa"));
		Mockito.verify(tested.entityService).get("aa");
		Mockito.verifyNoMoreInteractions(tested.entityService);
		Mockito.verify(tested.providerCache).get("aa");
		Mockito.verify(tested.providerCache).put("aa", providerLoaded);
		Mockito.verifyNoMoreInteractions(tested.providerCache);

		// case - existing in cache
		Mockito.reset(tested.entityService, tested.providerCache);
		Map<String, Object> providerCached = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("aa")).thenReturn(providerLoaded);
		Mockito.when(tested.providerCache.get("aa")).thenReturn(providerCached);
		Assert.assertEquals(providerCached, tested.findProvider("aa"));
		Mockito.verifyNoMoreInteractions(tested.entityService);
		Mockito.verify(tested.providerCache).get("aa");
		Mockito.verifyNoMoreInteractions(tested.providerCache);

	}

	@Test
	public void isSuperProvider() throws IOException {
		ProviderService tested = getTested();
		tested.providerCache = Mockito.mock(ProviderCache.class);

		Mockito.when(tested.providerCache.get(Mockito.anyString())).thenReturn(null);
		Mockito.when(tested.entityService.get("provider1")).thenReturn(
				TestUtils.loadJSONFromClasspathFile("/provider/provider_1.json"));
		Mockito.when(tested.entityService.get("provider2")).thenReturn(
				TestUtils.loadJSONFromClasspathFile("/provider/provider_2.json"));

		// case - unknown provider
		Assert.assertFalse(tested.isSuperProvider("unknown"));

		// case - found provider
		{
			Assert.assertFalse(tested.isSuperProvider("provider1"));
		}
		{
			Assert.assertTrue(tested.isSuperProvider("provider2"));
		}

	}

	@Test
	public void authenticate() throws IOException {
		ProviderService tested = getTested();
		tested.providerCache = Mockito.mock(ProviderCache.class);

		Mockito.when(tested.providerCache.get(Mockito.anyString())).thenReturn(null);
		Mockito.when(tested.entityService.get("provider1")).thenReturn(
				TestUtils.loadJSONFromClasspathFile("/provider/provider_1.json"));
		Mockito.when(tested.entityService.get("provider2")).thenReturn(
				TestUtils.loadJSONFromClasspathFile("/provider/provider_2.json"));

		// case - unknown type
		Assert.assertFalse(tested.authenticate("unknown", "pwd"));

		// case - found type
		{
			Assert.assertFalse(tested.authenticate("provider1", "badpwd"));
		}
		{
			Assert.assertTrue(tested.authenticate("provider1", "pwd"));
		}
	}

	@Test
	public void get() {
		ProviderService tested = getTested();

		Map<String, Object> expected = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("aaa")).thenReturn(expected);
		Assert.assertEquals(expected, tested.get("aaa"));
		Mockito.verify(tested.entityService).get("aaa");
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void getAll_paged() {
		ProviderService tested = getTested();

		String[] str = new String[] {};
		StreamingOutput ret = Mockito.mock(StreamingOutput.class);
		Mockito.when(tested.entityService.getAll(10, 20, str)).thenReturn(ret);
		Assert.assertEquals(ret, tested.getAll(10, 20, str));
		Mockito.verify(tested.entityService).getAll(10, 20, str);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void create() {
		ProviderService tested = getTested();

		tested.cacheAllProvidersValidTo = 1000;
		Map<String, Object> value = new HashMap<String, Object>();
		tested.create("aaa", value);
		// test cache was flushed!
		Assert.assertEquals(0, tested.cacheAllProvidersValidTo);
		Mockito.verify(tested.entityService).create("aaa", value);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void create_noid() {
		ProviderService tested = getTested();

		tested.cacheAllProvidersValidTo = 1000;
		String id = "aaa";
		Map<String, Object> value = new HashMap<String, Object>();
		Mockito.when(tested.entityService.create(value)).thenReturn(id);
		Assert.assertEquals(id, tested.create(value));
		// test cache was flushed!
		Assert.assertEquals(0, tested.cacheAllProvidersValidTo);
		Mockito.verify(tested.entityService).create(value);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void update() {
		ProviderService tested = getTested();

		tested.cacheAllProvidersValidTo = 1000;
		Map<String, Object> value = new HashMap<String, Object>();
		tested.update("aaa", value);
		// test cache was flushed!
		Assert.assertEquals(0, tested.cacheAllProvidersValidTo);
		Mockito.verify(tested.entityService).update("aaa", value);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void delete() {
		ProviderService tested = getTested();

		tested.cacheAllProvidersValidTo = 1000;
		tested.delete("aaa");
		// test cache was flushed!
		Assert.assertEquals(0, tested.cacheAllProvidersValidTo);
		Mockito.verify(tested.entityService).delete("aaa");
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	private ProviderService getTested() {
		ProviderService tested = new ProviderService();
		tested.securityService = new SecurityService();
		tested.entityService = Mockito.mock(EntityService.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

}
