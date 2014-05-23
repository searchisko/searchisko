/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.ejb.ObjectNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.indexer.EsRiverJiraIndexerHandler;
import org.searchisko.api.indexer.EsRiverRemoteIndexerHandler;
import org.searchisko.api.indexer.IndexerHandler;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.testtools.TestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link IndexerRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IndexerRestServiceTest {

	private static final String TYPE_KNOWN_WITH_INDEXER = "known-with-indexer";
	private static final String TYPE_KNOWN = "known";
	private static final String TYPE_UNKNOWN = "unknown";
	private static final String TEST_PROVIDER_NAME_UNAUTH = "other_provider";

	@Test
	public void getIndexerConfigurationWithManagePermissionCheck() throws ObjectNotFoundException {
		IndexerRestService tested = getTested();

		// case - field param required validation
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck(null);
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck("");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck("   ");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}

		// case - unknown type
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_UNKNOWN);
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

		// known type but without indexer defined, provider has permission
		try {
			Mockito.reset(tested.authenticationUtilService);
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_KNOWN);
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
			verify(tested.authenticationUtilService).checkProviderManagementPermission(
					ProviderServiceTest.TEST_PROVIDER_NAME);
		}

		// known type with indexer defined, provider has permission
		{
			Mockito.reset(tested.authenticationUtilService);
			Map<String, Object> ret = tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_KNOWN_WITH_INDEXER);
			Assert.assertNotNull(ret);
			verify(tested.authenticationUtilService).checkProviderManagementPermission(
					ProviderServiceTest.TEST_PROVIDER_NAME);
		}

		// known type, provider has no permission
		try {
			Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
					.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_KNOWN_WITH_INDEXER);
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

		// known type, provider not authenticated
		try {
			Mockito.doThrow(new NotAuthenticatedException(AuthenticatedUserType.PROVIDER))
					.when(tested.authenticationUtilService)
					.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_KNOWN_WITH_INDEXER);
			Assert.fail("NotAuthenticatedException expected");
		} catch (NotAuthenticatedException e) {
			// OK
		}

	}

	@Test
	public void getAllIndexerConfigurationsWithManagePermissionCheck() {
		IndexerRestService tested = getTested();

		// case - no one indexer available
		{
			List<ProviderContentTypeInfo> ret = tested.getAllIndexerConfigurationsWithManagePermissionCheck();
			Assert.assertNotNull(ret);
			Assert.assertEquals(0, ret.size());
		}

		// case - some indexers available, permission filtering etc.
		{

			List<Map<String, Object>> allProviders = new ArrayList<>();
			allProviders.add(TestUtils.loadJSONFromClasspathFile("/indexer/provider_1.json"));
			allProviders.add(TestUtils.loadJSONFromClasspathFile("/indexer/provider_2.json"));

			Mockito.when(tested.providerService.getAll()).thenReturn(allProviders);

			Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
					.checkProviderManagementPermission(TEST_PROVIDER_NAME_UNAUTH);

			List<ProviderContentTypeInfo> ret = tested.getAllIndexerConfigurationsWithManagePermissionCheck();
			Assert.assertNotNull(ret);
			Assert.assertEquals(2, ret.size());
			Assert.assertEquals(ProviderServiceTest.TEST_PROVIDER_NAME, ret.get(0).getProviderName());
			Assert.assertEquals("jbossorg_type_1", ret.get(0).getTypeName());
			Assert.assertEquals(ProviderServiceTest.TEST_PROVIDER_NAME, ret.get(1).getProviderName());
			Assert.assertEquals("jbossorg_type_3", ret.get(1).getTypeName());

			verify(tested.authenticationUtilService).checkProviderManagementPermission(
					ProviderServiceTest.TEST_PROVIDER_NAME);
			verify(tested.authenticationUtilService).checkProviderManagementPermission(
					TEST_PROVIDER_NAME_UNAUTH);
		}

	}

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
			tested.getIndexerHandler("unknown_indexer_type", "ct");
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
			when(tested.providerService.findProvider(ProviderServiceTest.TEST_PROVIDER_NAME)).thenReturn(null);
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_UNKNOWN);
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

		// case - known provider but unknown type
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_UNKNOWN);
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

		// case - known type without indexer
		try {
			tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_KNOWN);
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
		}

		// case - indexer configured
		Assert.assertEquals(INDEXER_NAME, tested.getIndexerConfigurationWithManagePermissionCheck(TYPE_KNOWN_WITH_INDEXER)
				.get(ProviderService.NAME));
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

	@Test
	public void getStatus() throws ObjectNotFoundException {
		IndexerRestService tested = Mockito.mock(IndexerRestService.class);
		tested.log = Logger.getLogger("testlogger");

		Map<String, Object> ic = new HashMap<>();
		ic.put(ProviderService.TYPE, INDEXER_TYPE);
		ic.put(ProviderService.NAME, INDEXER_NAME);

		// case - ObjectNotFoundException from handler
		try {
			Mockito.when(tested.getStatus(Mockito.anyString())).thenCallRealMethod();
			Mockito.when(tested.extractIndexerName(Mockito.anyMapOf(String.class, Object.class), Mockito.anyString())).thenCallRealMethod();
			Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);

			IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
			Mockito.when(ihMock.getStatus(INDEXER_NAME)).thenThrow(new ObjectNotFoundException());
			Mockito.when(tested.getIndexerHandler(INDEXER_TYPE, "my_type")).thenReturn(ihMock);

			tested.getStatus("my_type");
			Assert.fail("ObjectNotFoundException expected");
		} catch (ObjectNotFoundException e) {
			// OK
		}

		// case - status returned
		Mockito.reset(tested);
		Mockito.when(tested.getStatus(Mockito.anyString())).thenCallRealMethod();
		Mockito.when(tested.extractIndexerName(Mockito.anyMapOf(String.class, Object.class), Mockito.anyString())).thenCallRealMethod();
		Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);
		IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
		Mockito.when(ihMock.getStatus(INDEXER_NAME)).thenReturn("sys info");
		Mockito.when(tested.getIndexerHandler(INDEXER_TYPE, "my_type")).thenReturn(ihMock);

		Assert.assertEquals("sys info", tested.getStatus("my_type"));

		// case - no permission
		try {
			Mockito.reset(tested);
			Mockito.when(tested.getStatus(Mockito.anyString())).thenCallRealMethod();
			Mockito.when(tested.extractIndexerName(Mockito.anyMapOf(String.class, Object.class), Mockito.anyString())).thenCallRealMethod();
			Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenThrow(
					new NotAuthorizedException("no perm"));
			tested.getStatus("my_type");
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

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
			ic.put(ProviderService.TYPE, INDEXER_TYPE);
			ic.put(ProviderService.NAME, INDEXER_NAME);
			Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);

			IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
			Mockito.doThrow(new ObjectNotFoundException()).when(ihMock).forceReindex(INDEXER_NAME);
			Mockito.when(tested.getIndexerHandler(INDEXER_TYPE, "my_type")).thenReturn(ihMock);

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
		ic.put(ProviderService.TYPE, INDEXER_TYPE);
		ic.put(ProviderService.NAME, INDEXER_NAME);
		Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenReturn(ic);
		IndexerHandler ihMock = Mockito.mock(IndexerHandler.class);
		Mockito.when(tested.getIndexerHandler(INDEXER_TYPE, "my_type")).thenReturn(ihMock);

		Response r = tested.forceReindex("my_type");
		Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
		Mockito.verify(ihMock).forceReindex(INDEXER_NAME);

		// case - no permission
		Mockito.reset(tested);
		Mockito.when(tested.forceReindex(Mockito.anyString())).thenCallRealMethod();
		Mockito.when(tested.extractIndexerName(Mockito.anyMap(), Mockito.anyString())).thenCallRealMethod();
		Mockito.when(tested.getIndexerConfigurationWithManagePermissionCheck("my_type")).thenThrow(
				new NotAuthorizedException("no perm"));
		try {
			tested.forceReindex("my_type");
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

	}

	private IndexerRestService getTested() {
		IndexerRestService tested = new IndexerRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.esRiverJiraIndexerHandler = Mockito.mock(EsRiverJiraIndexerHandler.class);
		tested.esRiverRemoteIndexerHandler = Mockito.mock(EsRiverRemoteIndexerHandler.class);

		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		when(tested.authenticationUtilService.getAuthenticatedProvider()).thenReturn(
				ProviderServiceTest.TEST_PROVIDER_NAME);

		tested.providerService = Mockito.mock(ProviderService.class);
		setupProviderServiceMock(tested.providerService);
		return tested;
	}

	private static final String INDEXER_TYPE = "index_type";
	private static final String INDEXER_NAME = "index_name";

	public static void setupProviderServiceMock(ProviderService providerServiceMock) {
		Mockito.when(providerServiceMock.findContentType(TYPE_UNKNOWN)).thenReturn(null);
		Mockito.when(providerServiceMock.generateSysId(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		Map<String, Object> typeDefKnown = new HashMap<String, Object>();
		Mockito.when(providerServiceMock.findContentType(TYPE_KNOWN)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefKnown, TYPE_KNOWN));

		Map<String, Object> typeDefKnownWithIndexer = new HashMap<String, Object>();
		Map<String, Object> typeDefKnownIndexer = new HashMap<String, Object>();
		typeDefKnownWithIndexer.put(ProviderService.INDEXER, typeDefKnownIndexer);
		typeDefKnownIndexer.put("name", INDEXER_NAME);
		typeDefKnownIndexer.put("type", INDEXER_TYPE);
		Mockito.when(providerServiceMock.findContentType(TYPE_KNOWN_WITH_INDEXER)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefKnownWithIndexer, TYPE_KNOWN_WITH_INDEXER));

		Map<String, Object> providerDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.NAME, ProviderServiceTest.TEST_PROVIDER_NAME);
		when(providerServiceMock.findProvider(ProviderServiceTest.TEST_PROVIDER_NAME)).thenReturn(providerDef);
		Map<String, Object> typesDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, typesDef);
		typesDef.put(TYPE_KNOWN, typeDefKnown);
		typesDef.put(TYPE_KNOWN_WITH_INDEXER, typeDefKnownWithIndexer);
	}

}
