package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.rest.AuthStatusRestService;
import org.searchisko.ftest.DeploymentHelpers;

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFails;

/**
 * Integration test for /auth/status REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#userauthenticationstatusapi
 * 
 * @author Lukas Vlcek
 * @author Libor Krzyzanek
 */
@RunWith(Arquillian.class)
public class AuthStatusRestServiceTest {

	private static final String REST_API_AUTH_STATUS = DeploymentHelpers.CURRENT_REST_VERSION + "auth/status";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeploymentMinimalWebXML();
	}

	@Test
	@InSequence(0)
	public void assertSSOServiceNotAvailable() throws MalformedURLException {
		givenJsonAndLogIfFails().when().get(new URL(context, REST_API_AUTH_STATUS).toExternalForm()).then().statusCode(200)
				.header("WWW-Authenticate", nullValue()).body(AuthStatusRestService.RESPONSE_FIELD_AUTHENTICATED, is(false))
				.body(AuthStatusRestService.RESPONSE_FIELD_ROLES, nullValue());
	}

	@Test
	@InSequence(5)
	public void assertProviderLooksLikeUnauthenticatedThere() throws MalformedURLException {
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).log()
				.ifValidationFails().when().get(new URL(context, REST_API_AUTH_STATUS).toExternalForm()).then().statusCode(200)
				.header("WWW-Authenticate", nullValue()).body(AuthStatusRestService.RESPONSE_FIELD_AUTHENTICATED, is(false))
				.body(AuthStatusRestService.RESPONSE_FIELD_ROLES, nullValue());
	}

	@Test
	@InSequence(6)
	public void assertContributorAuthenticatedNoRolesRequested() throws MalformedURLException {
		given().contentType(ContentType.JSON).auth().preemptive().basic("contributor1", "password1").log()
				.ifValidationFails().expect().statusCode(200).header("WWW-Authenticate", nullValue())
				.body(AuthStatusRestService.RESPONSE_FIELD_AUTHENTICATED, is(true))
				.body(AuthStatusRestService.RESPONSE_FIELD_ROLES, nullValue()).when()
				.get(new URL(context, REST_API_AUTH_STATUS).toExternalForm());
	}

	@Test
	@InSequence(7)
	public void assertContributorAuthenticatedRolesRequested() throws MalformedURLException {
		List<String> roles = given().contentType(ContentType.JSON).auth().preemptive().basic("contributor1", "password1")
				.log().ifValidationFails().expect().statusCode(200).header("WWW-Authenticate", nullValue())
				.body(AuthStatusRestService.RESPONSE_FIELD_AUTHENTICATED, is(true)).when()
				.get(new URL(context, REST_API_AUTH_STATUS + "?roles=y").toExternalForm()).andReturn().getBody().jsonPath()
				.getList(AuthStatusRestService.RESPONSE_FIELD_ROLES);

		Assert.assertNotNull(roles);
		Assert.assertEquals(2, roles.size());
		Assert.assertTrue(roles.contains("contributor"));

	}

}
