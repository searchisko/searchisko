/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.io.Serializable;

/**
 * Content Tuple used to store id and content for some entity.
 * 
 * @param <X> type of id
 * @param <Y> type of content
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentTuple<X, Y> implements Serializable {

	private X id;
	private Y content;

	/**
	 * Create Tuple with id and content.
	 * 
	 * @param id to store in Tuple
	 * @param content to store in Tuple
	 * @throws IllegalArgumentException if id is null
	 */
	public ContentTuple(X id, Y content) throws IllegalArgumentException {
		if (id == null)
			throw new IllegalArgumentException("id can't be null");
		this.id = id;
		this.content = content;
	}

	public X getId() {
		return id;
	}

	public Y getContent() {
		return content;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ContentTuple<?, ?> other = (ContentTuple<?, ?>) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ContentTuple [id=" + id + ", content='" + content + "']";
	}

}
