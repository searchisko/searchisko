/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.ConfigService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.api.testtools.WarningMockPreprocessor;

import javax.ejb.ObjectNotFoundException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for {@link NormalizationRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class NormalizationRestServiceTest {

	private static final String TEST_NORM_NAME = "test_normalization";

	@Test
	public void availableNormalizations() {
		NormalizationRestService tested = new NormalizationRestService();
		tested.configService = Mockito.mock(ConfigService.class);

		// case - config file not found
		{
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(null);
			Map<String, Object> ret = tested.availableNormalizations();
			Assert.assertEquals(0, ret.size());
		}

		// case - ok
		{
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(
					TestUtils.loadJSONFromClasspathFile("/normalization/normalizations.json"));
			Map<String, Object> ret = tested.availableNormalizations();
			Assert.assertEquals(2, ret.size());
			Assert.assertEquals("description 1", ret.get("test_normalization"));
			Assert.assertEquals("", ret.get("test_normalization_bad"));

		}

	}

	@Test
	public void normalizeOne() throws Exception {
		NormalizationRestService tested = Mockito.mock(NormalizationRestService.class);
		Mockito.when(tested.normalizeOne(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		// case - input validations for normalization name
		try {
			tested.normalizeOne(null, "id");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
		}
		try {
			tested.normalizeOne(" ", "id");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
		}

		// case - input validations for id
		try {
			tested.normalizeOne(TEST_NORM_NAME, null);
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
		}
		try {
			tested.normalizeOne(TEST_NORM_NAME, " ");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
		}

		// case - everything OK
		List<StructuredContentPreprocessor> pl = new ArrayList<>();
		Mockito.when(tested.getPreprocessors(TEST_NORM_NAME)).thenReturn(pl);
		Map<String, Object> ret = new HashMap<>();
		Mockito.when(tested.runPreprocessors(pl, "myid")).thenReturn(ret);

		Assert.assertEquals(ret, tested.normalizeOne(TEST_NORM_NAME, "myid"));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void normalizeBulk() throws Exception {
		NormalizationRestService tested = Mockito.mock(NormalizationRestService.class);
		Mockito.when(tested.normalizeBulk(Mockito.anyString(), Mockito.any(UriInfo.class))).thenCallRealMethod();
		List<StructuredContentPreprocessor> pl = new ArrayList<>();
		Mockito.when(tested.getPreprocessors(TEST_NORM_NAME)).thenReturn(pl);

		UriInfo reqContent = new UriInfoImpl(new URI("http://localhost"), new URI("http://localhost"), "", "id=myid",
				new ArrayList());
		// case - input validations for normalization name
		try {
			tested.normalizeBulk(null, reqContent);
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
		}
		try {
			tested.normalizeBulk(" ", reqContent);
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
		}

		// case - input validations for id
		TestUtils.assertResponseStatus(tested.normalizeBulk(TEST_NORM_NAME, null), Status.BAD_REQUEST);

		reqContent = new UriInfoImpl(new URI("http://localhost"), new URI("http://localhost"), "", "", new ArrayList());
		TestUtils.assertResponseStatus(tested.normalizeBulk(TEST_NORM_NAME, reqContent), Status.BAD_REQUEST);

		reqContent = new UriInfoImpl(new URI("http://localhost"), new URI("http://localhost"), "", "param=aa",
				new ArrayList());
		TestUtils.assertResponseStatus(tested.normalizeBulk(TEST_NORM_NAME, reqContent), Status.BAD_REQUEST);

		// case - id processing
		Map<String, Object> sret = new HashMap<>();
		Mockito.when(tested.runPreprocessors(pl, "myid")).thenReturn(sret);
		Map<String, Object> sret2 = new HashMap<>();
		Mockito.when(tested.runPreprocessors(pl, "myid2")).thenReturn(sret2);

		reqContent = new UriInfoImpl(new URI("http://localhost"), new URI("http://localhost"), "", "id=myid&id=myid2",
				new ArrayList());
		Object ret = tested.normalizeBulk(TEST_NORM_NAME, reqContent);
		Assert.assertTrue(ret instanceof Map);
		Map r = (Map) ret;
		Assert.assertEquals(2, r.size());
		Assert.assertEquals(sret, r.get("myid"));
		Assert.assertEquals(sret2, r.get("myid2"));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void getPreprocessors() throws SettingsException, ObjectNotFoundException {
		NormalizationRestService tested = new NormalizationRestService();
		tested.configService = Mockito.mock(ConfigService.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Client clientMock = Mockito.mock(Client.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(clientMock);

		// case - normalizations config file not found
		{
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(null);
			try {
				tested.getPreprocessors(TEST_NORM_NAME);
				Assert.fail("ObjectNotFoundException expected");
			} catch (ObjectNotFoundException e) {
				// OK
			}
		}

		// case - normalizations config file is empty
		{
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(
					new HashMap<String, Object>());
			try {
				tested.getPreprocessors(TEST_NORM_NAME);
				Assert.fail("ObjectNotFoundException expected");
			} catch (ObjectNotFoundException e) {
				// OK
			}
		}

		// case - normalizations of given name not found
		{
			Mockito.reset(tested.configService);
			Map<String, Object> nc = new HashMap<String, Object>();
			nc.put("otherNorm", new HashMap());
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(nc);
			try {
				tested.getPreprocessors(TEST_NORM_NAME);
				Assert.fail("ObjectNotFoundException expected");
			} catch (ObjectNotFoundException e) {
				// OK
			}
		}

		// case - normalization of given name is not configured properly
		{
			Mockito.reset(tested.configService);
			Map<String, Object> nc = new HashMap<String, Object>();
			nc.put(TEST_NORM_NAME, new Object());
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(nc);
			try {
				tested.getPreprocessors(TEST_NORM_NAME);
				Assert.fail("ObjectNotFoundException expected");
			} catch (ObjectNotFoundException e) {
				// OK
			}
		}

		// case - normalization of given name is configured properly but no preprocessors defined
		{
			Mockito.reset(tested.configService);
			Map<String, Object> nc = new HashMap<String, Object>();
			nc.put(TEST_NORM_NAME, new HashMap<String, Object>());
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(nc);
			List l = tested.getPreprocessors(TEST_NORM_NAME);
			Assert.assertTrue(l.isEmpty());
		}

		// case - normalization of given name is configured properly and preprocessors defined
		{
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(
					TestUtils.loadJSONFromClasspathFile("/normalization/normalizations.json"));
			List<StructuredContentPreprocessor> l = tested.getPreprocessors(TEST_NORM_NAME);
			Assert.assertEquals(2, l.size());
			Assert.assertEquals("username to Contributor code mapper", l.get(0).getName());
			Assert.assertEquals("Profile by Contributor code loader", l.get(1).getName());
			Assert.assertEquals(clientMock, ((WarningMockPreprocessor) l.get(0)).getClient());
			Assert.assertEquals(clientMock, ((WarningMockPreprocessor) l.get(1)).getClient());
		}

		// case - normalization of given name is configured properly but preprocessors definition is wrong (unknown class)
		try {
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_NORMALIZATIONS)).thenReturn(
					TestUtils.loadJSONFromClasspathFile("/normalization/normalizations.json"));
			tested.getPreprocessors("test_normalization_bad");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void runPreprocessors() {
		NormalizationRestService tested = new NormalizationRestService();

		// case - null preprocessors
		{
			Map<String, Object> ret = tested.runPreprocessors(null, "id1");
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertEquals("id1", ret.get(NormalizationRestService.OUTKEY_INPUT_ID));
		}

		// case - empty preprocessors
		{
			List<StructuredContentPreprocessor> pl = new ArrayList<>();
			Map<String, Object> ret = tested.runPreprocessors(pl, "id1");
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertEquals("id1", ret.get(NormalizationRestService.OUTKEY_INPUT_ID));
		}

		// case - preprocessors called, warnings returned
		{
			List<StructuredContentPreprocessor> pl = new ArrayList<>();
			WarningMockPreprocessor wp = new WarningMockPreprocessor();
			wp.init("pp1", null, null);
			wp.warnAlways = true;
			pl.add(wp);

			WarningMockPreprocessor wp2 = new WarningMockPreprocessor();
			wp2.init("pp2", null, null);
			wp2.addValue = true;
			pl.add(wp2);

			Map<String, Object> ret = tested.runPreprocessors(pl, "id1");
			Assert.assertNotNull(ret);
			Assert.assertEquals(3, ret.size());
			Assert.assertEquals("id1", ret.get(NormalizationRestService.OUTKEY_INPUT_ID));
			Assert.assertEquals(1, ((List) ret.get(NormalizationRestService.OUTKEY_WARNINGS)).size());
			Assert.assertEquals("value", ret.get("key"));
		}
	}

	@Test
	public void extractPreprocessors() {

		Map<String, Object> normalizationDef = null;

		try {
			NormalizationRestService.extractPreprocessors(normalizationDef, TEST_NORM_NAME);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		normalizationDef = new HashMap<>();
		Assert.assertNull(NormalizationRestService.extractPreprocessors(normalizationDef, TEST_NORM_NAME));

		normalizationDef.put(NormalizationRestService.CFG_PREPROCESSORS, new Object());
		try {
			NormalizationRestService.extractPreprocessors(normalizationDef, TEST_NORM_NAME);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		List<Map<String, Object>> pl = new ArrayList<>();
		normalizationDef.put(NormalizationRestService.CFG_PREPROCESSORS, pl);

		Assert.assertEquals(pl, NormalizationRestService.extractPreprocessors(normalizationDef, TEST_NORM_NAME));
	}

}
