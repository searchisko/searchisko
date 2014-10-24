/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.lang.reflect.Member;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link Resources}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ResourcesTest {

	@Test
	public void produceFacesContext() {

		Resources tested = new Resources();

		// case - faces context not initialized
		try {
			tested.produceFacesContext();
			Assert.fail("ContextNotActiveException expected");
		} catch (ContextNotActiveException e) {
			// OK
		}

		// case - faces context initialized
		try {
			FacesContext fcMock = Mockito.mock(FacesContext.class);
			FacesContextMock.setCurrentInstanceImpl(fcMock);
			Assert.assertEquals(fcMock, tested.produceFacesContext());
		} finally {
			FacesContextMock.setCurrentInstanceImpl(null);
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void produceLog() {
		Resources tested = new Resources();
		InjectionPoint ipMock = Mockito.mock(InjectionPoint.class);
		Member memberMock = Mockito.mock(Member.class);
		Mockito.when(((Class<ProduceLogTestClass>) memberMock.getDeclaringClass())).thenReturn(ProduceLogTestClass.class);
		Mockito.when(ipMock.getMember()).thenReturn(memberMock);

		Logger l = tested.produceLog(ipMock);
		Assert.assertEquals("org.searchisko.api.util.ResourcesTest$ProduceLogTestClass", l.getName());
	}

	private static final class ProduceLogTestClass {
	};

	private static final class FacesContextMock extends FacesContext {

		public static void setCurrentInstanceImpl(FacesContext context) {
			setCurrentInstance(context);
		}

		@Override
		public Application getApplication() {
			return null;
		}

		@Override
		public Iterator<String> getClientIdsWithMessages() {
			return null;
		}

		@Override
		public ExternalContext getExternalContext() {
			return null;
		}

		@Override
		public Severity getMaximumSeverity() {
			return null;
		}

		@Override
		public Iterator<FacesMessage> getMessages() {
			return null;
		}

		@Override
		public Iterator<FacesMessage> getMessages(String clientId) {
			return null;
		}

		@Override
		public RenderKit getRenderKit() {
			return null;
		}

		@Override
		public boolean getRenderResponse() {
			return false;
		}

		@Override
		public boolean getResponseComplete() {
			return false;
		}

		@Override
		public ResponseStream getResponseStream() {
			return null;
		}

		@Override
		public void setResponseStream(ResponseStream responseStream) {
		}

		@Override
		public ResponseWriter getResponseWriter() {
			return null;
		}

		@Override
		public void setResponseWriter(ResponseWriter responseWriter) {
		}

		@Override
		public UIViewRoot getViewRoot() {
			return null;
		}

		@Override
		public void setViewRoot(UIViewRoot root) {
		}

		@Override
		public void addMessage(String clientId, FacesMessage message) {
		}

		@Override
		public void release() {
		}

		@Override
		public void renderResponse() {
		}

		@Override
		public void responseComplete() {
		}

	}

}
