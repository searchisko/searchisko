/**
 * 
 */
package org.jboss.dcp.persistence.jpa.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author Libor Krzyzanek
 * 
 */
public class ProjectConverterTest {

	@Test
	public void testConvertToModel() throws IOException {
		ProjectConverter converter = new ProjectConverter();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("code", "as7");
		data.put("name", "AS 7");

		Project p = converter.convertToModel(data);

		assertEquals("as7", p.getCode());
		assertEquals("{\"name\":\"AS 7\",\"code\":\"as7\"}", p.getValue());
	}

}
