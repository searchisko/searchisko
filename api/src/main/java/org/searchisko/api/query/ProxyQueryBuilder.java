/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryFilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.searchisko.api.model.QuerySettings;

/**
 * @author lvlcek@redhat.com
 * @deprecated
 */
public class ProxyQueryBuilder {

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(ProxyQueryBuilder.class.getName());

	@SuppressWarnings("unused")
	public static SearchSourceBuilder buildSearchQuery(QuerySettings settings) throws Exception {

		// QuerySettingsParser.sanityQuery(settings);

		// User query string
		QueryBuilder qb = null;
		QueryBuilder qb_wth_fields = null;
		if (settings.getQuery().trim().length() > 0 && !"*".equals(settings.getQuery()) && !"?".equals(settings.getQuery())) {
			qb = QueryBuilders.queryString(settings.getQuery());
			qb_wth_fields = QueryBuilders.queryString(settings.getQuery()).tieBreaker(5).field("subject", 6)
					.field("first_text_message_without_quotes", 8).field("first_text_message", 4).field("first_html_message", 4)
					.field("text_messages", 2).field("html_messages", 2).field("message_attachments").field("author.author")
					.field("author.not_analyzed");
		} else {
			qb = QueryBuilders.matchAllQuery();
			qb_wth_fields = qb;
		}

		// Create filters
		Map<String, FilterBuilder> queryFilters = new HashMap<String, FilterBuilder>();
		if (settings.getFilters() != null) {
			QuerySettings.Filters f = settings.getFilters();

			// if (f.getAuthor() != null) {
			// queryFilters.put("author.not_analyzed", new TermsFilterBuilder("author.not_analyzed", f.getAuthor()));
			// }
			// if (f.getMailList() != null) {
			// queryFilters.put("mailList",new TermsFilterBuilder("mail_list", f.getMailList()));
			// }
			// if (f.getProject() != null) {
			// queryFilters.put("project",new TermsFilterBuilder("_index", f.getProject()));
			// }
			// if (f.getFrom() != null || f.getTo() != null) {
			// RangeFilterBuilder range = new RangeFilterBuilder("date");
			// if (f.getFrom() != null) {
			// range.from(f.getFrom()).includeLower(true);
			// }
			// if (f.getTo() != null) {
			// range.to(f.getTo()).includeUpper(true);
			// }
			// queryFilters.put("range", range);
			// }
			// Apply "past" filter only if explicit from/to filters were not used.
			// (i.e. from/to filters always DO OVERRIDE past filter)
			// else if (f.getPast() != null) {
			// try {
			// RangeFilterBuilder range = new RangeFilterBuilder("date");
			// QuerySettingsParser.PastIntervalNames past = QuerySettingsParser.PastIntervalNames.valueOf(f.getPast()
			// .toUpperCase());
			// long now = System.currentTimeMillis();
			// Long from = null;
			// switch (past) {
			// case WEEK:
			// from = now - (7L * 24L * 60L * 60L * 1000L);
			// break;
			// case MONTH:
			// from = now - (31L * 24L * 60L * 60L * 1000L);
			// break;
			// case QUARTER:
			// from = now - (3L * 31L * 24L * 60L * 60L * 1000L);
			// break;
			// case YEAR:
			// from = now - (365L * 24L * 60L * 60L * 1000L);
			// break;
			// }
			// if (from != null) {
			// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			// range.from(format.format(new Date(from))).includeLower(true);
			// queryFilters.put("range", range);
			// }
			// } catch (Exception e) {
			// log.log(Level.FINE, "Failed to apply filters[past] parameter value [{0}] Ignoring it. {1}",
			// new Object[] { f.getPast(), e });
			// }
			// }
		}

		if (queryFilters.size() > 0) {
			AndFilterBuilder and = new AndFilterBuilder(getFilters(queryFilters,
					queryFilters.keySet().toArray(new String[queryFilters.keySet().size()])));
			queryFilters.put("query", new QueryFilterBuilder(qb_wth_fields));
			qb_wth_fields = new FilteredQueryBuilder(qb_wth_fields, and);
		}

		SearchSourceBuilder b = new SearchSourceBuilder();
		b.query(qb_wth_fields);

		if (settings.getFilters() != null && settings.getFrom() != null) {
			int from = settings.getFrom();
			if (from > 0) {
				b.from(from);
			}
		} else {
			b.from(0);
		}

		// Fields
		b.fields("date", "document_url", "author", "mail_list", "message_id", "message_snippet", "subject",
				"subject_original", "to", "in_reply_to", "references");

		// Sorting
		/*
		 * String sort = settings.getSortBy(); if (sort != null) { if ("new".equalsIgnoreCase(sort)) {
		 * b.sort(SortBuilders.fieldSort("date").order(SortOrder.DESC)); } else if ("old".equalsIgnoreCase(sort)) {
		 * b.sort(SortBuilders.fieldSort("date").order(SortOrder.ASC)); } else { b.sort(SortBuilders.scoreSort()); } } else
		 * { b.sort(SortBuilders.scoreSort()); }
		 */

		// Highlights
		b.highlight(b.highlighter().preTags("<span class='hlt'>").postTags("</span>")
				.field("first_text_message", -1, 3, 20).field("first_html_message", -1, 3, 20)
				.field("message_attachments", -1, 3, 20).field("subject", -1, 0));

		// Facets
		// b.facet(new DateHistogramFacetBuilder("histogram").field("date").interval(
		// settings.getInterval() != null ? settings.getInterval() : "month"));

		if (!queryFilters.isEmpty()) {
			b.facet(new TermsFacetBuilder("projects")
					.field("_index")
					.size(300)
					.global(true)
					.facetFilter(
							new AndFilterBuilder(getFilters(queryFilters, "query", "range", "mailList", "author.not_analyzed"))));
		} else {
			b.facet(new TermsFacetBuilder("projects").field("_index").size(300).global(true)
					.facetFilter(new QueryFilterBuilder(qb_wth_fields)));
		}

		if (!queryFilters.isEmpty()) {
			b.facet(new TermsFacetBuilder("listType")
					.field("mail_list")
					.size(10)
					.global(true)
					.facetFilter(
							new AndFilterBuilder(getFilters(queryFilters, "query", "range", "project", "author.not_analyzed"))));
		} else {
			b.facet(new TermsFacetBuilder("listType").field("mail_list").size(10).global(true)
					.facetFilter(new QueryFilterBuilder(qb_wth_fields)));
		}

		if (!queryFilters.isEmpty()) {
			b.facet(new TermsFacetBuilder("author").field("author.not_analyzed").size(150).global(true)
					.facetFilter(new AndFilterBuilder(getFilters(queryFilters, "query", "range", "project", "mailList"))));
			// if (settings.getFilters().getAuthor() != null) {
			// b.facet(new TermsFacetBuilder("author_filter")
			// .field("author.not_analyzed")
			// .size(30)
			// .global(true)
			// .facetFilter(
			// new AndFilterBuilder(getFilters(queryFilters, "query", "author.not_analyzed", "range", "project",
			// "mailList"))));
			// }
		} else {
			b.facet(new TermsFacetBuilder("author").field("author.not_analyzed").size(150).global(true)
					.facetFilter(new QueryFilterBuilder(qb_wth_fields)));
		}

		return b;
	}

