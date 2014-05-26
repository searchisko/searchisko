/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.audit.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Method annotated by {@link org.searchisko.api.audit.annotation.AuditIgnore} is ignored
 * even class is annotated by {@link org.searchisko.api.audit.annotation.Audit}.
 * <p/>
 * In case you need to use {@link org.searchisko.api.audit.annotation.AuditIgnore} on methods in parent class you can enforce audit
 * by adding {@link org.searchisko.api.audit.annotation.Audit} annotation on method of child class again because this annotation
 * has higher precedence.
 *
 * @author Libor Krzyzanek
 */
@Retention(RUNTIME)
@Target({METHOD})
public @interface AuditIgnore {

}
