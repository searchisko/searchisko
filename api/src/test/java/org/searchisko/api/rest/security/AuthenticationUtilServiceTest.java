/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

/**
 * Unit test for {@link AuthenticationUtilService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AuthenticationUtilServiceTest {

//	/**
//	 * @return RestServiceBase instance for test with initialized logger
//	 */
//	protected AuthenticationUtilService getTested() {
//		AuthenticationUtilService tested = new AuthenticationUtilService();
//		tested.log = Logger.getLogger(RestServiceBase.class.getName());
//		return tested;
//	}
//
//	@Test
//	public void checkProviderManagementPermission() {
//		AuthenticationUtilService tested = getTested();
//
//		SecurityContext securityContextSuperadmin = new ProviderCustomSecurityContext(new SimplePrincipal("aa"), true,
//				true, "cas");
//		SecurityContext securityContextProvider = new ProviderCustomSecurityContext(new SimplePrincipal("myProvider"),
//				false, true, "cas");
//
//		// case - context for super admin provider
//		tested.checkProviderManagementPermission(securityContextSuperadmin, "myProvider");
//		// case - context for same provider
//		tested.checkProviderManagementPermission(securityContextProvider, "myProvider");
//
//		// case - context for another provider than checked
//		try {
//			tested.checkProviderManagementPermission(securityContextProvider, "myProviderAnother");
//			Assert.fail("NotAuthorizedException expected");
//		} catch (NotAuthorizedException e) {
//			// OK
//		}
//
//		// case - security context of wrong type
//		try {
//			tested.checkProviderManagementPermission(new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true,
//					"cas"), "myProvider");
//			Assert.fail("NotAuthenticatedException expected");
//		} catch (NotAuthenticatedException e) {
//			// OK
//		}
//
//		// case - no security context
//		try {
//			tested.checkProviderManagementPermission(null, "myProvider");
//			Assert.fail("NotAuthorizedException expected");
//		} catch (NotAuthorizedException e) {
//			// OK
//		}
//
//		// case - no provider
//		try {
//			tested.checkProviderManagementPermission(securityContextProvider, null);
//			Assert.fail("NotAuthorizedException expected");
//		} catch (NotAuthorizedException e) {
//			// OK
//		}
//
//	}
//
//	@Test
//	public void getAuthenticatedProvider() {
//		AuthenticationUtilService tested = getTested();
//
//		// CASE - not authenticated - security context is empty
//		try {
//			tested.getAuthenticatedProvider(null);
//			Assert.fail("Exception must be thrown");
//		} catch (NotAuthenticatedException e) {
//			// OK
//		}
//
//		// CASE - not authenticated - security context is bad type
//		{
//			SecurityContext scMock = Mockito.mock(SecurityContext.class);
//			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
//			try {
//				tested.getAuthenticatedProvider(scMock);
//				Assert.fail("Exception must be thrown");
//			} catch (NotAuthenticatedException e) {
//				// OK
//			}
//		}
//		{
//			SecurityContext securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "aa");
//			try {
//				tested.getAuthenticatedProvider(securityContext);
//				Assert.fail("Exception must be thrown");
//			} catch (NotAuthenticatedException e) {
//				// OK
//			}
//		}
//
//		// CASE - not authenticated - security context is correct type but principal is empty
//		{
//			SecurityContext securityContext = new ProviderCustomSecurityContext(null, false, false, "a");
//			try {
//				tested.getAuthenticatedProvider(securityContext);
//				Assert.fail("Exception must be thrown");
//			} catch (NotAuthenticatedException e) {
//				// OK
//			}
//		}
//
//		// CASE - provider authenticated OK
//		{
//			SecurityContext securityContext = new ProviderCustomSecurityContext(new SimplePrincipal("aa"), false, false, "a");
//			Assert.assertEquals("aa", tested.getAuthenticatedProvider(securityContext));
//		}
//
//		// CASE - provider authenticated OK - subclass of security context is evaluated OK (due proxying in CDI)
//		{
//			SecurityContext securityContext = new ProviderCustomSecurityContext(new SimplePrincipal("aa"), false, false, "a") {
//			};
//			Assert.assertEquals("aa", tested.getAuthenticatedProvider(securityContext));
//		}
//	}
//
//	@Test
//	public void getAuthenticatedContributor() {
//
//		// CASE - not authenticated - security context is empty
//		try {
//			AuthenticationUtilService tested = getTested();
//			tested.getAuthenticatedContributor(null, false);
//			Assert.fail("Exception must be thrown");
//		} catch (NotAuthenticatedException e) {
//			// OK
//		}
//
//		// CASE - not authenticated - security context is bad type
//		{
//			AuthenticationUtilService tested = getTested();
//			SecurityContext scMock = Mockito.mock(SecurityContext.class);
//			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
//			try {
//				tested.getAuthenticatedContributor(scMock, false);
//				Assert.fail("Exception must be thrown");
//			} catch (NotAuthenticatedException e) {
//				// OK
//			}
//		}
//		{
//			AuthenticationUtilService tested = getTested();
//			SecurityContext securityContext = new ProviderCustomSecurityContext(new SimplePrincipal("aa"), true, true,
//					ContributorAuthenticationInterceptor.AUTH_METHOD_CAS);
//			try {
//				tested.getAuthenticatedContributor(securityContext, false);
//				Assert.fail("Exception must be thrown");
//			} catch (NotAuthenticatedException e) {
//				// OK
//			}
//		}
//
//		// CASE - not authenticated - security context is correct type but principal is empty
//		{
//			AuthenticationUtilService tested = getTested();
//			SecurityContext securityContext = new ContributorCustomSecurityContext(null, true, "a");
//			try {
//				tested.getAuthenticatedContributor(securityContext, false);
//				Assert.fail("Exception must be thrown");
//			} catch (NotAuthenticatedException e) {
//				// OK
//			}
//		}
//
//		// CASE - provider authenticated OK - contributor id required and returned
//		{
//			AuthenticationUtilService tested = getTested();
//			boolean forceCreate = true;
//			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
//			Mockito.when(
//					tested.contributorProfileService.getContributorId(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
//							"aa", forceCreate)).thenReturn("bb");
//			SecurityContext securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true,
//					ContributorAuthenticationInterceptor.AUTH_METHOD_CAS);
//			Assert.assertEquals("bb", tested.getAuthenticatedContributor(securityContext, forceCreate));
//			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
//					Mockito.anyString(), Mockito.anyBoolean());
//
//			// second run uses cache
//			Assert.assertEquals("bb", tested.getAuthenticatedContributor(securityContext, forceCreate));
//			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
//					Mockito.anyString(), Mockito.anyBoolean());
//		}
//
//		// CASE - provider authenticated OK - contributor id is not required so not returned
//		{
//			AuthenticationUtilService tested = getTested();
//			boolean forceCreate = false;
//			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
//			Mockito.when(
//					tested.contributorProfileService.getContributorId(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
//							"aa", forceCreate)).thenReturn(null);
//			// we prepare subclass of security context to check it is evaluated OK (due proxying in CDI)
//			SecurityContext securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true,
//					ContributorAuthenticationInterceptor.AUTH_METHOD_CAS);
//			Assert.assertEquals(null, tested.getAuthenticatedContributor(securityContext, forceCreate));
//			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
//					Mockito.anyString(), Mockito.anyBoolean());
//
//			// second run do not uses cache for this case
//			Assert.assertEquals(null, tested.getAuthenticatedContributor(securityContext, forceCreate));
//			Mockito.verify(tested.contributorProfileService, Mockito.times(2)).getContributorId(Mockito.anyString(),
//					Mockito.anyString(), Mockito.anyBoolean());
//		}
//
//		// CASE - provider authenticated OK - contributor id is not required and returned first time, but is required and
//		// returned second time
//		{
//			AuthenticationUtilService tested = getTested();
//			boolean forceCreate = false;
//			boolean forceCreate2 = true;
//			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
//			Mockito.when(
//					tested.contributorProfileService.getContributorId(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
//							"aa", forceCreate)).thenReturn(null);
//			Mockito.when(
//					tested.contributorProfileService.getContributorId(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
//							"aa", forceCreate2)).thenReturn("bb");
//			SecurityContext securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true,
//					ContributorAuthenticationInterceptor.AUTH_METHOD_CAS);
//			Assert.assertEquals(null, tested.getAuthenticatedContributor(securityContext, forceCreate));
//			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
//					Mockito.anyString(), Mockito.anyBoolean());
//
//			// second run do not uses cache in this case
//			Assert.assertEquals("bb", tested.getAuthenticatedContributor(securityContext, forceCreate2));
//			Mockito.verify(tested.contributorProfileService, Mockito.times(2)).getContributorId(Mockito.anyString(),
//					Mockito.anyString(), Mockito.anyBoolean());
//		}
//
//	}
//
//	@Test
//	public void updateAuthenticatedContributorProfile() {
//		AuthenticationUtilService tested = getTested();
//		tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
//
//		// CASE - not authenticated - no call to service
//		{
//			Mockito.reset(tested.contributorProfileService);
//			tested.updateAuthenticatedContributorProfile(null);
//			Mockito.verifyZeroInteractions(tested.contributorProfileService);
//		}
//		{
//			Mockito.reset(tested.contributorProfileService);
//			SecurityContext scMock = Mockito.mock(SecurityContext.class);
//			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
//			tested.updateAuthenticatedContributorProfile(scMock);
//			Mockito.verifyZeroInteractions(tested.contributorProfileService);
//		}
//
//		SecurityContext securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true,
//				ContributorAuthenticationInterceptor.AUTH_METHOD_CAS);
//		// case - service call OK
//		{
//			Mockito.reset(tested.contributorProfileService);
//			tested.updateAuthenticatedContributorProfile(securityContext);
//			Mockito.verify(tested.contributorProfileService).createOrUpdateProfile(
//					ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "aa", false);
//		}
//
//		// case - service call exception is not propagated
//		{
//			Mockito.reset(tested.contributorProfileService);
//			Mockito.doThrow(new RuntimeException("Test exception from profile update"))
//					.when(tested.contributorProfileService)
//					.createOrUpdateProfile(Mockito.anyString(), Mockito.anyString(), Mockito.eq(false));
//			tested.updateAuthenticatedContributorProfile(securityContext);
//			Mockito.verify(tested.contributorProfileService).createOrUpdateProfile(
//					ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "aa", false);
//		}
//
//		// case - no service call for unsupported auth scheme
//		securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "a");
//		{
//			Mockito.reset(tested.contributorProfileService);
//			Mockito.doThrow(new RuntimeException("Test exception from profile update"))
//					.when(tested.contributorProfileService)
//					.createOrUpdateProfile(Mockito.anyString(), Mockito.anyString(), Mockito.eq(false));
//			tested.updateAuthenticatedContributorProfile(securityContext);
//			Mockito.verifyZeroInteractions(tested.contributorProfileService);
//		}
//
//	}

}
