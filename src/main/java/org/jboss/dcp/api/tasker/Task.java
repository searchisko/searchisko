/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

/**
 * Interface for task implementation. Main task work is done inside {@link #run()} method.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface Task extends Runnable {

}
