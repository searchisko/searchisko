package org.searchisko.persistence.jpa.model;

import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link QueryConverter}.
 *
 * @author Lukas Vlcek
 */
public class QueryConverterTest {

	@Test
	public void convertToModel() throws IOException {
		QueryConverter converter = new QueryConverter();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("template", "{\"query\":{\"match_all\":{}}}");

		Query p = converter.convertToModel("matchAllQuery", data);

		assertEquals("matchAllQuery", p.getName());
		TestUtils.assertJsonContent("{\"template\":\"{\\\"query\\\":{\\\"match_all\\\":{}}}\"}", p.getValue());

	}

	@Test
	public void convertToModelNullTemplate() throws IOException {
		QueryConverter converter = new QueryConverter();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("template", null);

		Query p = converter.convertToModel("nullTemplateQuery", data);
		assertEquals("{\"template\":null}", p.getValue());
	}

	@Test
	public void convertToModelMissingTemplate() throws IOException {
		QueryConverter converter = new QueryConverter();

		Map<String, Object> data = new HashMap<String, Object>();
		Query p = converter.convertToModel("missingTemplateQuery", data);
		assertEquals("{}", p.getValue());
	}
}
