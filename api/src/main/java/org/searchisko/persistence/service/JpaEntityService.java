/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.searchisko.api.rest.ESDataOnlyResponse;
import org.searchisko.persistence.jpa.model.ModelToJSONMapConverter;

/**
 * JPA implementation of entity service. It's not session bean because type is unknown, so must be called from Session
 * bean to work with transactions!
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class JpaEntityService<T> implements EntityService {

	protected Logger log;

	private EntityManager em;

	protected ModelToJSONMapConverter<T> converter;

	protected Class<T> entityType;

	public JpaEntityService(EntityManager em, ModelToJSONMapConverter<T> converter, Class<T> entityType) {
		log = Logger.getLogger(this.getClass().getName());
		this.em = em;
		this.converter = converter;
		this.entityType = entityType;
	}

	@Override
	public StreamingOutput getAll(Integer from, Integer size, final String[] fieldsToRemove) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		final List<T> result = listEntities(cb, from, size);

		CriteriaQuery<Long> queryCount = cb.createQuery(Long.class);
		queryCount.select(cb.count(queryCount.from(entityType)));
		final long count = em.createQuery(queryCount).getSingleResult();

		return new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				XContentBuilder builder = XContentFactory.jsonBuilder(output);
				builder.startObject();
				if (result != null) {
					builder.field("total", count);
					builder.startArray("hits");
					for (T t : result) {
						Map<String, Object> jsonData = converter.convertToJsonMap(t);
						builder.startObject();
						builder.field("id", converter.getId(t));
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

	protected List<T> listEntities(CriteriaBuilder cb, Integer from, Integer size) {
		CriteriaQuery<T> queryList = cb.createQuery(entityType);
		Root<T> root = queryList.from(entityType);
		queryList.select(root);
		queryList.orderBy(cb.asc(root.get(converter.getEntityIdFieldName())));
		TypedQuery<T> q = em.createQuery(queryList);
		if (from != null && from >= 0)
			q.setFirstResult(from);
		if (size != null && size > 0)
			q.setMaxResults(size);
		return q.getResultList();
	}

	@Override
	public List<Map<String, Object>> getAll() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> criteria = cb.createQuery(entityType);
		Root<T> root = criteria.from(entityType);
		criteria.select(root);
		final List<T> result = em.createQuery(criteria).getResultList();

		List<Map<String, Object>> ret = new ArrayList<>();
		try {
			for (T row : result) {
				ret.add(converter.convertToJsonMap(row));
			}
			return ret;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Object> get(String id) {
		T jpaEntity = em.find(entityType, id);
		if (jpaEntity == null) {
			return null;
		}
		try {
			return converter.convertToJsonMap(jpaEntity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String create(Map<String, Object> entity) {
		String id = Strings.randomBase64UUID();
		create(id, entity);
		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		try {
			T jpaEntity = em.find(entityType, id);
			if (jpaEntity != null) {
				// Entity exists. Only update the value
				converter.updateValue(jpaEntity, entity);
			} else {
				jpaEntity = converter.convertToModel(id, entity);
				em.persist(jpaEntity);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		create(id, entity);
	}

	@Override
	public void delete(String id) {
		try {
			T reference = em.getReference(entityType, id);
			em.remove(reference);
		} catch (EntityNotFoundException e) {
			// OK
		}
	}

	protected int LIST_PAGE_SIZE = 200;

	protected static class ListRequestImpl implements ListRequest {

		List<ContentTuple<String, Map<String, Object>>> content;
		int beginIndex = 0;

		protected ListRequestImpl(int beginIndex, List<ContentTuple<String, Map<String, Object>>> content) {
			super();
			this.beginIndex = beginIndex;
			this.content = content;
		}

		@Override
		public boolean hasContent() {
			return content != null && !content.isEmpty();
		}

		@Override
		public List<ContentTuple<String, Map<String, Object>>> content() {
			return content;
		}

	}

	@Override
	public ListRequest listRequestInit() {
		return listRequestImpl(0);
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		ListRequestImpl lr = (ListRequestImpl) previous;
		return listRequestImpl(lr.beginIndex + LIST_PAGE_SIZE);
	}

	protected ListRequest listRequestImpl(int beginIndex) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		final List<T> result = listEntities(cb, beginIndex, LIST_PAGE_SIZE);

		List<ContentTuple<String, Map<String, Object>>> content = new ArrayList<>(10);

		for (T data : result) {
			try {
				content.add(converter.convertToContentTuple(data));
			} catch (IOException e) {
				log.severe("Could not convert Entity.id=" + converter.getId(data)
						+ " JSON content to valid object so skip it: " + e.getMessage());
			}
		}

		return new ListRequestImpl(beginIndex, content);
	}

}
