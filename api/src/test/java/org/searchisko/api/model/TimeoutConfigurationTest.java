/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link TimeoutConfiguration}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TimeoutConfigurationTest {

  @Test
  public void init() throws IOException {
    TimeoutConfiguration tested = new TimeoutConfiguration();

    // case - defaults
    Assert.assertEquals(0, tested.stats());
    Assert.assertEquals(0, tested.search());
    Assert.assertEquals(0, tested.ping());
    Assert.assertEquals(0, tested.documentDetail());
    Assert.assertEquals(0, tested.documentReferencesCount());
    Assert.assertEquals(0, tested.documentReferencesSearch());
    Assert.assertEquals(0, tested.subjectPatternCount());
    Assert.assertEquals(0, tested.subjectPatternSearch());

    // case - init load
    tested.init();
    Assert.assertEquals(2, tested.stats());
    Assert.assertEquals(3, tested.search());
    Assert.assertEquals(4, tested.ping());
    Assert.assertEquals(5, tested.documentDetail());
    Assert.assertEquals(6, tested.documentReferencesCount());
    Assert.assertEquals(7, tested.documentReferencesSearch());
    Assert.assertEquals(8, tested.subjectPatternCount());
    Assert.assertEquals(9, tested.subjectPatternSearch());
  }

}
