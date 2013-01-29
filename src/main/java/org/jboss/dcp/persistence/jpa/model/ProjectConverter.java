/**
 * 
 */
package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

/**
 * @author Libor Krzyzanek
 * 
 */
public class ProjectConverter extends StringValueConverter<Project> {

	@Override
	public String getValue(Project jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Project jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Project convertToModel(Map<String, Object> jsonMap) throws IOException {
		Project p = new Project();
		p.setCode(jsonMap.get("code").toString());
		updateValue(p, jsonMap);
		return p;
	}
}
