/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;

/**
 * Unit test for {@link org.searchisko.api.audit.AuditService}
 *
 * @author Libor Krzyzanek
 */
public class AuditServiceTest {

	public void methodTestOkBoth(@AuditId String id, @AuditContent Long data) {
		Object[] params = new Object[]{id, data};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertEquals(id, AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertEquals(data, AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}


	public void methodTestOnlyData(String id, @AuditContent Long data) {
		Object[] params = new Object[]{id, data};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertEquals(data, AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}

	public void methodTestOnlyData(@AuditContent Long data) {
		Object[] params = new Object[]{data};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertEquals(data, AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}


	public void methodTestOnlyId(@AuditId String id, Long data) {
		Object[] params = new Object[]{id, data};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertEquals(id, AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}

	public void methodTestOnlyId(@AuditId String id) {
		Object[] params = new Object[]{id};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertEquals(id, AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}


	public void methodTestNoAnnotations(String id, Long data) {
		Object[] params = new Object[]{id, data};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}

	public void methodTestNoAnnotations(String id) {
		Object[] params = new Object[]{id};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}

	public void methodTestNoAnnotations() {
		Object[] params = new Object[]{};
		Method m = new Object() {
		}.getClass().getEnclosingMethod();

		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditId.class));
		Assert.assertNull(AuditService.getAnnotatedParamValue(m, params, AuditContent.class));
	}


	@Test
	public void testGetAnnotatedParamValue() throws Exception {
		String id = "id";
		Long data = new Long(100);

		methodTestOkBoth(id, data);
		methodTestOnlyData(id, data);
		methodTestOnlyData(data);
		methodTestOnlyId(id, data);
		methodTestOnlyId(id);
		methodTestNoAnnotations(id, data);
		methodTestNoAnnotations(id);
		methodTestNoAnnotations();
	}
}