/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import java.util.List;

import org.jboss.dcp.api.util.QuerySettingsParser;

/**
 * Search Query settings sent from client to search method.
 * 
 * @author lvlcek@redhat.com
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class QuerySettings {

	public static class Filters {

		public static final String CONTENT_TYPE_KEY = "type";

		/**
		 * DCP content Type Filtering - dcp_content_type field
		 */
		private String contentType;

		public static final String PROJECTS_KEY = "project";

		/**
		 * Filtering based on project
		 */
		private List<String> projects = null;

		private String[] author = null;

		/**
		 * Filtering based on tags
		 */
		private List<String> tags = null;

		public static final String TAGS_KEY = "tag";

		private String from = null;
		private String to = null;

		/**
		 * Results Paging - start index
		 */
		private Integer start = null;

		public static final String START_KEY = "start";

		/**
		 * Results Paging - count of results
		 */
		private Integer count = null;

		public static final String COUNT_KEY = "count";

		public void setProjects(List<String> projects) {
			this.projects = projects;
		}

		public void setAuthor(String[] author) {
			this.author = author;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public void setStart(Integer start) {
			this.start = start;
		}

		public List<String> getProjects() {
			return projects;
		}

		public String[] getAuthor() {
			return author;
		}

		public String getFrom() {
			return from;
		}

		public String getTo() {
			return to;
		}

		public Integer getStart() {
			return start;
		}

		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

		public String getContentType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

	}

	private Filters filters;

	/**
	 * Fulltext query
	 */
	private String query;

	public static final String QUERY_KEY = "query";

	/**
	 * Sorting of results
	 */
	private SortByValue sortBy;

	public static final String SORT_BY_KEY = "sortBy";

	public enum SortByValue {
		/**
		 * Newest first
		 */
		NEW {
			@Override
			public String toString() {
				return "new";
			}
		},
		/**
		 * Oldest first
		 */
		OLD {
			@Override
			public String toString() {
				return "old";
			}
		}
	}

	public void setFilters(Filters filters) {
		this.filters = filters;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setSortBy(SortByValue sortBy) {
		this.sortBy = sortBy;
	}

	public Filters getFilters() {
		return filters;
	}

	/**
	 * Get search query
	 * 
	 * @return normalized query or null
	 * @see QuerySettingsParser#normalizeQueryString(String)
	 */
	public String getQuery() {
		return query;
	}

	public SortByValue getSortBy() {
		return sortBy;
	}

}
