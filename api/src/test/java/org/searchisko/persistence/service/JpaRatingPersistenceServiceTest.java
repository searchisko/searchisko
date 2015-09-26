/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.persistence.jpa.model.Rating;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * Unit test for {@link JpaRatingPersistenceService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class JpaRatingPersistenceServiceTest extends JpaTestBase {
	private static final String CONTENT_ID_3 = "contId3";
	private static final String CONTENT_ID_2 = "contId2";
	private static final String CONTENT_ID_1 = "contId1";
	private static final String CONTRIB_ID_2 = "contribId2";
	private static final String CONTRIB_ID_1 = "contribId1";
	private static final String CONTRIB_ID_3 = "contribId3";
	private static final String CONTRIB_ID_4 = "contribId4";
	private static final String CONTRIB_ID_5 = "contribId5";

	{
		logger = Logger.getLogger(JpaRatingPersistenceServiceTest.class.getName());
	}

	@Test
	public void getRatings_rate() {
		JpaRatingPersistenceService tested = getTested();

		// case - get from empty store
		em.getTransaction().begin();
		List<Rating> ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1);
		Assert.assertTrue(ret.isEmpty());

		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertTrue(ret.isEmpty());

		// case - rate some content
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);

		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - get rated content
		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_2);
		Assert.assertEquals(0, ret.size());

		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1);
		Assert.assertEquals(1, ret.size());

		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertEquals(2, ret.size());

		ret = tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertEquals(1, ret.size());
		Assert.assertEquals(CONTENT_ID_1, ret.get(0).getContentId());
		Assert.assertEquals(CONTRIB_ID_2, ret.get(0).getContributorId());
		Assert.assertEquals(5, ret.get(0).getRating());
		Assert.assertNotNull(ret.get(0).getRatedAt());

		// case - update some rating
		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 2);
		em.getTransaction().commit();

		em.getTransaction().begin();
		ret = tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertEquals(1, ret.size());
		Assert.assertEquals(CONTENT_ID_1, ret.get(0).getContentId());
		Assert.assertEquals(CONTRIB_ID_2, ret.get(0).getContributorId());
		Assert.assertEquals(2, ret.get(0).getRating());
		Assert.assertNotNull(ret.get(0).getRatedAt());
		em.getTransaction().commit();

	}

	protected JpaRatingPersistenceService getTested() {
		JpaRatingPersistenceService tested = new JpaRatingPersistenceService();
		tested.em = em;
		return tested;
	}

	@Test
	public void countRatingStats() {
		JpaRatingPersistenceService tested = getTested();

		// create some content
		em.getTransaction().begin();
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);
		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);
		em.getTransaction().commit();

		em.getTransaction().begin();

		RatingStats rs = tested.countRatingStats(CONTENT_ID_1);
		Assert.assertEquals(CONTENT_ID_1, rs.getContentId());
		Assert.assertEquals(3, rs.getAverage(), 0.01);
		Assert.assertEquals(2, rs.getNumber());

		// case - unrated content
		Assert.assertNull(tested.countRatingStats(CONTENT_ID_2));

		rs = tested.countRatingStats(CONTENT_ID_3);
		Assert.assertEquals(CONTENT_ID_3, rs.getContentId());
		Assert.assertEquals(4, rs.getAverage(), 0.01);
		Assert.assertEquals(1, rs.getNumber());

		em.getTransaction().commit();
	}

	@Test
	public void mergeRatingsForContributors() {
		JpaRatingPersistenceService tested = getTested();

		em.getTransaction().begin();
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);

		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_2, 3);

		tested.rate(CONTRIB_ID_3, CONTENT_ID_1, 2);
		tested.rate(CONTRIB_ID_3, CONTENT_ID_2, 2);
		em.getTransaction().commit();
		
		try {
			// testing merging of contributors that didn't have any ratings
			// this was initially causing SQLException, so in case the bug repeats 
			// we expect it and close the transaction in finally block, 
			// otherwise the test hangs indefinitely
			em.getTransaction().begin();
			tested.mergeRatingsForContributors(CONTRIB_ID_4, CONTRIB_ID_5);
		} finally {
			em.getTransaction().commit();
		}
		
		em.getTransaction().begin();
		tested.mergeRatingsForContributors(CONTRIB_ID_1, CONTRIB_ID_2);
		em.getTransaction().commit();

		em.getTransaction().begin();
		
		// assert that contributors without ratings still don't have them
		Assert.assertEquals(0,tested.getRatings(CONTRIB_ID_4, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		Assert.assertEquals(0,tested.getRatings(CONTRIB_ID_5, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		
		// assert removed from first
		Assert.assertEquals(0, tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());

		// assert merged to second
		Assert.assertEquals(3, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		Assert.assertEquals(5, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1).get(0).getRating());
		Assert.assertEquals(3, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_2).get(0).getRating());
		Assert.assertEquals(4, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_3).get(0).getRating());

		// assert third untouched
		Assert.assertEquals(2, tested.getRatings(CONTRIB_ID_3, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		em.getTransaction().commit();

	}

	@Test
	public void deleteRatingsForContributor() {
		JpaRatingPersistenceService tested = getTested();

		em.getTransaction().begin();
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);

		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_2, 3);

		tested.rate(CONTRIB_ID_3, CONTENT_ID_1, 2);
		tested.rate(CONTRIB_ID_3, CONTENT_ID_2, 2);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - no NPE
		tested.deleteRatingsForContributor(null);
		// case - delete
		tested.deleteRatingsForContributor(CONTRIB_ID_1);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// assert removed from first
		Assert.assertEquals(0, tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());

		// assert second untouched
		Assert.assertEquals(2, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		// assert third untouched
		Assert.assertEquals(2, tested.getRatings(CONTRIB_ID_3, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		em.getTransaction().commit();

	}

	@Test
	public void deleteRatingsForContent() {
		JpaRatingPersistenceService tested = getTested();

		em.getTransaction().begin();
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);

		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_2, 3);

		tested.rate(CONTRIB_ID_3, CONTENT_ID_1, 2);
		tested.rate(CONTRIB_ID_3, CONTENT_ID_2, 2);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - no NPE
		tested.deleteRatingsForContent((String[]) null);
		tested.deleteRatingsForContent(new String[] {});
		// case - delete for one
		tested.deleteRatingsForContent(CONTENT_ID_1);
		em.getTransaction().commit();

		em.getTransaction().begin();
		Assert.assertEquals(1, tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		Assert.assertEquals(1, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		Assert.assertEquals(1, tested.getRatings(CONTRIB_ID_3, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - delete for more
		tested.deleteRatingsForContent(CONTENT_ID_2, CONTENT_ID_3);
		em.getTransaction().commit();

		em.getTransaction().begin();
		Assert.assertEquals(0, tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		Assert.assertEquals(0, tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		Assert.assertEquals(0, tested.getRatings(CONTRIB_ID_3, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3).size());
		em.getTransaction().commit();

	}

}
