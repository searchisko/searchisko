/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.annotations.header;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.dcp.api.rest.CORSSupportInterceptor;

/**
 * Annotation used to support CORS specification.
 * <p/>
 * It adds <code>'Access-Control-Allow-Origin: *'<code> header of JAX-RS response to allow a <em>simple cross-origin</em> requests.
 * <p/>
 * See <a href="http://www.w3.org/TR/cors/#simple-cross-origin-request">http://www.w3.org/TR/cors/#simple-cross-origin-request</a>
 * <p/>
 * It also allows to support <em>pre-flight</em> requests by listing allowed methods for <code>OPTIONS</code> methods that
 * will be returned in response header.
 * <p/>
 * Example:
 * <pre>
 * {@code
 *    @OPTIONS
 *    @Path("/{search_result_uuid}/{hit_id}")
 *    @GuestAllowed
 *    @CORSSupport(allowedMethods = {CORSSupport.PUT, CORSSupport.POST})
 *    public Object writeSearchHitUsedStatisticsRecordOPTIONS() {
 *        return Response.ok().build();
 *    }
 * }
 * </pre>
 * <p/>
 * See <a href="http://www.w3.org/TR/cors/#preflight-request">http://www.w3.org/TR/cors/#preflight-request</a>
 *
 * @author Lukas Vlcek
 * @see CORSSupportInterceptor
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CORSSupport {

    /**
     * Basic set of HTTP Request Methods.
     *
     * @link http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods
     */
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    /**
     * @return String[] array of allowed methods that will be returned in response to <code>OPTIONS</code> request.
     */
    String[] allowedMethods() default {};
}
