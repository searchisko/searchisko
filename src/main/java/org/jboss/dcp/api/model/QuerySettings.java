/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import java.util.List;

import org.jboss.dcp.api.util.QuerySettingsParser;

/**
 * Query settings sent by ajax client to servlet proxy.
 * 
 * @author lvlcek@redhat.com
 * @author Libor Krzyzanek
 */
public class QuerySettings {

	public static class Filters {

		private String[] project = null;
		private String[] author = null;
		private String[] mailList = null;

		/**
		 * Filtering based on tags
		 */
		private List<String> tags = null;

		public static final String TAGS_KEY = "filters[tag]";

		private String from = null;
		private String to = null;

		/**
		 * Paging - start
		 */
		private Integer start = null;

		public static final String START_KEY = "filters[start]";

		/**
		 * Paging - count
		 */
		private Integer count = null;

		public static final String COUNT_KEY = "filters[count]";

		private String past = null;

		public void setProject(String[] project) {
			this.project = project;
		}

		public void setAuthor(String[] author) {
			this.author = author;
		}

		public void setMailList(String[] mailList) {
			this.mailList = mailList;
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

		public void setPast(String past) {
			this.past = past;
		}

		public String[] getProject() {
			return project;
		}

		public String[] getAuthor() {
			return author;
		}

		public String[] getMailList() {
			return mailList;
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

		public String getPast() {
			return past;
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

	}

	private boolean count = false;
	private Filters filters;

	/**
	 * Fulltext query
	 */
	private String query;

	public static final String QUERY_KEY = "query";

	private String interval;

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

	/**
	 * DCP content Type
	 */
	private String contentType;

	public void setCount(boolean value) {
		this.count = value;
	}

	public void setFilters(Filters filters) {
		this.filters = filters;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setInterval(String interval) {
		this.interval = interval;
	}

	public void setSortBy(SortByValue sortBy) {
		this.sortBy = sortBy;
	}

	public boolean getCount() {
		return this.count;
	}

	public Filters getFilters() {
		return filters;
	}

	/**
	 * Get serach query
	 * 
	 * @return normalized query or null
	 * @see QuerySettingsParser#normalizeQueryString(String)
	 */
	public String getQuery() {
		return query;
	}

	public String getInterval() {
		return interval;
	}

	public SortByValue getSortBy() {
		return sortBy;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}
