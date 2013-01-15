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

		public static final String DCP_TYPE_KEY = "dcp_type";

		/**
		 * DCP Type Filtering - dcp_type field
		 */
		private List<String> dcpType;

		public static final String PROJECTS_KEY = "project";

		private String dcpContentProvider;

		public static final String DCP_CONTENT_PROVIDER = "content_provider";

		/**
		 * Filtering based on project
		 */
		private List<String> projects = null;

		/**
		 * Filtering based on tags
		 */
		private List<String> tags = null;

		public static final String TAGS_KEY = "tag";

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

		@Override
		public String toString() {
			return "Filters [contentType=" + contentType + ", dcpType=" + dcpType + ", dcpContentProvider="
					+ dcpContentProvider + ", projects=" + projects + ", tags=" + tags + ", start=" + start + ", count=" + count
					+ "]";
		}

		public void setProjects(List<String> projects) {
			this.projects = projects;
		}

		public void setStart(Integer start) {
			this.start = start;
		}

		public List<String> getProjects() {
			return projects;
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

		public List<String> getDcpType() {
			return dcpType;
		}

		public void setDcpType(List<String> dcpType) {
			this.dcpType = dcpType;
		}

		public String getDcpContentProvider() {
			return dcpContentProvider;
		}

		public void setDcpContentProvider(String dcpContentProvider) {
			this.dcpContentProvider = dcpContentProvider;
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

	@Override
	public String toString() {
		return "QuerySettings [query=" + query + ", sortBy=" + sortBy + ", filters=" + filters + "]";
	}

}