	private static FilterBuilder[] getFilters(Map<String, FilterBuilder> filters, String... names) {
		List<FilterBuilder> builders = new ArrayList<FilterBuilder>();
		for (String name : names) {
			if (filters.containsKey(name)) {
				builders.add(filters.get(name));
			}
		}
		return builders.toArray(new FilterBuilder[builders.size()]);
	}

	public static SearchRequestBuilder prepareDocumentDetailQuery(SearchRequestBuilder builder, String documentId,
			String userQuery) {
		builder
				.addFields("message_id", "message_id_original", "subject_original", "subject", "date", "author",
						"first_text_message", "first_html_message", "references", "project", "mail_list", "document_url")
				.setQuery(
						QueryBuilders.boolQuery().should(QueryBuilders.queryString(userQuery))
								.must(QueryBuilders.termQuery("message_id", documentId)))
				.setFilter(FilterBuilders.idsFilter("mail").ids(documentId)).setHighlighterPreTags("<span class='phlt'>")
				.setHighlighterPostTags("</span>").addHighlightedField("subject_original", -1, 0)
				.addHighlightedField("first_text_message", -1, 0).addHighlightedField("first_html_message", -1, 0)
				.addHighlightedField("message_attachments", -1, 3);
		return builder;
	}

	public static SearchRequestBuilder prepareConversationThreadForReferencesQuery(int count,
			SearchRequestBuilder builder, String[] references, String project, String mail_list) {
		builder
				.setFrom(0)
				.setSize(count)
				.addFields("date", "subject", "mail_list", "message_id", "message_id_original", "author.not_analyzed",
						"references")
				.addScriptField("millis", "doc['date'].date.millis")
				.setQuery(
						QueryBuilders.constantScoreQuery(FilterBuilders
								.boolFilter()
								.must(
										FilterBuilders.orFilter(FilterBuilders.termsFilter("references", references),
												FilterBuilders.termsFilter("message_id_original", references)))
								.must(FilterBuilders.queryFilter(QueryBuilders.termQuery("project", project)))
								.must(FilterBuilders.queryFilter(QueryBuilders.termQuery("mail_list", mail_list)))));
		return builder;
	}

