/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.security.PermitAll;
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
import org.jboss.resteasy.plugins.providers.atom.Category;
import org.jboss.resteasy.plugins.providers.atom.Content;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.plugins.providers.atom.Generator;
import org.jboss.resteasy.plugins.providers.atom.Link;
import org.jboss.resteasy.plugins.providers.atom.Person;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.QuerySettings.Filters;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.service.SearchService;
import org.searchisko.api.service.StatsRecordType;
import org.searchisko.api.service.SystemInfoService;
import org.searchisko.api.util.QuerySettingsParser;
import org.searchisko.api.util.SearchUtils;

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
			TAG_SCHEME_URI = new URI("searchisko:content:tags");
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
	@PermitAll
	public Object feed(@Context UriInfo uriInfo) throws URISyntaxException {

		QuerySettings querySettings = null;
		try {

			if (uriInfo == null) {
				throw new BadFieldException("uriInfo");
			}
			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			querySettings = querySettingsParser.parseUriParams(params);

			patchQuerySettings(querySettings);

			String responseUuid = UUID.randomUUID().toString();

			SearchResponse searchResponse = searchService.performSearch(querySettings, responseUuid, StatsRecordType.FEED);

			return createAtomResponse(querySettings, searchResponse, uriInfo);
		} catch (IllegalArgumentException e) {
			throw new BadFieldException("unknown", e);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	/**
	 * Patch query settings to search correct values necessary for feeds.
	 * 
	 * @param querySettings
	 */
	protected void patchQuerySettings(QuerySettings querySettings) {
		querySettings.clearAggregations();
		querySettings.setQueryHighlight(false);
		querySettings.clearFields();

		querySettings.addField(ContentObjectFields.SYS_TITLE);
		querySettings.addField(ContentObjectFields.SYS_DESCRIPTION);
		querySettings.addField(ContentObjectFields.SYS_URL_VIEW);
		querySettings.addField(ContentObjectFields.SYS_CREATED);
		querySettings.addField(ContentObjectFields.SYS_LAST_ACTIVITY_DATE);
		querySettings.addField(ContentObjectFields.SYS_CONTENT);
		querySettings.addField(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE);
		querySettings.addField(ContentObjectFields.SYS_TAGS);
		querySettings.addField(ContentObjectFields.SYS_CONTRIBUTORS);

		if (querySettings.getSortBy() != SortByValue.NEW_CREATION) {
			querySettings.setSortBy(SortByValue.NEW);
		}

		if (querySettings.getFrom() == null)
			querySettings.setFrom(0);
		if (querySettings.getSize() == null)
			querySettings.setSize(20);
		Filters filters = querySettings.getFiltersInit();
		// TODO: no hardcodes, we could probably remove these
		filters.forgetUrlFilterCandidate("activity_date_from");
		filters.forgetUrlFilterCandidate("activity_date_to");
		filters.forgetUrlFilterCandidate("activity_date_interval");
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
		generator.setText("Searchisko");
		generator.setVersion(systemInfoService.getVersion());
		feed.setGenerator(generator);
		feed.setUpdated(new Date());
		if (searchResponse.getHits().getHits() != null) {
			for (SearchHit hit : searchResponse.getHits().getHits()) {
				Entry entry = new Entry();
				entry.setId(new URI("searchisko:content:id:" + hit.getId()));
				entry.setPublished(getHitDateFieldValue(hit, ContentObjectFields.SYS_CREATED));
				entry.setUpdated(getHitDateFieldValue(hit, ContentObjectFields.SYS_LAST_ACTIVITY_DATE));
				entry.setTitle(getHitStringFieldValue(hit, ContentObjectFields.SYS_TITLE));
				String url = getHitStringFieldValue(hit, ContentObjectFields.SYS_URL_VIEW);
				if (url != null)
					entry.getLinks().add(new Link(null, url));
				String contentValue = getHitStringFieldValue(hit, ContentObjectFields.SYS_CONTENT);
				String descriptionValue = getHitStringFieldValue(hit, ContentObjectFields.SYS_DESCRIPTION);
				if (contentValue != null) {
					Content content = new Content();
					try {
						content.setType(MediaType
								.valueOf(getHitStringFieldValue(hit, ContentObjectFields.SYS_CONTENT_CONTENT_TYPE)));
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

				List<Object> contributors = getHitListFieldValue(hit, ContentObjectFields.SYS_CONTRIBUTORS);
				if (contributors != null) {
					for (Object contributor : contributors) {
						Person p = new Person(ContributorService.extractContributorName(contributor.toString()));
						entry.getAuthors().add(p);
					}
				}
				// ATOM spec requires at least one author
				if (entry.getAuthors().isEmpty()) {
					entry.getAuthors().add(new Person("unknown"));
				}

				List<Object> tags = getHitListFieldValue(hit, ContentObjectFields.SYS_TAGS);
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

		appendParamIfExists(sb, "project", f.getFilterCandidateValues("project"));
		appendParamIfExists(sb, "contributor", f.getFilterCandidateValues("contributor"));
		appendParamIfExists(sb, "tag", f.getFilterCandidateValues("tag"));
		appendParamIfExists(sb, "sys_type", f.getFilterCandidateValues("sys_type"));
		appendParamIfExists(sb, "type", f.getFilterCandidateValues("type"));
		appendParamIfExists(sb, "content_provider", f.getFilterCandidateValues("content_provider"));
		appendParamIfExists(sb, "query", querySettings.getQuery(), true);
		if (querySettings.getSortBy() != null && querySettings.getSortBy() != SortByValue.NEW)
			appendParamIfExists(sb, "sortBy", querySettings.getSortBy().toString());
		if (sb.length() == 0)
			return "Whole feed content";
		else
			return "Feed content for criteria " + sb.toString();
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
			return hit.getFields().get(fieldName).getValues();
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
