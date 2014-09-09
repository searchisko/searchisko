/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.ObjectNotFoundException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchResponse;
import org.jboss.resteasy.spi.BadRequestException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link ContributorRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorRestServiceTest {

	@Test
	public void search_inputParamValidation() throws Exception {
		ContributorRestService tested = getTested();

		// only one param with some value is accepted
		TestUtils.assertResponseStatus(tested.search(null), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(TestUtils.prepareUriInfiWithParams()), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(TestUtils.prepareUriInfiWithParams("a", "b", "c", "d")),
				Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(TestUtils.prepareUriInfiWithParams("a", "  ")), Status.BAD_REQUEST);
	}

	@Test
	public void search_byOtherIdentifier() throws Exception {
		ContributorRestService tested = getTested();

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.findByTypeSpecificCode("idType", "idValue")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams("idType", "idValue"));
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.findByTypeSpecificCode("idType", "idValue")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams("idType", "idValue"));
			Mockito.verify(tested.contributorService).findByTypeSpecificCode("idType", "idValue");
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.findByTypeSpecificCode("idType", "idValue")).thenThrow(
					new RuntimeException("test exception"));
			tested.search(TestUtils.prepareUriInfiWithParams("idType", "idValue"));
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	@Test
	public void search_byEmail() throws Exception {
		ContributorRestService tested = getTested();

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.findByEmail("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams(
					ContributorRestService.PARAM_EMAIL, "email@em"));
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.findByEmail("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams(
					ContributorRestService.PARAM_EMAIL, "email@em"));
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.findByEmail("email@em")).thenThrow(new RuntimeException("test exception"));
			tested.search(TestUtils.prepareUriInfiWithParams(ContributorRestService.PARAM_EMAIL, "email@em"));
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	@Test
	public void search_byCode() throws Exception {
		ContributorRestService tested = getTested();

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.findByCode("e j <email@em>")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams(
					ContributorRestService.PARAM_CODE, "e j <email@em>"));
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.findByCode("e j <email@em>")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(TestUtils.prepareUriInfiWithParams(
					ContributorRestService.PARAM_CODE, "e j <email@em>"));
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.findByCode("e j <email@em>")).thenThrow(
					new RuntimeException("test exception"));
			tested.search(TestUtils.prepareUriInfiWithParams(ContributorRestService.PARAM_CODE, "e j <email@em>"));
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void codeChange_validation_id() throws Exception {
		getTested().codeChange(null, "code");
	}

	@Test(expected = RequiredFieldException.class)
	public void codeChange_validation_id2() throws Exception {
		getTested().codeChange(" ", "code");
	}

	@Test(expected = RequiredFieldException.class)
	public void codeChange_validation_code() throws Exception {
		getTested().codeChange("id", null);
	}

	@Test(expected = RequiredFieldException.class)
	public void codeChange_validation_code2() throws Exception {
		getTested().codeChange("id", " ");
	}

	@Test
	public void codeChange() throws Exception {
		ContributorRestService tested = getTested();

		Map<String, Object> value = new HashMap<String, Object>();
		Mockito.when(tested.contributorService.changeContributorCode("id", "code")).thenReturn(value);
		Assert.assertEquals(value, tested.codeChange("id", "code"));
	}

	@Test(expected = BadRequestException.class)
	public void codeChange_exception() throws Exception {
		ContributorRestService tested = getTested();
		Mockito.doThrow(new BadRequestException("a")).when(tested.contributorService)
				.changeContributorCode(Mockito.anyString(), Mockito.anyString());
		tested.codeChange("id", "code");
	}

	@Test(expected = RequiredFieldException.class)
	public void mergeContributors_validation_idFrom() throws Exception {
		getTested().mergeContributors(null, "idto");
	}

	@Test(expected = RequiredFieldException.class)
	public void mergeContributors_validation_idFrom2() throws Exception {
		getTested().mergeContributors(" ", "idto");
	}

	@Test(expected = RequiredFieldException.class)
	public void mergeContributors_validation_idTo() throws Exception {
		getTested().mergeContributors("idfrom", null);
	}

	@Test(expected = RequiredFieldException.class)
	public void mergeContributors_validation_idTo2() throws Exception {
		getTested().mergeContributors("idfrom", " ");
	}

	@Test
	public void mergeContributors() throws Exception {
		ContributorRestService tested = getTested();
		Map<String, Object> value = new HashMap<String, Object>();
		Mockito.when(tested.contributorService.mergeContributors("from", "to")).thenReturn(value);
		Assert.assertEquals(value, tested.mergeContributors("idFrom", "idTo"));
	}

	@Test(expected = ObjectNotFoundException.class)
	public void mergeContributors_exception() throws Exception {
		ContributorRestService tested = getTested();
		Mockito.when(tested.contributorService.mergeContributors(Mockito.anyString(), Mockito.anyString())).thenThrow(
				new ObjectNotFoundException());
		tested.mergeContributors("id", "id2");
	}

	private ContributorRestService getTested() {
		ContributorRestService tested = new ContributorRestService();
		tested.contributorService = Mockito.mock(ContributorService.class);
		RestEntityServiceBaseTest.mockLogger(tested);
		return tested;
	}

}
