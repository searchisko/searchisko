/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

/**
 * Unit test for {@link SearchRestService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchRestServiceTest {

//	@Test
//	public void search_permissions() {
//		TestUtils.assertPermissionGuest(SearchRestService.class, "search", UriInfo.class);
//	}
//
//	@Test
//	public void search() throws IOException {
//		SearchRestService tested = getTested();
//
//		// case - incorrect input
//		{
//			Object response = tested.search(null);
//			TestUtils.assertResponseStatus(response, Status.INTERNAL_SERVER_ERROR);
//		}
//
//		// case - correct processing sequence
//		{
//			UriInfo uriInfo = Mockito.mock(UriInfo.class);
//			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
//			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
//			QuerySettings qs = new QuerySettings();
//			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
//			SearchResponse sr = Mockito.mock(SearchResponse.class);
//			Mockito.doAnswer(new Answer<Object>() {
//
//				@Override
//				public Object answer(InvocationOnMock invocation) throws Throwable {
//					XContentBuilder b = (XContentBuilder) invocation.getArguments()[0];
//					b.field("testfield", "testvalue");
//					return null;
//				}
//			}).when(sr).toXContent(Mockito.any(XContentBuilder.class), Mockito.any(Params.class));
//			Mockito.when(
//					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
//							Mockito.eq(StatsRecordType.SEARCH))).thenReturn(sr);
//			Mockito.when(tested.searchService.getSearchResponseAdditionalFields(Mockito.eq(qs))).thenReturn(
//					new HashMap<String, String>());
//			Object response = tested.search(uriInfo);
//			Mockito.verify(uriInfo).getQueryParameters();
//			Mockito.verify(tested.querySettingsParser).parseUriParams(qp);
//			Mockito.verify(tested.searchService).performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
//					Mockito.eq(StatsRecordType.SEARCH));
//			Mockito.verify(tested.searchService).getSearchResponseAdditionalFields(Mockito.eq(qs));
//			Mockito.verifyNoMoreInteractions(tested.searchService);
//			TestUtils.assetStreamingOutputContentRegexp("\\{\"uuid\":\".+\",\"testfield\":\"testvalue\"\\}", response);
//		}
//
//		// case - error handling for invalid request value
//		{
//			Mockito.reset(tested.querySettingsParser, tested.searchService);
//			UriInfo uriInfo = Mockito.mock(UriInfo.class);
//			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
//			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
//			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenThrow(
//					new IllegalArgumentException("test exception"));
//			Object response = tested.search(uriInfo);
//			TestUtils.assertResponseStatus(response, Status.BAD_REQUEST);
//			Mockito.verifyNoMoreInteractions(tested.searchService);
//		}
//
//		// case - error handling for index not found exception
//		{
//			Mockito.reset(tested.querySettingsParser, tested.searchService);
//			UriInfo uriInfo = Mockito.mock(UriInfo.class);
//			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
//			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
//			QuerySettings qs = new QuerySettings();
//			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
//			Mockito.when(
//					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
//							Mockito.eq(StatsRecordType.SEARCH))).thenThrow(new IndexMissingException(null));
//			Object response = tested.search(uriInfo);
//			TestUtils.assertResponseStatus(response, Status.NOT_FOUND);
//		}
//
//		// case - error handling for other exceptions
//		{
//			Mockito.reset(tested.querySettingsParser, tested.searchService);
//			UriInfo uriInfo = Mockito.mock(UriInfo.class);
//			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
//			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
//			QuerySettings qs = new QuerySettings();
//			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
//			Mockito.when(
//					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
//							Mockito.eq(StatsRecordType.SEARCH))).thenThrow(new RuntimeException());
//			Object response = tested.search(uriInfo);
//			TestUtils.assertResponseStatus(response, Status.INTERNAL_SERVER_ERROR);
//		}
//	}
//
//	@Test
//	public void writeSearchHitUsedStatisticsRecord() {
//		SearchRestService tested = getTested();
//
//		// case - incorrect input
//		{
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord(null, "aa", null), Status.BAD_REQUEST);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("", "aa", null), Status.BAD_REQUEST);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord(" ", "aa", null), Status.BAD_REQUEST);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("sss", null, null), Status.BAD_REQUEST);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("sss", "", null), Status.BAD_REQUEST);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("sss", "  ", null), Status.BAD_REQUEST);
//			Mockito.verifyZeroInteractions(tested.searchService);
//		}
//
//		// case - input params trimmed
//		{
//			Mockito.reset(tested.searchService);
//			Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", null)).thenReturn(true);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord(" aaa ", " bb  ", "  "), Status.OK);
//			Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa", "bb", null);
//			Mockito.verifyNoMoreInteractions(tested.searchService);
//		}
//
//		// case - record accepted
//		{
//			Mockito.reset(tested.searchService);
//			Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", null)).thenReturn(true);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("aaa", "bb", null), Status.OK,
//					"statistics record accepted");
//			Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa", "bb", null);
//			Mockito.verifyNoMoreInteractions(tested.searchService);
//		}
//
//		// case - record denied
//		{
//			Mockito.reset(tested.searchService);
//			Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", "jj")).thenReturn(false);
//			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("aaa", "bb", "jj"), Status.OK,
//					"statistics record ignored");
//			Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa", "bb", "jj");
//    Mockito.verifyNoMoreInteractions(tested.searchService);
//}
//
//// case - exception from business service
//{
//        Mockito.reset(tested.searchService);
//Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa","bb",null)).thenThrow(
//        new RuntimeException("exception"));
//TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("aaa","bb",null),
//        Status.INTERNAL_SERVER_ERROR);
//Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa","bb",null);
//Mockito.verifyNoMoreInteractions(tested.searchService);
//}
//        }
//
//private SearchRestService getTested(){
//        SearchRestService tested=new SearchRestService();
//tested.querySettingsParser=Mockito.mock(QuerySettingsParser.class);
//tested.searchService=Mockito.mock(SearchService.class);
//tested.log=Logger.getLogger("test logger");
//return tested;
//}

}
