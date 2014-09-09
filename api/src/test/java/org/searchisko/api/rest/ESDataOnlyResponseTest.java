/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.json.JSONException;
import org.searchisko.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ESDataOnlyResponse}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ESDataOnlyResponseTest {

	@Test
	public void write_basic() throws WebApplicationException, IOException, JSONException {

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(null);
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", tested);
		}

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse(null, null, null, null));
			TestUtils.assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", tested);
		}

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", null, null));
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"1\",\"data\":{\"sys_name\":\"name1\",\"sys_id\":\"1\"}}]}", tested);
		}

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", "35", "myname"));
			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":2,\"hits\":[{\"id\":\"1\",\"data\":{\"sys_name\":\"name1\",\"sys_id\":\"1\"}},{\"id\":\"35\",\"data\":{\"sys_name\":\"myname\",\"sys_id\":\"35\"}}]}",
							tested);
		}
	}

	@Test
	public void write_filtering() throws WebApplicationException, IOException, JSONException {

		// case - testing source filtering
		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", "35", "myname"),
					new String[] { "sys_name" });
			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":2,\"hits\":[{\"id\":\"1\",\"data\":{\"sys_id\":\"1\"}},{\"id\":\"35\",\"data\":{\"sys_id\":\"35\"}}]}",
							tested);
		}
	}

	@Test
	public void write_idfield() throws WebApplicationException, IOException, JSONException {

		// case - testing source filtering
		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", "35", "myname"), "sys_name");
			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":2,\"hits\":[{\"id\":\"name1\",\"data\":{\"sys_name\":\"name1\",\"sys_id\":\"1\"}},{\"id\":\"myname\",\"data\":{\"sys_name\":\"myname\",\"sys_id\":\"35\"}}]}",
							tested);
		}
	}

	@Test
	public void write_filtering_idfield() throws WebApplicationException, IOException, JSONException {

		// case - testing source filtering
		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", "35", "myname"), "sys_name",
					new String[] { "sys_name" });
			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":2,\"hits\":[{\"id\":\"name1\",\"data\":{\"sys_id\":\"1\"}},{\"id\":\"myname\",\"data\":{\"sys_id\":\"35\"}}]}",
							tested);
		}
	}

	/**
	 * Create mock {@link SearchResponse} instance with passed in values.
	 *
	 * @param id1 id of first result record - can be null
	 * @param name1 additional data for first record
	 * @param id2 id of second result record - can be null
	 * @param name2 additional data for second record
	 * @return mock instance
	 */
	public static SearchResponse mockSearchResponse(String id1, String name1, String id2, String name2) {

		SearchResponse sr = Mockito.mock(SearchResponse.class);
		SearchHits sh = Mockito.mock(SearchHits.class);

		int len = 0;
		if (id2 != null) {
			len = 2;
		} else if (id1 != null) {
			len = 1;
		}
		SearchHit[] s = new SearchHit[len];
		if (len > 0) {
			s[0] = prepareSH(id1, name1);
		}
		if (len > 1) {
			s[1] = prepareSH(id2, name2);
		}

		Mockito.when(sr.getHits()).thenReturn(sh);
		Mockito.when(sh.getTotalHits()).thenReturn((long) s.length);
		Mockito.when(sh.getHits()).thenReturn(s);
		return sr;
	}

	private static SearchHit prepareSH(String id, String name) {
		SearchHit sh = Mockito.mock(SearchHit.class);
		Mockito.when(sh.getId()).thenReturn(id);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("sys_id", id);
		map.put("sys_name", name);
		Mockito.when(sh.sourceAsMap()).thenReturn(map);
		return sh;
	}

}
