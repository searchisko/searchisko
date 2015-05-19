/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.persistence.jpa.model.Tag;

/**
 * Unit test for {@link JpaCustomTagPersistenceService}
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
public class JpaCustomTagPersistenceServiceTest extends JpaTestBase {

	private static final String CONTENT_TYPE_1 = "type1";
	private static final String CONTENT_TYPE_2 = "type2";
	private static final String CONTENT_TYPE_1_ID_2 = "type1-contId2";
	private static final String CONTENT_TYPE_1_ID_1 = "type1-contId1";
	private static final String CONTENT_TYPE_2_ID_2 = "type2-contId2";
	private static final String CONTENT_TYPE_2_ID_1 = "type2-contId1";
	private static final String CONTRIB_ID_2 = "contribId2";
	private static final String CONTRIB_ID_1 = "contribId1";

	{
		logger = Logger.getLogger(JpaCustomTagPersistenceServiceTest.class.getName());
	}

	@Test
	public void getTags_createTag() {
		JpaCustomTagPersistenceService tested = getTested();

		// case - get from empty store
		em.getTransaction().begin();
		List<Tag> ret = tested.getTagsByContent(CONTENT_TYPE_1_ID_1);
		Assert.assertTrue(ret.isEmpty());

		Tag tag1 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_1, "label1");
		Tag tag2 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_2, "label2");
		Tag tag3 = new Tag(CONTENT_TYPE_1_ID_2, CONTRIB_ID_2, "label3");
		Tag tag4 = new Tag(CONTENT_TYPE_2_ID_1, CONTRIB_ID_2, "label4");

		// create tags
		tested.createTag(tag1);
		tested.createTag(tag2);
		tested.createTag(tag3);
		tested.createTag(tag4);

		em.getTransaction().commit();
		em.getTransaction().begin();

		// get tag for content 1
		ret = tested.getTagsByContent(CONTENT_TYPE_1_ID_1);
		Assert.assertEquals(2, ret.size());
		ret = tested.getTagsByContent(CONTENT_TYPE_1_ID_2);
		Assert.assertEquals(1, ret.size());
		Tag testedTag = ret.get(0);
		Assert.assertEquals(tag3.getContentId(), testedTag.getContentId());
		Assert.assertEquals(tag3.getContributorId(), testedTag.getContributorId());
		Assert.assertEquals(tag3.getTagLabel(), testedTag.getTagLabel());
		Assert.assertEquals(tag3.getCreatedAt(), testedTag.getCreatedAt());

		em.getTransaction().commit();
		em.getTransaction().begin();

		// get tags for content type 1
		ret = tested.getTagsByContentType(CONTENT_TYPE_1);
		Assert.assertEquals(3, ret.size());

		List<String> labels = new ArrayList<>();
		for (Tag tag : ret) {
			labels.add(tag.getTagLabel());
		}

		Assert.assertTrue(labels.contains("label1"));
		Assert.assertTrue(labels.contains("label2"));
		Assert.assertTrue(labels.contains("label3"));
		em.getTransaction().commit();

		// get tags for content type 2
		em.getTransaction().begin();
		ret = tested.getTagsByContentType(CONTENT_TYPE_2);
		Assert.assertEquals(1, ret.size());

		Assert.assertTrue(ret.get(0).getTagLabel().equals("label4"));
		em.getTransaction().commit();

		// get tags for nonexisting content type
		em.getTransaction().begin();
		ret = tested.getTagsByContentType("nonexistingType");
		Assert.assertEquals(0, ret.size());
		em.getTransaction().commit();
	}

	@Test
	public void changeOwnershipOfTags() {
		JpaCustomTagPersistenceService tested = getTested();

		em.getTransaction().begin();
		// create tags
		Tag tag1 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_1, "label1");
		Tag tag2 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_2, "label2");
		Tag tag3 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_2, "label3");

		tested.createTag(tag1);
		tested.createTag(tag2);
		tested.createTag(tag3);

		List<Tag> ret = tested.getTagsByContentType(CONTENT_TYPE_1_ID_1);
		Assert.assertEquals(3, ret.size());

		int count = 0;
		for (Tag tag : ret) {
			if (tag.getContributorId().equals(CONTRIB_ID_2)) {
				count++;
			}
		}

		Assert.assertEquals(2, count);

		System.out.println("pred: " + tested.getTagsByContentType(CONTENT_TYPE_1_ID_1));
		em.getTransaction().commit();

		em.getTransaction().begin();
		tested.changeOwnershipOfTags(CONTRIB_ID_1, CONTRIB_ID_2);
		em.getTransaction().commit();

		em.getTransaction().begin();
		System.out.println("po: " + tested.getTagsByContentType(CONTENT_TYPE_1_ID_1));

		ret = tested.getTagsByContentType(CONTENT_TYPE_1_ID_1);
		Assert.assertEquals(3, ret.size());

		count = 0;
		for (Tag tag : ret) {
			if (tag.getContributorId().equals(CONTRIB_ID_2)) {
				count++;
			}
		}

		Assert.assertEquals(3, count);
		em.getTransaction().commit();
	}

	@Test
	public void deleteTag() {
		JpaCustomTagPersistenceService tested = getTested();

		Tag tag1 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_1, "label1");
		Tag tag2 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_2, "label2");
		Tag tag3 = new Tag(CONTENT_TYPE_1_ID_2, CONTRIB_ID_2, "label3");

		em.getTransaction().begin();
		tested.createTag(tag1);
		tested.createTag(tag2);
		tested.createTag(tag3);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - nonexisting label
		tested.deleteTag(CONTENT_TYPE_1_ID_1, "nonexisting label");
		List<Tag> ret = tested.getTagsByContentType(CONTENT_TYPE_1);
		Assert.assertEquals(3, ret.size());
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - existing label with other content id
		tested.deleteTag(CONTENT_TYPE_1_ID_2, "label2");
		ret = tested.getTagsByContentType(CONTENT_TYPE_1);
		Assert.assertEquals(3, ret.size());
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - delete label1
		tested.deleteTag(CONTENT_TYPE_1_ID_1, "label1");
		ret = tested.getTagsByContentType(CONTENT_TYPE_1);
		Assert.assertEquals(2, ret.size());
		Tag testedTag1 = ret.get(0);
		Tag testedTag2 = ret.get(1);

		if (testedTag1.getTagLabel().equals("label2")) {
			Assert.assertEquals(tag2.getContentId(), testedTag1.getContentId());
			Assert.assertEquals(tag2.getContributorId(), testedTag1.getContributorId());

			Assert.assertEquals(tag3.getContentId(), testedTag2.getContentId());
			Assert.assertEquals(tag3.getContributorId(), testedTag2.getContributorId());
			Assert.assertEquals("label3", testedTag2.getTagLabel());
		} else {
			Assert.assertEquals(tag2.getContentId(), testedTag2.getContentId());
			Assert.assertEquals(tag2.getContributorId(), testedTag2.getContributorId());

			Assert.assertEquals(tag3.getContentId(), testedTag1.getContentId());
			Assert.assertEquals(tag3.getContributorId(), testedTag1.getContributorId());
			Assert.assertEquals("label3", testedTag1.getTagLabel());
		}

		em.getTransaction().commit();
	}

	@Test
	public void deleteTagsForContent() {
		JpaCustomTagPersistenceService tested = getTested();

		Tag tag1 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_1, "label1");
		Tag tag2 = new Tag(CONTENT_TYPE_1_ID_1, CONTRIB_ID_2, "label2");
		Tag tag3 = new Tag(CONTENT_TYPE_1_ID_2, CONTRIB_ID_2, "label3");

		em.getTransaction().begin();
		tested.createTag(tag1);
		tested.createTag(tag2);
		tested.createTag(tag3);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - no content specified
		tested.deleteTagsForContent((String[]) null);
		List<Tag> ret = tested.getTagsByContentType(CONTENT_TYPE_1);
		Assert.assertEquals(3, ret.size());
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - delete for content 1
		tested.deleteTagsForContent(CONTENT_TYPE_1_ID_1);
		ret = tested.getTagsByContentType(CONTENT_TYPE_1);
		Assert.assertEquals(1, ret.size());
		Tag testedTag = ret.get(0);
		Assert.assertEquals(CONTENT_TYPE_1_ID_2, testedTag.getContentId());
		Assert.assertEquals(CONTRIB_ID_2, testedTag.getContributorId());
		Assert.assertEquals("label3", testedTag.getTagLabel());

		em.getTransaction().commit();
	}

	protected JpaCustomTagPersistenceService getTested() {
		JpaCustomTagPersistenceService tested = new JpaCustomTagPersistenceService();
		tested.em = em;
		return tested;
	}
}
