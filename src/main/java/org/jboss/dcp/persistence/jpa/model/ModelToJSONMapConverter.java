/**
 * 
 */
package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * @author Libor Krzyzanek
 * 
 */
public interface ModelToJSONMapConverter<T> {

	public T convertToModel(Map<String, Object> jsonMap) throws IOException;

	public Map<String, Object> convertToJsonMap(T jpaEntity) throws JsonParseException, JsonMappingException,
			IOException;

	public void updateValue(T jpaEntity, Map<String, Object> jsonMapValue) throws IOException;

}
