package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * @author Libor Krzyzanek
 * 
 */
public abstract class CommonConverter<T> implements ModelToJSONMapConverter<T> {

	public String convertJsonMapToString(Map<String, Object> jsonMapValue) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(jsonMapValue);
	}

	public Map<String, Object> convertToJsonMap(byte[] jsonData) throws JsonParseException, JsonMappingException,
			IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {
		});

	}
}
