/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ESDataOnlyResponse}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ESDataOnlyResponseTest {

	@Test
	public void write() throws WebApplicationException, IOException {

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse(null, null, null, null));
			assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", tested);
		}

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", null, null));
			assetStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"1\",\"data\":{\"dcp_id\":\"1\",\"dcp_name\":\"name1\"}}]}", tested);
		}

		{
			ESDataOnlyResponse tested = new ESDataOnlyResponse(mockSearchResponse("1", "name1", "35", "myname"));
			assetStreamingOutputContent(
					"{\"total\":2,\"hits\":[{\"id\":\"1\",\"data\":{\"dcp_id\":\"1\",\"dcp_name\":\"name1\"}},{\"id\":\"35\",\"data\":{\"dcp_id\":\"35\",\"dcp_name\":\"myname\"}}]}",
					tested);
		}
	}

	/**
	 * Assert string value equals one written by the StreamingOutput
	 * 
	 * @param expected value
	 * @param actual value
	 * @throws IOException
	 */
	public static void assetStreamingOutputContent(String expected, Object actual) throws IOException {
		if (!(actual instanceof StreamingOutput)) {
			Assert.fail("Result must be StreamingOutput but is " + actual);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		((StreamingOutput) actual).write(output);
		Assert.assertEquals(expected, output.toString());
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
		map.put("dcp_id", id);
		map.put("dcp_name", name);
		Mockito.when(sh.sourceAsMap()).thenReturn(map);
		return sh;
	}

}
