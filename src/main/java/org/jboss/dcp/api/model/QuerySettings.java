/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import java.util.List;

import org.jboss.dcp.api.util.QuerySettingsParser;
import org.jboss.dcp.api.util.QuerySettingsParser.PastIntervalName;

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

		public static final String DCP_TYPES_KEY = "dcp_type";

		/**
		 * DCP Type Filtering - dcp_type field
		 */
		private List<String> dcpTypes;

		public static final String DCP_CONTENT_PROVIDER = "content_provider";

		private String dcpContentProvider;

		public static final String PROJECTS_KEY = "project";

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
		 * Filtering based on contributors
		 */
		private List<String> contributors = null;

		public static final String CONTRIBUTORS_KEY = "contributor";

		/**
		 * Filtering based on activity dates
		 */
		private PastIntervalName activityDateInterval;

		public static final String ACTIVITY_DATE_INTERVAL_KEY = "activity_date_interval";

		public static final String ACTIVITY_DATE_FROM_KEY = "activity_date_from";
		public static final String ACTIVITY_DATE_TO_KEY = "activity_date_to";
		private Long activityDateFrom;
		private Long activityDateTo;

		/**
		 * Results Paging - start index
		 */
		private Integer from = null;

		public static final String FROM_KEY = "from";

		/**
		 * Results Paging - count of returned results
		 */
		private Integer size = null;

		public static final String SIZE_KEY = "size";

		@Override
		public String toString() {
			return "Filters [contentType=" + contentType + ", dcpTypes=" + dcpTypes + ", dcpContentProvider="
					+ dcpContentProvider + ", projects=" + projects + ", tags=" + tags + ", contributors=" + contributors
					+ ", activityDateInterval=" + activityDateInterval + ", activityDateFrom=" + activityDateFrom
					+ ", activityDateTo=" + activityDateTo + ", from=" + from + ", size=" + size + "]";
		}

		public Long getActivityDateFrom() {
			return activityDateFrom;
		}

		public void setActivityDateFrom(Long activityDateFrom) {
			this.activityDateFrom = activityDateFrom;
		}

		public Long getActivityDateTo() {
			return activityDateTo;
		}

		public void setActivityDateTo(Long activityDateTo) {
			this.activityDateTo = activityDateTo;
		}

		public void setProjects(List<String> projects) {
			this.projects = projects;
		}

		public void setFrom(Integer from) {
			this.from = from;
		}

		public List<String> getProjects() {
			return projects;
		}

		public Integer getFrom() {
			return from;
		}

		public Integer getSize() {
			return size;
		}

		public void setSize(Integer size) {
			this.size = size;
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

		public List<String> getDcpTypes() {
			return dcpTypes;
		}

		public void setDcpTypes(List<String> dcpTypes) {
			this.dcpTypes = dcpTypes;
		}

		public String getDcpContentProvider() {
			return dcpContentProvider;
		}

		public void setDcpContentProvider(String dcpContentProvider) {
			this.dcpContentProvider = dcpContentProvider;
		}

		public List<String> getContributors() {
			return contributors;
		}

		public void setContributors(List<String> contributors) {
			this.contributors = contributors;
		}

		public PastIntervalName getActivityDateInterval() {
			return activityDateInterval;
		}

		public void setActivityDateInterval(PastIntervalName activityDateInterval) {
			this.activityDateInterval = activityDateInterval;
		}

	}

	private Filters filters;

	/**
	 * Fulltext query
	 */
	private String query;

	public static final String QUERY_KEY = "query";

	/**
	 * List of fields in response
	 */
	private List<String> fields;

	public static final String FIELDS_KEY = "field";

	/**
	 * Sorting of results
	 */
	private SortByValue sortBy;

	public static final String SORT_BY_KEY = "sortBy";

	public static enum SortByValue {
		/**
		 * Newest first
		 */
		NEW("new"),
		/**
		 * Oldest first
		 */
		OLD("old");

		/**
		 * Value used in request parameter
		 */
		private String value;

		private SortByValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
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

	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	@Override
	public String toString() {
		return "QuerySettings [filters=" + filters + ", query=" + query + ", fields=" + fields + ", sortBy=" + sortBy + "]";
	}

}
