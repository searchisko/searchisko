/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.annotations.header.CORSSupport;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.service.SearchService;
import org.jboss.dcp.api.service.StatsRecordType;
import org.jboss.dcp.api.service.SystemInfoService;
import org.jboss.dcp.api.util.QuerySettingsParser;
import org.jboss.dcp.api.util.SearchUtils;
import org.jboss.resteasy.plugins.providers.atom.Category;
import org.jboss.resteasy.plugins.providers.atom.Content;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.plugins.providers.atom.Generator;
import org.jboss.resteasy.plugins.providers.atom.Link;
import org.jboss.resteasy.plugins.providers.atom.Person;

/**
 * Feed REST API.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@RequestScoped
@Path("/feed")
@Produces(MediaType.APPLICATION_ATOM_XML)
public class FeedRestService extends RestServiceBase {

	@Inject
	protected SystemInfoService systemInfoService;

	protected static final String REQPARAM_FEED_TITLE = "feed_title";

	private static URI TAG_SCHEME_URI = null;
	static {
		try {
			TAG_SCHEME_URI = new URI("dcp:content:tags");
		} catch (URISyntaxException e) {
			// OK
		}
	}

	@Inject
	protected SearchService searchService;

	@Inject
	protected QuerySettingsParser querySettingsParser;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_ATOM_XML)
	@GuestAllowed
	@CORSSupport
	public Object feed(@Context UriInfo uriInfo) {

		QuerySettings querySettings = null;
		try {

			if (uriInfo == null) {
				throw new Exception("Incorrect call, uriInfo param is null");
			}
			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			querySettings = querySettingsParser.parseUriParams(params);

			patchQuerySettings(querySettings);

			String responseUuid = UUID.randomUUID().toString();

			SearchResponse searchResponse = searchService.performSearch(querySettings, responseUuid, StatsRecordType.FEED);

			return createAtomResponse(querySettings, searchResponse, uriInfo);
		} catch (IllegalArgumentException e) {
			return createBadFieldDataResponse(e.getMessage());
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	/**
	 * Patch query settings to search correct values necessary for feeds.
	 * 
	 * @param querySettings
	 */
	protected void patchQuerySettings(QuerySettings querySettings) {
		querySettings.clearFacets();
		querySettings.setQueryHighlight(false);
		querySettings.clearFields();

		querySettings.addField(DcpContentObjectFields.DCP_TITLE);
		querySettings.addField(DcpContentObjectFields.DCP_DESCRIPTION);
		querySettings.addField(DcpContentObjectFields.DCP_URL_VIEW);
		querySettings.addField(DcpContentObjectFields.DCP_CREATED);
		querySettings.addField(DcpContentObjectFields.DCP_LAST_ACTIVITY_DATE);
		querySettings.addField(DcpContentObjectFields.DCP_CONTENT);
		querySettings.addField(DcpContentObjectFields.DCP_CONTENT_CONTENT_TYPE);
		querySettings.addField(DcpContentObjectFields.DCP_TAGS);
		querySettings.addField(DcpContentObjectFields.DCP_CONTRIBUTORS);

		if (querySettings.getSortBy() != SortByValue.NEW_CREATION) {
			querySettings.setSortBy(SortByValue.NEW);
		}
		Filters filters = querySettings.getFiltersInit();
		filters.setFrom(0);
		filters.setSize(20);
		filters.setActivityDateFrom(null);
		filters.setActivityDateTo(null);
		filters.setActivityDateInterval(null);
	}

	protected Feed createAtomResponse(final QuerySettings querySettings, final SearchResponse searchResponse,
			final UriInfo uriInfo) throws URISyntaxException {
		Feed feed = new Feed();
		feed.setId(uriInfo.getRequestUri());
		String title = SearchUtils.trimToNull(uriInfo.getQueryParameters().getFirst(REQPARAM_FEED_TITLE));
		if (title == null)
			title = constructFeedTitle(querySettings);
		feed.setTitle(title);
		Generator generator = new Generator();
		generator.setText("DCP");
		generator.setVersion(systemInfoService.getVersion());
		feed.setGenerator(generator);
		feed.setUpdated(new Date());
		if (searchResponse.getHits().getHits() != null) {
			for (SearchHit hit : searchResponse.getHits().getHits()) {
				Entry entry = new Entry();
				entry.setId(new URI("dcp:content:id:" + hit.getId()));
				entry.setPublished(getHitDateFieldValue(hit, DcpContentObjectFields.DCP_CREATED));
				entry.setUpdated(getHitDateFieldValue(hit, DcpContentObjectFields.DCP_LAST_ACTIVITY_DATE));
				entry.setTitle(getHitStringFieldValue(hit, DcpContentObjectFields.DCP_TITLE));
				String url = getHitStringFieldValue(hit, DcpContentObjectFields.DCP_URL_VIEW);
				if (url != null)
					entry.getLinks().add(new Link(null, url));
				String contentValue = getHitStringFieldValue(hit, DcpContentObjectFields.DCP_CONTENT);
				String descriptionValue = getHitStringFieldValue(hit, DcpContentObjectFields.DCP_DESCRIPTION);
				if (contentValue != null) {
					Content content = new Content();
					try {
						content.setType(MediaType.valueOf(getHitStringFieldValue(hit, DcpContentObjectFields.DCP_CONTENT_TYPE)));
					} catch (IllegalArgumentException e) {
						content.setType(MediaType.TEXT_PLAIN_TYPE);
					}
					content.setText(contentValue);
					entry.setContent(content);
					if (descriptionValue != null)
						entry.setSummary(descriptionValue);
				} else if (descriptionValue != null) {
					Content content = new Content();
					content.setType(MediaType.TEXT_PLAIN_TYPE);
					content.setText(descriptionValue);
					entry.setContent(content);
				}

				List<Object> contributors = getHitListFieldValue(hit, DcpContentObjectFields.DCP_CONTRIBUTORS);
				if (contributors != null) {
					for (Object contributor : contributors) {
						Person p = new Person(SearchUtils.extractContributorName(contributor.toString()));
						entry.getAuthors().add(p);
					}
				}
				// ATOM spec requires at least one author
				if (entry.getAuthors().isEmpty()) {
					entry.getAuthors().add(new Person("unknown"));
				}

				List<Object> tags = getHitListFieldValue(hit, DcpContentObjectFields.DCP_TAGS);
				if (tags != null) {
					for (Object tag : tags) {
						Category c = new Category();
						c.setTerm(tag.toString());
						c.setScheme(TAG_SCHEME_URI);
						entry.getCategories().add(c);
					}
				}
				feed.getEntries().add(entry);
			}
		}
		return feed;
	}

	protected String constructFeedTitle(QuerySettings querySettings) {
		StringBuilder sb = new StringBuilder();

		Filters f = querySettings.getFiltersInit();

		appendParamIfExists(sb, "project", f.getProjects());
		appendParamIfExists(sb, "contributor", f.getContributors());
		appendParamIfExists(sb, "tag", f.getTags());
		appendParamIfExists(sb, "dcp_type", f.getDcpTypes());
		appendParamIfExists(sb, "type", f.getContentType());
		appendParamIfExists(sb, "content_provider", f.getDcpContentProvider());
		appendParamIfExists(sb, "query", querySettings.getQuery(), true);
		if (querySettings.getSortBy() != null && querySettings.getSortBy() != SortByValue.NEW)
			appendParamIfExists(sb, "sortBy", querySettings.getSortBy().toString());
		if (sb.length() == 0)
			return "DCP whole content feed";
		else
			return "DCP content feed for criteria " + sb.toString();
	}

	private void appendParamIfExists(StringBuilder sb, String paramName, String value) {
		appendParamIfExists(sb, paramName, value, false);
	}

	private void appendParamIfExists(StringBuilder sb, String paramName, String value, boolean quote) {
		if (value != null) {
			if (sb.length() > 0)
				sb.append(" and ");
			sb.append(paramName).append("=");
			if (quote)
				sb.append("'");
			sb.append(value);
			if (quote)
				sb.append("'");
		}
	}

	private void appendParamIfExists(StringBuilder sb, String paramName, List<?> value) {
		if (value != null) {
			appendParamIfExists(sb, paramName, value.toString(), false);
		}
	}

	protected String getHitStringFieldValue(SearchHit hit, String fieldName) {
		if (hit.getFields().containsKey(fieldName))
			return SearchUtils.trimToNull((String) hit.getFields().get(fieldName).getValue());
		else
			return null;
	}

	protected List<Object> getHitListFieldValue(SearchHit hit, String fieldName) {
		if (hit.getFields().containsKey(fieldName))
			return (List<Object>) hit.getFields().get(fieldName).getValues();
		else
			return null;
	}

	protected Date getHitDateFieldValue(SearchHit hit, String fieldName) {
		if (hit.getFields().containsKey(fieldName)) {
			Object o = SearchUtils.trimToNull((String) hit.getFields().get(fieldName).getValue());
			if (o instanceof Date) {
				return (Date) o;
			}
			if (o != null) {
				return SearchUtils.dateFromISOString(o.toString(), true);
			}
		}
		return null;
	}

}
