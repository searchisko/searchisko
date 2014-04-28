/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.ObjectNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.indexer.EsRiverJiraIndexerHandler;
import org.searchisko.api.indexer.EsRiverRemoteIndexerHandler;
import org.searchisko.api.indexer.IndexerHandler;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;

import static org.mockito.Mockito.when;

/**
 * Unit test for {@link IndexerRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IndexerRestServiceTest {

	@Test
	public void getIndexerHandler() throws ObjectNotFoundException {
		IndexerRestService tested = getTested();

		try {
			tested.getIndexerHandler(null, "ct");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK;
		}

		try {
			tested.getIndexerHandler("", "ct");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK;
		}

		try {
			tested.getIndexerHandler("unknown", "ct");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK;
		}

		Assert.assertEquals(tested.esRiverJiraIndexerHandler,
				tested.getIndexerHandler(IndexerRestService.INDEXER_TYPE_ES_RIVER_JIRA, "ct"));

		Assert.assertEquals(tested.esRiverRemoteIndexerHandler,
				tested.getIndexerHandler(IndexerRestService.INDEXER_TYPE_ES_RIVER_REMOTE, "ct"));
	}

	@Test
	public void getIndexerConfiguration() throws ObjectNotFoundException {
		IndexerRestService tested = getTested();

		try {
			tested.getIndexerConfigurationWithManagePermissionCheck(null);
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}

		// case - unknown provider
		try {
			when(tested.providerService.findProvider("jbossorg")).thenReturn(null);
			tested.getIndexerConfigurationWithManagePermissionCheck("unknown_type");
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

		Mockito.reset(tested.providerService);
		Map<String, Object> providerInfo = new HashMap<String, Object>();
		providerInfo.put(ProviderService.NAME, "jbossorg");
		Map<String, Object> typesMap = new HashMap<String, Object>();
		providerInfo.put(ProviderService.TYPE, typesMap);

		Map<String, Object> tni = new HashMap<String, Object>();
		typesMap.put("type_no_indexer", tni);
		Map<String, Object> twi = new HashMap<String, Object>();
		Map<String, Object> indexerInfo = new HashMap<String, Object>();
		twi.put(ProviderService.INDEXER, indexerInfo);
		typesMap.put("type_with_indexer", twi);

		when(tested.providerService.findProvider("jbossorg")).thenReturn(providerInfo);

		// case - known provider but unknown type
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck("unknown_type");
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

		// case - known type without indexer
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck("type_no_indexer");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
		}

		// case - indexer configured
		Assert.assertEquals(indexerInfo, tested.getIndexerConfigurationWithManagePermissionCheck("type_with_indexer"));
	}

	@Test
	public void extractIndexerName() throws ObjectNotFoundException {
		IndexerRestService tested = getTested();

		Map<String, Object> ic = new HashMap<>();

		// case - name not configured
		try {
			tested.extractIndexerName(ic, "type");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
		}

		ic.put(ProviderService.NAME, "myName");
		Assert.assertEquals("myName", tested.extractIndexerName(ic, "type"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void getStatus() throws ObjectNotFoundException {
		IndexerRestService tested = Mockito.mock(IndexerRestService.class);
		tested.log = Logger.getLogger("testlogger");

		// case - ObjectNotFoundException from handler
		try {
			Mockito.when(tested.getStatus(Mockito.anyString())).thenCallRealMethod();
			Mockito.when(tested.extractIndexerName(Mockito.anyMap(), Mockito.anyString())).thenCallRealMethod();
			Map<String, Object> ic = new HashMap<>();
			ic.put(ProviderService.TYPE, "indexer_type");
			ic.put(ProviderService.NAME, "indexer_name");
			Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);

			IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
			Mockito.when(ihMock.getStatus("indexer_name")).thenThrow(new ObjectNotFoundException());
			Mockito.when(tested.getIndexerHandler("indexer_type", "my_type")).thenReturn(ihMock);

			tested.getStatus("my_type");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
		}

		// case - ObjectNotFoundException from handler
		Mockito.reset(tested);
		Mockito.when(tested.getStatus(Mockito.anyString())).thenCallRealMethod();
		Mockito.when(tested.extractIndexerName(Mockito.anyMap(), Mockito.anyString())).thenCallRealMethod();
		Map<String, Object> ic = new HashMap<>();
		ic.put(ProviderService.TYPE, "indexer_type");
		ic.put(ProviderService.NAME, "indexer_name");
		Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);
		IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
		Mockito.when(ihMock.getStatus("indexer_name")).thenReturn("sys info");
		Mockito.when(tested.getIndexerHandler("indexer_type", "my_type")).thenReturn(ihMock);

		Assert.assertEquals("sys info", tested.getStatus("my_type"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void forceReindex() throws ObjectNotFoundException {
		IndexerRestService tested = Mockito.mock(IndexerRestService.class);
		tested.log = Logger.getLogger("testlogger");

		// case - ObjectNotFoundException from handler
		try {
			Mockito.when(tested.forceReindex(Mockito.anyString())).thenCallRealMethod();
			Mockito.when(tested.extractIndexerName(Mockito.anyMap(), Mockito.anyString())).thenCallRealMethod();
			Map<String, Object> ic = new HashMap<>();
			ic.put(ProviderService.TYPE, "indexer_type");
			ic.put(ProviderService.NAME, "indexer_name");
			Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);

			IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
			Mockito.doThrow(new ObjectNotFoundException()).when(ihMock).forceReindex("indexer_name");
			Mockito.when(tested.getIndexerHandler("indexer_type", "my_type")).thenReturn(ihMock);

			tested.forceReindex("my_type");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
		}

		// case - success call
		Mockito.reset(tested);
		Mockito.when(tested.forceReindex(Mockito.anyString())).thenCallRealMethod();
		Mockito.when(tested.extractIndexerName(Mockito.anyMap(), Mockito.anyString())).thenCallRealMethod();
		Map<String, Object> ic = new HashMap<>();
		ic.put(ProviderService.TYPE, "indexer_type");
		ic.put(ProviderService.NAME, "indexer_name");
		Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);
		IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
		Mockito.when(tested.getIndexerHandler("indexer_type", "my_type")).thenReturn(ihMock);

		Response r = tested.forceReindex("my_type");
		Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
		Mockito.verify(ihMock).forceReindex("indexer_name");

	}

	private IndexerRestService getTested() {
		IndexerRestService tested = new IndexerRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.esRiverJiraIndexerHandler = Mockito.mock(EsRiverJiraIndexerHandler.class);
		tested.esRiverRemoteIndexerHandler = Mockito.mock(EsRiverRemoteIndexerHandler.class);

		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		when(tested.authenticationUtilService.getAuthenticatedProvider(null)).thenReturn("jbossorg");

		tested.providerService = Mockito.mock(ProviderService.class);
		return tested;
	}

}
