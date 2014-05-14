/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.ResponseSpecification;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for /contributor REST API.
 *
 * @author Vlastimil Elias (velias at redhat dot com), Jason Porter (jporter@redhat.com)
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ContributorRestService
 */
@RunWith(Arquillian.class)
public class ContributorRestServiceTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws IOException {
        return DeploymentHelpers.createDeployment();
    }

    private static final String restVersion = DeploymentHelpers.DEFAULT_REST_VERSION;

	public static final String CONTRIBUTOR_REST_API = restVersion + "contributor/{id}";

	private static String contributorId;

	private static String contributorCode;

	@Test
	@InSequence(0)
	public void assertNotAuthenticated(@ArquillianResource URL context) throws MalformedURLException {
		int expStatus = 401;

		// GET /contributor/
		given().contentType(ContentType.JSON)
				.pathParam("id", "")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, CONTRIBUTOR_REST_API).toExternalForm());


		// GET /contributor/some-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, CONTRIBUTOR_REST_API).toExternalForm());

		// GET /contributor/search
		given().contentType(ContentType.JSON)
				.pathParam("id", "")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, CONTRIBUTOR_REST_API + "search").toExternalForm());

		// POST /contributor/
		given().contentType(ContentType.JSON)
				.pathParam("id", "")
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTRIBUTOR_REST_API).toExternalForm());

		// POST /contributor/some-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTRIBUTOR_REST_API).toExternalForm());


		// POST /contributor/some-id/code/some-code
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.pathParam("code", "some-code")
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTRIBUTOR_REST_API + "/code/{code}").toExternalForm());

		// POST /contributor/some-id/mergeTo/idTo
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.pathParam("idTo", "idTo")
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTRIBUTOR_REST_API + "/mergeTo/{idTo}").toExternalForm());

		// DELETE /contributor/some-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONTRIBUTOR_REST_API).toExternalForm());
	}


    @Test @InSequence(0)
    public void assertServiceRespondingNoHits(@ArquillianResource URL context) throws MalformedURLException {
        given().
				contentType(ContentType.JSON).
				auth().basic("jbossorg", "jbossorgjbossorg").
                param("email", "no-email@redhat.com").
        expect().
                log().ifError().
                statusCode(200).
                body("total", is(0)).
                body("hits", is(empty())).
        when().
                get(new URL(context, restVersion + "contributor/search").toExternalForm());
    }

	public static String createContributor(URL context, Map<String, Object> data) throws MalformedURLException {
		return given().
				contentType(ContentType.JSON).
				pathParam("id", "").
				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
				body(data).
				expect().
				contentType(ContentType.JSON).
				statusCode(200).
				body("id", is(not(empty()))).
				when().
				post(new URL(context, CONTRIBUTOR_REST_API).toExternalForm()).
				andReturn().body().jsonPath().get("id");
	}

    @Test @InSequence(1)
    public void assertPostWorksAuthenticated(@ArquillianResource URL context) throws MalformedURLException {
        final Map<String, Object> typeSpecificCode = new HashMap<>();
        typeSpecificCode.put("jbossorg_username", "LightGuard");
        typeSpecificCode.put("jbossorg_blog", "");
        typeSpecificCode.put("github_username", "LightGuard");

		contributorCode = "Jason Porter <jporter@redhat.com>";

        final Map<String, Object> params = new HashMap<>();
        params.put("code", contributorCode);
        params.put("email", "jporter@redhat.com");
        params.put("type_specific_code", typeSpecificCode);

        contributorId = createContributor(context, params);
    }

    @Test @InSequence(2)
    public void assertUnauthenticatedPostErrors(@ArquillianResource URL context) throws MalformedURLException {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", "Jason Porter <jporter@redhat.com>");
        params.put("email", "jporter@redhat.com");
        params.put("type_specific_code", "");

        given().
                contentType(ContentType.JSON).
                parameters(params).
        expect().
                statusCode(401).
        when().
                post(new URL(context, restVersion + "contributor").toExternalForm());
    }

    @Test @InSequence(3)
    public void assertAuthenticatedGetWithId(@ArquillianResource URL context) throws MalformedURLException {
        given().
                pathParam("id", contributorId).
                auth().basic("jbossorg", "jbossorgjbossorg").
        expect().
                log().ifError().
                statusCode(200).
                contentType(ContentType.JSON).
                body("email", is("jporter@redhat.com")).
                body("code", is(contributorCode)).
                body("type_specific_code.github_username", is("LightGuard")).
        when().
                get(new URL(context, restVersion + "contributor/{id}").toExternalForm());
    }

    @Test @InSequence(4)
    public void assertNonAuthenticatedGetWithId(@ArquillianResource URL context) throws MalformedURLException {
        given().
                pathParam("id", contributorId).
        expect().
                statusCode(401).
        when().
                get(new URL(context, restVersion + "contributor/{id}").toExternalForm());
    }

    @Test @InSequence(5)
    public void assertNonAuthenticatedGet(@ArquillianResource URL context) throws MalformedURLException {
        given().
        expect().
                statusCode(401).
        when().
                get(new URL(context, restVersion + "contributor/").toExternalForm());
    }

    @Test @InSequence(6)
    public void assertAuthenticatedUpdate(@ArquillianResource URL context) throws Exception {
        final Map<String, Object> typeSpecificCode = new HashMap<>();
        typeSpecificCode.put("jbossorg_blog", new String[] {"seam.Jason Porter"});

        final Map<String, Object> params = new HashMap<>();
		params.put("code", contributorCode);
		params.put("email", "jporter@redhat.com");
		params.put("name", "Jason Porter");
        params.put("type_specific_code", typeSpecificCode);

        given().
                pathParam("id", contributorId).
                contentType(ContentType.JSON).
                body(params).
                auth().basic("jbossorg", "jbossorgjbossorg").
        expect().
                statusCode(200).
				contentType(ContentType.JSON).
				body("id", is(contributorId)).
        when().
                post(new URL(context, restVersion + "contributor/{id}").toExternalForm());
    }

	@Test
	@InSequence(7)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}


	@Test @InSequence(8)
    public void assertAuthenticatedGet(@ArquillianResource URL context) throws MalformedURLException {
        given().
                parameters("from", 0, "to", 100).
                auth().basic("jbossorg", "jbossorgjbossorg").
        expect().
                statusCode(200).
				log().ifError().
                contentType(ContentType.JSON).
                body("total", is(1)).
                body("hits[0].id", is(contributorId)).
				body("hits[0].data.email", is("jporter@redhat.com")).
				body("hits[0].data.name", is("Jason Porter")).
				body("hits[0].data.type_specific_code.jbossorg_blog", hasItem("seam.Jason Porter")).
        when().
                get(new URL(context, restVersion + "contributor/").toExternalForm());
    }

	@Test @InSequence(20)
	public void assertSearch(@ArquillianResource URL context) throws MalformedURLException {
		ResponseSpecBuilder builder = new ResponseSpecBuilder();
		builder.expectContentType(ContentType.JSON);
		builder.expectStatusCode(200);
		builder.expectBody("total", is(1));
		builder.expectBody("hits[0].id", is(contributorId));
		builder.expectBody("hits[0].data.code", is(contributorCode));
		builder.expectBody("hits[0].data.name", is("Jason Porter"));
		builder.expectBody("hits[0].data.email", is("jporter@redhat.com"));
		builder.expectBody("hits[0].data.type_specific_code.jbossorg_blog", hasItem("seam.Jason Porter"));
		ResponseSpecification responseSpec = builder.build();

		given().
				parameters("code", contributorCode).
				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
				expect().
				log().ifError().
				spec(responseSpec).
				when().
				get(new URL(context, restVersion + "contributor/search").toExternalForm());

		given().
				parameters("email", "jporter@redhat.com").
				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
				expect().
				log().ifError().
				spec(responseSpec).
				when().
				get(new URL(context, restVersion + "contributor/search").toExternalForm());
		// Can be tested when .fulltext is present in mapping
//		given().
//				parameters("name", "jason").
//				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
//				expect().
//				log().ifError().
//				spec(responseSpec).
//				when().
//				get(new URL(context, restVersion + "contributor/search").toExternalForm());

	}

	@Test @InSequence(21)
	public void assertSearchNoResult(@ArquillianResource URL context) throws MalformedURLException {
		given().
				parameters("code", "bad-code").
				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
				expect().
				statusCode(200).
				log().ifError().
				contentType(ContentType.JSON).
				body("total", is(0)).
				when().
				get(new URL(context, restVersion + "contributor/search").toExternalForm());

		given().
				parameters("email", "bad-email").
				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
				expect().
				statusCode(200).
				log().ifError().
				contentType(ContentType.JSON).
				body("total", is(0)).
				when().
				get(new URL(context, restVersion + "contributor/search").toExternalForm());

		given().
				parameters("name", "bad-name").
				auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).
				expect().
				statusCode(200).
				log().ifError().
				contentType(ContentType.JSON).
				body("total", is(0)).
				when().
				get(new URL(context, restVersion + "contributor/search").toExternalForm());

	}


}
