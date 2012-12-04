/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import junit.framework.Assert;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * TODO Unit test for {@link ContentRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentRestServiceTest extends ESRealClientTestBase {

	private static final String INDEX_TYPE = "index_type";
	private static final String INDEX_NAME = "index_name";

	@SuppressWarnings("unchecked")
	@Test
	public void getContent() {
		ContentRestService tested = getTested(false);

		// case - invalid input parameters
		TestUtils.assertResponseStatus(tested.getContent(null, "1"), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getContent("", "1"), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getContent("testtype", null), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getContent("testtype", ""), Response.Status.BAD_REQUEST);

		// case - type is unknown
		TestUtils.assertResponseStatus(tested.getContent("unknown", "12"), Response.Status.BAD_REQUEST);

		// case - type configuration is invalid (do not contains index name and type)
		TestUtils.assertResponseStatus(tested.getContent("invalid", "12"), Response.Status.INTERNAL_SERVER_ERROR);

		try {
			tested = getTested(true);

			// case - type is valid, but nothing is found for passed id because index is missing
			indexDelete(INDEX_NAME);
			TestUtils.assertResponseStatus(tested.getContent("known", "12"), Response.Status.NOT_FOUND);

			// case - index is present bud document is not found
			indexCreate(INDEX_NAME);
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "1", "{\"name\":\"test\"}");
			TestUtils.assertResponseStatus(tested.getContent("known", "2"), Response.Status.NOT_FOUND);

			// case - document found
			Map<String, Object> ret = (Map<String, Object>) tested.getContent("known", "1");
			Assert.assertEquals("test", ret.get("name"));

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void checkSearchIndexSettings() {

		try {
			ContentRestService.checkSearchIndexSettings("type", null, "doctype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			ContentRestService.checkSearchIndexSettings("type", "  ", "doctype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			ContentRestService.checkSearchIndexSettings("type", "index", null);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			ContentRestService.checkSearchIndexSettings("type", "index", "   ");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		ContentRestService.checkSearchIndexSettings("type", "index", "doctype");
	}

	/**
	 * Prepare tested instance with injected mocks.
	 * 
	 * @param initEsClient - searchClientService is initialized from
	 *          {@link ESRealClientTestBase#prepareSearchClientServiceMock()} if this is true, so do not forget to clean
	 *          up client in finally!
	 * @return instance for test
	 */
	protected ContentRestService getTested(boolean initEsClient) {
		ContentRestService tested = new ContentRestService();
		if (initEsClient)
			tested.searchClientService = prepareSearchClientServiceMock();

		tested.providerService = Mockito.mock(ProviderService.class);
		Mockito.when(tested.providerService.findContentType("unknown")).thenReturn(null);
		Map<String, Object> typeDef = new HashMap<String, Object>();
		Map<String, Object> typeDefIndex = new HashMap<String, Object>();
		typeDef.put("index", typeDefIndex);
		typeDefIndex.put("name", INDEX_NAME);
		typeDefIndex.put("type", INDEX_TYPE);
		Mockito.when(tested.providerService.findContentType("known")).thenReturn(typeDef);
		tested.log = Logger.getLogger("testlogger");
		tested.securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(new Principal() {

			@Override
			public String getName() {
				return "jbossorg";
			}
		});
		return tested;
	}
}
