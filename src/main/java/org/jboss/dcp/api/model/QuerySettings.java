/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.dcp.api.util.QuerySettingsParser;

/**
 * Search Query settings sent from client to search method.
 * 
 * @author Lukas Vlcek
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
		private PastIntervalValue activityDateInterval;

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

		public void addProject(String project) {
			if (projects == null)
				projects = new ArrayList<String>();
			projects.add(project);
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

		public void addTag(String tag) {
			if (tags == null)
				tags = new ArrayList<String>();
			tags.add(tag);
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

		public void addDcpType(String dcpType) {
			if (dcpTypes == null)
				dcpTypes = new ArrayList<String>();
			dcpTypes.add(dcpType);
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

		public void addContributor(String contributor) {
			if (contributors == null) {
				contributors = new ArrayList<String>();
			}
			contributors.add(contributor);
		}

		public PastIntervalValue getActivityDateInterval() {
			return activityDateInterval;
		}

		public void setActivityDateInterval(PastIntervalValue activityDateInterval) {
			this.activityDateInterval = activityDateInterval;
		}

	}

	private Filters filters;

	/**
	 * Fulltext query
	 */
	private String query;

	public static final String QUERY_KEY = "query";

	private boolean queryHighlight = false;

	public static final String QUERY_HIGHLIGHT_KEY = "query_highlight";

	/**
	 * List of fields in response
	 */
	private List<String> fields;

	public static final String FIELDS_KEY = "field";

	private Set<FacetValue> facets;

	public static final String FACETS_KEY = "facet";

	/**
	 * Sorting of results
	 */
	private SortByValue sortBy;

	public static final String SORT_BY_KEY = "sortBy";

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

	public void addField(String value) {
		if (value == null)
			return;
		if (fields == null)
			fields = new ArrayList<String>();
		fields.add(value);
	}

	public boolean isQueryHighlight() {
		return queryHighlight;
	}

	public void setQueryHighlight(boolean queryHighlight) {
		this.queryHighlight = queryHighlight;
	}

	public Set<FacetValue> getFacets() {
		return facets;
	}

	public void addFacet(FacetValue value) {
		if (value == null)
			return;
		if (facets == null)
			facets = new LinkedHashSet<FacetValue>();
		facets.add(value);
	}

	@Override
	public String toString() {
		return "QuerySettings [filters=" + filters + ", query=" + query + ", queryHighlight=" + queryHighlight + ", field="
				+ fields + ", facet=" + facets + ", sortBy=" + sortBy + "]";
	}

}
