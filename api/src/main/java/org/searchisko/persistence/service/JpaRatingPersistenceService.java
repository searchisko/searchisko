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
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.searchisko.persistence.jpa.model.Rating;

/**
 * JPA based implementation of {@link RatingPersistenceService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Stateless
@LocalBean
public class JpaRatingPersistenceService implements RatingPersistenceService {

	@Inject
	protected EntityManager em;

	@Override
	public List<Rating> getRatings(String contributorId, String... contentId) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Rating> queryList = cb.createQuery(Rating.class);
		Root<Rating> rating = queryList.from(Rating.class);
		queryList.select(rating);
		queryList.where(cb.and(cb.equal(rating.get("contributorId"), contributorId),
				rating.get("contentId").in((Object[]) contentId)));
		TypedQuery<Rating> q = em.createQuery(queryList);
		return q.getResultList();
	}

	@Override
	public void rate(String contributorId, String contentId, int rating) {
		Rating jpaEntity = null;
		List<Rating> rl = getRatings(contributorId, contentId);
		boolean newEntity = false;
		if (rl != null && !rl.isEmpty()) {
			jpaEntity = rl.get(0);
			em.lock(jpaEntity, LockModeType.PESSIMISTIC_WRITE);
		} else {
			jpaEntity = new Rating();
			jpaEntity.setContentId(contentId);
			jpaEntity.setContributorId(contributorId);
			newEntity = true;
		}
		jpaEntity.setRating(rating);
		jpaEntity.setRatedAt(new Timestamp(System.currentTimeMillis()));
		if (newEntity) {
			em.persist(jpaEntity);
		}

	}

	private static String STAT_QUERY = "select AVG(r.rating), COUNT(r.contributorId) from Rating r where r.contentId = ?1";

	@Override
	public RatingStats countRatingStats(String contentId) {
		Object[] ret = (Object[]) em.createQuery(STAT_QUERY).setParameter(1, contentId).getSingleResult();
		if (ret != null && ret.length > 0 && ret[0] != null) {
			return new RatingStats(contentId, (Double) ret[0], (Long) ret[1]);
		}
		return null;
	}

	@Override
	public void mergeRatingsForContributors(String contributorIdFrom, String contributorIdTo) {
		
		if (contributorIdFrom == null || contributorIdTo == null) {
			return;
		}
		
		List<String> contentIds = em.createQuery("select r.contentId from Rating r where r.contributorId = ?1",String.class)
			.setParameter(1, contributorIdTo)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.getResultList();
		
		if(contentIds==null || contentIds.size()==0) {
			return;
		}
		
		em.createQuery(
				"update Rating r set r.contributorId = ?1 where r.contributorId = ?2 and r.contentId not in (?3)")
				.setParameter(1, contributorIdTo).setParameter(2, contributorIdFrom).setParameter(3, contentIds)
				.executeUpdate();
		em.createQuery("delete from Rating r where r.contributorId = ?1").setParameter(1, contributorIdFrom)
				.executeUpdate();
	}

	@Override
	public void deleteRatingsForContributor(String contributorId) {
		if (contributorId != null) {
			em.createQuery("delete from Rating r where r.contributorId = ?1").setParameter(1, contributorId).executeUpdate();
		}
	}

	@Override
	public void deleteRatingsForContent(String... contentId) {
		if (contentId != null && contentId.length > 0) {
			em.createQuery("delete from Rating r where r.contentId in ?1").setParameter(1, Arrays.asList(contentId))
					.executeUpdate();
		}
	}

}
