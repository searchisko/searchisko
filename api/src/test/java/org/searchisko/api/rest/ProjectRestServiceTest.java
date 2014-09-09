/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.ProjectService;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;

/**
 * Unit test for {@link ProjectRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProjectRestServiceTest {

	@Test
	public void init() {
		ProjectRestService tested = new ProjectRestService();
		Assert.assertNull(tested.entityService);
		tested.projectService = Mockito.mock(ProjectService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(tested.projectService, tested.entityService);
	}

	@Test
	public void getAll_ok() {
		ProjectRestService tested = getTested();

		ESDataOnlyResponse res = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 12, tested.fieldsToRemove)).thenReturn(res);
		Assert.assertEquals(res, tested.getAll(10, 12));

	}

	@Test(expected = RuntimeException.class)
	public void getAll_errorFromService() {
		ProjectRestService tested = getTested();

		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, tested.fieldsToRemove)).thenThrow(
				new RuntimeException("my exception"));
		tested.getAll(10, 12);
	}

	@Test
	public void get_ok() {
		ProjectRestService tested = getTested();

		Map<String, Object> m = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("10")).thenReturn(m);
		Assert.assertEquals(m, tested.get("10"));

	}

	@Test(expected = RuntimeException.class)
	public void get_errorFromService() {
		ProjectRestService tested = getTested();

		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("my exception"));
		tested.get("10");
	}

	@Test(expected = RequiredFieldException.class)
	public void create_id_invalidParam() {
		ProjectRestService tested = getTested();

		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProjectService.FIELD_CODE, "myname");
			TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
		}
	}

	@Test
	public void create_id() {
		ProjectRestService tested = getTested();

		// case - invalid name field in input data
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
			m.put(ProjectService.FIELD_CODE, "");
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
		}

		// case - name field in data is not same as id parameter
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProjectService.FIELD_CODE, "myanothername");
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
		}

		// case - OK
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProjectService.FIELD_CODE, "myname");
			@SuppressWarnings("unchecked")
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("myname", m.get(ProjectService.FIELD_CODE));
			Mockito.verify(tested.entityService).create("myname", m);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

	}

	@Test(expected = RuntimeException.class)
	public void create_id_errorFromService() {
		ProjectRestService tested = getTested();

		Mockito.reset(tested.entityService);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProjectService.FIELD_CODE, "myname");
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("myname", m);
		tested.create("myname", m);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_noid() {
		ProjectRestService tested = getTested();

		// case - invalid name field in input data
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
			m.put(ProjectService.FIELD_CODE, "");
			TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
		}

		// case - OK
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProjectService.FIELD_CODE, "myname");
			Mockito.when(tested.entityService.get("myname")).thenReturn(null);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("myname", m.get(ProjectService.FIELD_CODE));
			Mockito.verify(tested.entityService).create("myname", m);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

	}

	@Test(expected = RuntimeException.class)
	public void create_noid_errorFromService() {
		ProjectRestService tested = getTested();
		Mockito.reset(tested.entityService);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProjectService.FIELD_CODE, "myname");
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("myname", m);
		tested.create(m);
	}

	@Test
	public void search_inputParamValidation() throws Exception {
		ProjectRestService tested = new ProjectRestService();
		RestEntityServiceBaseTest.mockLogger(tested);

		// only one param with some value is accepted
		TestUtils.assertResponseStatus(tested.search(null), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(TestUtils.prepareUriInfiWithParams()), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(TestUtils.prepareUriInfiWithParams("a", "b", "c", "d")),
				Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(TestUtils.prepareUriInfiWithParams("a", "  ")), Status.BAD_REQUEST);
	}

	@Test
	public void search_byOtherIdentifier() throws Exception {
		ProjectRestService tested = new ProjectRestService();
		tested.projectService = Mockito.mock(ProjectService.class);
		RestEntityServiceBaseTest.mockLogger(tested);

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.projectService.findByTypeSpecificCode("idType", "idValue")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams("idType", "idValue"));
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.projectService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.projectService.findByTypeSpecificCode("idType", "idValue")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams("idType", "idValue"));
			Mockito.verify(tested.projectService).findByTypeSpecificCode("idType", "idValue");
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.projectService);
			Mockito.when(tested.projectService.findByTypeSpecificCode("idType", "idValue")).thenThrow(
					new RuntimeException("test exception"));
			tested.search(TestUtils.prepareUriInfiWithParams("idType", "idValue"));
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	@Test
	public void search_byCode() throws Exception {
		ProjectRestService tested = new ProjectRestService();
		tested.projectService = Mockito.mock(ProjectService.class);
		RestEntityServiceBaseTest.mockLogger(tested);

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.projectService.findByCode("testcode")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams(
					ProjectRestService.PARAM_CODE, "testcode"));
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.projectService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.projectService.findByCode("testcode")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams(
					ProjectRestService.PARAM_CODE, "testcode"));
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.projectService);
			Mockito.when(tested.projectService.findByCode("testcode")).thenThrow(new RuntimeException("test exception"));
			tested.search(TestUtils.prepareUriInfiWithParams(ProjectRestService.PARAM_CODE, "testcode"));
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	protected ProjectRestService getTested() {
		ProjectRestService tested = new ProjectRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.setEntityService(Mockito.mock(EntityService.class));
		tested.securityContext = Mockito.mock(SecurityContext.class);
		return tested;
	}

}
