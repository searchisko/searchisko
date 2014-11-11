/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

/**
 * Custom tag entity class. Class is JPA annotated.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
@Entity
public class Tag implements Serializable {

	@Id
	@GeneratedValue
	private long id;

	/**
	 * Id of the content this tag belogs to.
	 */
	@NotNull
	private String contentId;

	/**
	 * Id of the contributor who created the tag.
	 */
	@NotNull
	private String contributorId;

	/**
	 * Text representation of the tag.
	 */
	@NotNull
	private String tag;

	/**
	 * Timestamp when tag has been created last time.
	 */
	@NotNull
	private Timestamp createdAt;

	/**
	 * Basic constructor.
	 */
	public Tag() {
		super();
	}

	public Tag(String contentId, String contributorId, String tag, Timestamp createdAt) {
		this.contentId = contentId;
		this.contributorId = contributorId;
		this.tag = tag;
		this.createdAt = createdAt;
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

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
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
		if (id != other.id)
			return false;
		if (id == 0)
			return obj == this;
		return true;
	}

	@Override
	public String toString() {
		return "Tag{" + "id=" + id + ", contentId=" + contentId + ", contributorId=" + contributorId +
			", tag=" + tag + ", createdAt=" + createdAt + '}';
	}

}
