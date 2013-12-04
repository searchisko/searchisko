/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link JSONPRequestFilter}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@SuppressWarnings("deprecation")
public class JSONPRequestFilterTest {

	private static final String PARAM_CALLBACK = "callback";

	@Test
	public void doFilter() throws IOException, ServletException {
		JSONPRequestFilter tested = new JSONPRequestFilter();

		HttpServletResponse resMock = Mockito.mock(HttpServletResponse.class);
		FilterChain chainMock = Mockito.mock(FilterChain.class);

		// case - bad request type check
		try {
			tested.doFilter(Mockito.mock(ServletRequest.class), resMock, chainMock);
			Assert.fail("ServletException expected");
		} catch (ServletException e) {
			// OK
		}

		HttpServletRequest reqMock = Mockito.mock(HttpServletRequest.class);

		// case - no callback method defined
		Mockito.reset(reqMock, resMock, chainMock);
		Mockito.when(reqMock.getParameter(PARAM_CALLBACK)).thenReturn(null);
		tested.doFilter(reqMock, resMock, chainMock);
		Mockito.verify(chainMock).doFilter(reqMock, resMock);

		// case - callback method name invalid
		Mockito.reset(reqMock, resMock, chainMock);
		Mockito.when(reqMock.getParameter(PARAM_CALLBACK)).thenReturn("aho;'");
		try {
			tested.doFilter(reqMock, resMock, chainMock);
			Assert.fail("ServletException expected");
		} catch (ServletException e) {
			// OK
		}

		// case - callback method defined, response over writer
		{
			ServletOutputStream bos = Mockito.mock(ServletOutputStream.class);
			Mockito.reset(reqMock, resMock, chainMock);
			Mockito.when(reqMock.getParameter(PARAM_CALLBACK)).thenReturn("ahoj");
			Mockito.when(resMock.getOutputStream()).thenReturn(bos);
			Mockito.doAnswer(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Object[] args = invocation.getArguments();
					HttpServletResponse resp = (HttpServletResponse) args[1];
					resp.getWriter().write("ahoj response");
					resp.getWriter().write(" continue");
					return null;
				}
			}).when(chainMock).doFilter(Mockito.eq(reqMock), Mockito.any(ServletResponse.class));

			tested.doFilter(reqMock, resMock, chainMock);
			Mockito.verify(resMock).setContentType(JSONPRequestFilter.CONTENT_TYPE);
			Mockito.verify(resMock).setCharacterEncoding("UTF-8");
			Mockito.verify(resMock, Mockito.times(3)).getOutputStream();
			Mockito.verify(bos).write("ahoj(".getBytes());
			Mockito.verify(bos).write("ahoj response continue".getBytes());
			Mockito.verify(bos).write(");".getBytes());
			Mockito.verifyNoMoreInteractions(bos);
			Mockito.verifyNoMoreInteractions(resMock);
		}

		// case - callback method defined, response over output stream
		{
			ServletOutputStream bos = Mockito.mock(ServletOutputStream.class);
			Mockito.reset(reqMock, resMock, chainMock);
			Mockito.when(reqMock.getParameter(PARAM_CALLBACK)).thenReturn("ahoj");
			Mockito.when(resMock.getOutputStream()).thenReturn(bos);
			Mockito.doAnswer(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Object[] args = invocation.getArguments();
					HttpServletResponse resp = (HttpServletResponse) args[1];
					resp.getOutputStream().write("ahoj response".getBytes());
					resp.getOutputStream().write(" continue".getBytes());
					return null;
				}
			}).when(chainMock).doFilter(Mockito.eq(reqMock), Mockito.any(ServletResponse.class));

			tested.doFilter(reqMock, resMock, chainMock);
			Mockito.verify(resMock).setContentType(JSONPRequestFilter.CONTENT_TYPE);
			Mockito.verify(resMock).setCharacterEncoding("UTF-8");
			Mockito.verify(resMock, Mockito.times(3)).getOutputStream();
			Mockito.verify(bos).write("ahoj(".getBytes());
			Mockito.verify(bos).write("ahoj response continue".getBytes());
			Mockito.verify(bos).write(");".getBytes());
			Mockito.verifyNoMoreInteractions(bos);
			Mockito.verifyNoMoreInteractions(resMock);
		}

	}

	@Test
	public void getCallbackMethod() {
		JSONPRequestFilter tested = new JSONPRequestFilter();

		try {
			tested.getCallbackMethod(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		HttpServletRequest rMock = Mockito.mock(HttpServletRequest.class);
		Mockito.when(rMock.getParameter(PARAM_CALLBACK)).thenReturn("ahoj");
		Assert.assertEquals("ahoj", tested.getCallbackMethod(rMock));
	}

	@Test
	public void isJSONPRequest() {
		JSONPRequestFilter tested = new JSONPRequestFilter();

		Assert.assertFalse(tested.isJSONPRequest(null));
		Assert.assertFalse(tested.isJSONPRequest(""));
		Assert.assertTrue(tested.isJSONPRequest("a"));
	}
}
