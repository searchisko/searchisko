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
	public List<Tag> getTagsByContentType(String contentType) {
		return em.createQuery("SELECT t FROM Tag t WHERE SUBSTRING(t.contentId, 1, ?1) = ?2")
			.setParameter(1, contentType.length())
			.setParameter(2, contentType)
			.getResultList();
	}

	@Override
	public boolean createTag(Tag tag) {
		if (!(tag instanceof Tag)) {
			return false;
		}
		if (tag.getTagLabel() == null) {
			return false;
		}

		// to lower case
		tag.setTagLabel(tag.getTagLabel().toLowerCase());

		// check if there is same tag for the same content
		for (Tag item : getTagsByContent(tag.getContentId())) {
			if (item.getTagLabel().equals(tag.getTagLabel())) {
				// tag already exists
				return false;
			}
		}

		tag.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		em.persist(tag);
		return true;
	}

	@Override
	public void deleteTag(String contentId, String tagLabel) {
		tagLabel = tagLabel.toLowerCase();
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

		List<Tag> tags = em.createQuery("FROM Tag").getResultList();
		for (Tag tag : tags) {
			if (tag.getContributorId().equals(contributorFrom)) {
				tag.setContributorId(contributorTo);
				em.merge(tag);
			}
		}
	}

}
