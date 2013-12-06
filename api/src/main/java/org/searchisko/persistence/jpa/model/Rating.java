/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;

/**
 * Personalized content rating entity class.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Entity
public class Rating implements Serializable {

	// TODO _RATING JPA entity mapping, equals and toString

	/**
	 * Id of content this rating is for.
	 */
	private String contentId;

	/**
	 * Id og contributor who rated.
	 */
	private String contributorId;

	/**
	 * Rating value - number between 1 and 5.
	 */
	private int rating;

	/**
	 * Timestamp when rating has been updated last time.
	 */
	private Timestamp ratedAt;

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

	@Override
	public String toString() {
		return "Rating [contentId=" + contentId + ", contributorId=" + contributorId + ", rating=" + rating + ", ratedAt="
				+ ratedAt + "]";
	}

}
