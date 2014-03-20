/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exceptionmapper;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link ExceptionMapperBase}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ExceptionMapperBaseTest {

	@Test
	public void collectErrorMessages() {

		// empty messages
		Assert.assertEquals("", ExceptionMapperBase.collectErrorMessages(null).toString());

		Assert.assertEquals("", ExceptionMapperBase.collectErrorMessages(new Exception()).toString());

		Assert.assertEquals("", ExceptionMapperBase.collectErrorMessages(new Exception((String) null)).toString());

		Assert.assertEquals("", ExceptionMapperBase.collectErrorMessages(new Exception("   ")).toString());

		// NPE special handling
		Assert.assertEquals("NullPointerException", ExceptionMapperBase.collectErrorMessages(new NullPointerException())
				.toString());

		// message exists
		Assert.assertEquals("test message", ExceptionMapperBase.collectErrorMessages(new Exception("test message"))
				.toString());

		// causes collection

		Assert.assertEquals("test message. Caused by: cause 1. Caused by: cause 2", ExceptionMapperBase
				.collectErrorMessages(new Exception("test message", new Exception("cause 1", new Exception("cause 2"))))
				.toString());

		Assert.assertEquals("test message. Caused by: NullPointerException",
				ExceptionMapperBase.collectErrorMessages(new Exception("test message", new NullPointerException())).toString());

	}

}
