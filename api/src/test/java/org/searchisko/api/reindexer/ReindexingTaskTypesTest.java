/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import org.searchisko.api.tasker.UnsupportedTaskException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link ReindexingTaskTypes}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexingTaskTypesTest {

	@Test
	public void getTaskType() {
		Assert.assertEquals("reindex_from_persistence", ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType());
	}

	@Test
	public void getInstance() throws UnsupportedTaskException {
		try {
			ReindexingTaskTypes.getInstance(null);
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}
		try {
			ReindexingTaskTypes.getInstance("");
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}
		try {
			ReindexingTaskTypes.getInstance("nonsense");
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}

		for (ReindexingTaskTypes t : ReindexingTaskTypes.values()) {
			Assert.assertEquals(t, ReindexingTaskTypes.getInstance(t.getTaskType()));

		}
	}

}
