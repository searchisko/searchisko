package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * @author Libor Krzyzanek
 * 
 */
public abstract class StringValueConverter<T> extends CommonConverter<T> {

	public abstract String getValue(T jpaEntity);

	public abstract void setValue(T jpaEntity, String value);

	@Override
	public Map<String, Object> convertToJsonMap(T jpaEntity) throws JsonParseException, JsonMappingException,
			IOException {
		return convertToJsonMap(getValue(jpaEntity).getBytes());
	}

	@Override
	public void updateValue(T jpaEntity, Map<String, Object> jsonMapValue) throws IOException {
		String value = convertJsonMapToString(jsonMapValue);
		setValue(jpaEntity, value);
	}

}