	public static SearchRequestBuilder prepareConversationThreadForReferencesQuery(int count,
			SearchRequestBuilder builder, String[] references) {
		builder
				.setFrom(0)
				.setSize(count)
				.addFields("date", "subject", "mail_list", "message_id", "message_id_original", "author.not_analyzed",
						"references")
				.addScriptField("millis", "doc['date'].date.millis")
				.setQuery(
						QueryBuilders.constantScoreQuery(FilterBuilders.orFilter(
								FilterBuilders.termsFilter("references", references),
								FilterBuilders.termsFilter("message_id_original", references))));
		return builder;
	}

	public static SearchRequestBuilder countConversationThreadForReferencesQuery(SearchRequestBuilder builder,
			String[] references, String project, String mail_list) {
		builder.setQuery(QueryBuilders.constantScoreQuery(FilterBuilders
				.boolFilter()
				.must(
						FilterBuilders.orFilter(FilterBuilders.termsFilter("references", references),
								FilterBuilders.termsFilter("message_id_original", references)))
				.must(FilterBuilders.queryFilter(QueryBuilders.termQuery("project", project)))
				.must(FilterBuilders.queryFilter(QueryBuilders.termQuery("mail_list", mail_list)))));
		return builder;
	}

	public static SearchRequestBuilder countConversationThreadForReferencesQuery(SearchRequestBuilder builder,
			String[] references) {
		builder.setQuery(QueryBuilders.constantScoreQuery(FilterBuilders.orFilter(
				FilterBuilders.termsFilter("references", references),
				FilterBuilders.termsFilter("message_id_original", references))));
		return builder;
	}

	public static SearchRequestBuilder prepareConversationThreadForDocumentSubjectQuery(int count,
			SearchRequestBuilder builder, String subjectPattern, String project, String mail_list) {
		prepareConversationThreadForDocumentSubjectQuery(count, builder, subjectPattern).setFilter(
				FilterBuilders.andFilter(FilterBuilders.termsFilter("project", project),
						FilterBuilders.termsFilter("mail_list", mail_list)));
		return builder;
	}

	public static SearchRequestBuilder prepareConversationThreadForDocumentSubjectQuery(int count,
			SearchRequestBuilder builder, String subjectPattern) {
		builder
				.setFrom(0)
				.setSize(count)
				.addFields("date", "subject", "mail_list", "message_id", "message_id_original", "author.not_analyzed",
						"references").addScriptField("millis", "doc['date'].date.millis")
				.setQuery(QueryBuilders.textPhraseQuery("subject", subjectPattern));
		return builder;
	}

	public static SearchRequestBuilder countConversationThreadForDocumentSubjectQuery(SearchRequestBuilder builder,
			String subjectPattern, String project, String mail_list) {
		countConversationThreadForDocumentSubjectQuery(builder, subjectPattern).setFilter(
				FilterBuilders.andFilter(FilterBuilders.termsFilter("project", project),
						FilterBuilders.termsFilter("mail_list", mail_list)));
		return builder;
	}

	public static SearchRequestBuilder countConversationThreadForDocumentSubjectQuery(SearchRequestBuilder builder,
			String subjectPattern) {
		builder.setQuery(QueryBuilders.textPhraseQuery("subject", subjectPattern));
		return builder;
	}

	public static SearchSourceBuilder subjectSuggestionsSearch(String query) {
		SearchSourceBuilder b = new SearchSourceBuilder();
		b.query(QueryBuilders.textPhrasePrefixQuery("subject.suggestions", query).maxExpansions(1000)).facet(
				FacetBuilders.termsFacet("suggest").field("subject.suggestions").size(7)
						.regex("^" + query + "([\\S]*[\\s]?[\\S]*)"));
		return b;
	}
}
