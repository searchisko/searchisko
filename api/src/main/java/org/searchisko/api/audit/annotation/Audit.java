/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.audit.annotation;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Entry point for auditing. Use this annotation for marking method or class to be audited.
 * Use {@link org.searchisko.api.audit.annotation.AuditIgnore} if you need to audit all methods in class but ignore particular method.
 * <br/>
 * Use Annotations {@link org.searchisko.api.audit.annotation.AuditId} for marking method's parameter to be content id
 * and {@link org.searchisko.api.audit.annotation.AuditContent} for marking parameter as content.
 * <p/>
 * Enable Audit facility by adding {@link org.searchisko.api.audit.AuditInterceptor} into beans.xml:
 * <pre>
 * <interceptors>
 *     <class>org.searchisko.api.audit.AuditInterceptor</class>
 * </interceptors>
 * </pre>
 *
 * @author Libor Krzyzanek
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE})
@InterceptorBinding
public @interface Audit {

}
