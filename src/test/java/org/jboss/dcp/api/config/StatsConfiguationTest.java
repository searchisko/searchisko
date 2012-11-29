/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.config;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link StatsConfiguation}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StatsConfiguationTest {

  @Test
  public void init() throws IOException {
    StatsConfiguation tested = new StatsConfiguation();
    Assert.assertFalse(tested.enabled());

    tested.init();

    Assert.assertTrue(tested.enabled());
  }

}
