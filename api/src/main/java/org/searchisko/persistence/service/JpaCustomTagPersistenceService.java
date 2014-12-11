/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.searchisko.persistence.jpa.model.Tag;

/**
 * JPA based implementation of {@link CustomTagPersistenceService}.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
@Stateless
@LocalBean
public class JpaCustomTagPersistenceService implements CustomTagPersistenceService {

	@Inject
	protected EntityManager em;

	@Override
	public List<Tag> getTagsByContent(String... contentId) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Tag> queryList = cb.createQuery(Tag.class);
		Root<Tag> tagRoot = queryList.from(Tag.class);
		queryList.select(tagRoot);
		queryList.where(cb.isTrue(tagRoot.get("contentId").in((Object[]) contentId)));
		TypedQuery<Tag> q = em.createQuery(queryList);
		return q.getResultList();
	}

	@Override
	public List<Tag> getAllTags() {
		return em.createQuery("from Tag").getResultList();
	}

	@Override
	public boolean createTag(Tag tag) {
		// check if there is same tag for the same content
		List<Tag> tagList = getTagsByContent(tag.getContentId());
		if (tagList.contains(tag)) {
			// tag already exists
			return false;
		}
		tag.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		em.persist(tag);
		return true;
	}

	@Override
	public void deleteTag(String contentId, String tagLabel) {
		if ((contentId != null) && (tagLabel!= null)) {
			em.createQuery("delete from Tag t where t.contentId = ?1 and t.tagLabel = ?2")
				.setParameter(1, contentId)
				.setParameter(2, tagLabel)
				.executeUpdate();
		}
	}

	@Override
	public void deleteTagsForContent(String... contentId) {
		if (contentId != null && contentId.length > 0)
			em.createQuery("delete from Tag t where t.contentId in ?1").setParameter(1, Arrays.asList(contentId))
					.executeUpdate();
	}

	@Override
	public void changeOwnershipOfTags(String contributorFrom, String contributorTo) {
		if (contributorFrom == null || contributorTo == null)
			return;

		em.createQuery(
				"update Tag t set t.contributorId = ?1 where t.contributorId = ?2")
				.setParameter(1, contributorTo)
				.setParameter(2, contributorFrom)
				.executeUpdate();
	}

}
