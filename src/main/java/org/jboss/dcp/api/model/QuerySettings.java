/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

/**
 * Query settings sent by ajax client to servlet proxy.
 * 
 * @author lvlcek@redhat.com
 */
public class QuerySettings {

	public static class Filters {

		private String[] project = null;
		private String[] author = null;
		private String[] mailList = null;
		private String from = null;
		private String to = null;
		private String start = null;
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

		public void setStart(String start) {
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

		public String getStart() {
			return start;
		}

		public String getPast() {
			return past;
		}

	}

	private boolean count = false;
	private Filters filters;
	private String query;
	private String interval;
	private String sortBy;

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

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public boolean getCount() {
		return this.count;
	}

	public Filters getFilters() {
		return filters;
	}

	public String getQuery() {
		return query;
	}

	public String getInterval() {
		return interval;
	}

	public String getSortBy() {
		return sortBy;
	}

}
