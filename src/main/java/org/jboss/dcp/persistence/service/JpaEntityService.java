/**
 * 
 */
package org.jboss.dcp.persistence.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.jboss.dcp.api.rest.ESDataOnlyResponse;
import org.jboss.dcp.persistence.jpa.model.ModelToJSONMapConverter;

/**
 * JPA implementation of entity service. It's not session bean because type is unknown.
 * 
 * @author Libor Krzyzanek
 * 
 */
public class JpaEntityService<T> implements EntityService {

	private EntityManager em;

	protected ModelToJSONMapConverter<T> converter;

	protected Class<T> entityType;

	public JpaEntityService(EntityManager em, ModelToJSONMapConverter<T> converter, Class<T> entityType) {
		this.em = em;
		this.converter = converter;
		this.entityType = entityType;
	}

	@Override
	public StreamingOutput getAll(Integer from, Integer size, final String[] fieldsToRemove) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> criteria = cb.createQuery(entityType);
		Root<T> root = criteria.from(entityType);
		criteria.select(root);
		final List<T> result = em.createQuery(criteria).getResultList();

		return new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				XContentBuilder builder = XContentFactory.jsonBuilder(output);
				// shows only hits
				builder.startObject();
				if (result != null) {
					// TODO: PERSISTENCE - total should be count of all rows
					builder.field("total", result.size());
					builder.startArray("hits");
					for (T t : result) {
						Map<String, Object> jsonData = converter.convertToJsonMap(t);
						// TODO: PERSISTENCE - Add ID of entity
						builder.startObject();
						builder.field("id", "TODO");
						builder.field("data", ESDataOnlyResponse.removeFields(jsonData, fieldsToRemove));
						builder.endObject();
					}
				} else {
					builder.field("total", 0);
					builder.startArray("hits");
				}
				builder.endArray();
				builder.endObject();
				builder.close();
			}
		};

	}

	@Override
	public Map<String, Object> get(String id) {
		T jpaEntity = em.find(entityType, id);
		try {
			return converter.convertToJsonMap(jpaEntity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String create(Map<String, Object> entity) {
		Random randomGenerator = new Random();
		randomGenerator.nextLong();

		String generatedId = Long.toString(randomGenerator.nextLong());

		create(generatedId, entity);

		return generatedId;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void create(String id, Map<String, Object> entity) {
		try {
			Object jpaEntity = converter.convertToModel(entity);
			em.persist(jpaEntity);
			em.flush();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void update(String id, Map<String, Object> entity) {
		T jpaEntity = em.find(entityType, id);

		try {
			converter.updateValue(jpaEntity, entity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		em.merge(jpaEntity);
		em.flush();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void delete(String id) {
		T reference = em.getReference(entityType, id);
		em.remove(reference);
	}

}
