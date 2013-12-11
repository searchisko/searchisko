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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * Personalized content rating entity class. Class is JPA annotated.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "contentId", "contributorId" }))
public class Rating implements Serializable {

	@Id
	@GeneratedValue
	private long id;

	/**
	 * Id of content this rating is for.
	 */
	@NotNull
	private String contentId;

	/**
	 * Id og contributor who rated.
	 */
	@NotNull
	private String contributorId;

	/**
	 * Rating value - number between 1 and 5.
	 */
	@NotNull
	private int rating;

	/**
	 * Timestamp when rating has been updated last time.
	 */
	@NotNull
	private Timestamp ratedAt;

	/**
	 * Basic constructor.
	 */
	public Rating() {
		super();
	}

	public Rating(String contentId, String contributorId, int rating, Timestamp ratedAt) {
		super();
		this.contentId = contentId;
		this.contributorId = contributorId;
		this.rating = rating;
		this.ratedAt = ratedAt;
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

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public Timestamp getRatedAt() {
		return ratedAt;
	}

	public void setRatedAt(Timestamp ratedAt) {
		this.ratedAt = ratedAt;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rating other = (Rating) obj;
		if (id != other.id)
			return false;
		if (id == 0)
			return obj == this;
		return true;
	}

	@Override
	public String toString() {
		return "Rating [id=" + id + ", contentId=" + contentId + ", contributorId=" + contributorId + ", rating=" + rating
				+ ", ratedAt=" + ratedAt + "]";
	}

}
