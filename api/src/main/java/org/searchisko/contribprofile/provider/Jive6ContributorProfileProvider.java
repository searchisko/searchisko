/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import org.searchisko.contribprofile.model.ContributorProfile;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * Jive 6 implementation of Contributor Provider. <br/>
 * Documentation for Jive 6 REST API: https://developers.jivesoftware.com/api/v3/rest/PersonService.html
 *
 * @author Libor Krzyzanek
 */
@Named
@Stateless
@LocalBean
public class Jive6ContributorProfileProvider implements ContributorProfileProvider {

	@Override
	public ContributorProfile getProfile(String jbossorgUsername) {
		// TODO: Implement logic of getting profile from https://community.jboss.org/api/core/v3/people/username/%7Busername%7D

		String contributorId = "John Doe <john.doe@doe.com>";

		ContributorProfile profile = new ContributorProfile(contributorId);
		return profile;
	}
}
