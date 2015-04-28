/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tagLabel. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Comparator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

/**
 * Custom tagLabel entity class. Class is JPA annotated.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
@Entity
public class Tag implements Serializable, Comparable {

	@Id
	@GeneratedValue
	private long id;

	/**
	 * Id of the content this tagLabel belogs to.
	 */
	@NotNull
	private String contentId;

	/**
	 * Id of the contributor who created the tagLabel.
	 */
	@NotNull
	private String contributorId;

	/**
	 * Text representation of the tagLabel.
	 */
	@NotNull
	private String tagLabel;

	/**
	 * Timestamp when tagLabel has been created last time.
	 */
	@NotNull
	private Timestamp createdAt;

	/**
	 * Basic constructor.
	 */
	public Tag() {
		super();
	}

	public Tag(String contentId, String contributorId, String tagLabel) {
		this.contentId = contentId;
		this.contributorId = contributorId;
		this.tagLabel = tagLabel;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getContributorId() {
		return contributorId;
	}

	public void setContributorId(String contributorId) {
		this.contributorId = contributorId;
	}

	public String getTagLabel() {
		return tagLabel;
	}

	public void setTagLabel(String tagLabel) {
		this.tagLabel = tagLabel;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 67 * hash + (int) (this.id ^ (this.id >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tag other = (Tag) obj;

		if (getId() != other.getId()) {
			return false;
		}

		return true;
	}

	@Override
	public int compareTo(Object o) {
		if (o == null)
			return 1;
		if (getClass() != o.getClass())
			return 1;
		Tag other = (Tag) o;

		if (getId() == other.getId()) {
			return 0;
		}

		if (getId() > other.getId()) {
			return 1;
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "Tag{" + "id=" + id + ", contentId=" + contentId + ", contributorId=" + contributorId +
			", tag=" + tagLabel + ", createdAt=" + createdAt + '}';
	}

}
