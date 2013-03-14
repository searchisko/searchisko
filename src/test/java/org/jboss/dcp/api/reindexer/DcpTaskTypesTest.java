/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import org.jboss.dcp.api.tasker.UnsupportedTaskException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link DcpTaskTypes}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class DcpTaskTypesTest {

	@Test
	public void getTaskType() {
		Assert.assertEquals("reindex_from_persistence", DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType());
	}

	@Test
	public void getInstance() throws UnsupportedTaskException {
		try {
			DcpTaskTypes.getInstance(null);
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}
		try {
			DcpTaskTypes.getInstance("");
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}
		try {
			DcpTaskTypes.getInstance("nonsense");
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}

		for (DcpTaskTypes t : DcpTaskTypes.values()) {
			Assert.assertEquals(t, DcpTaskTypes.getInstance(t.getTaskType()));

		}
	}

}
