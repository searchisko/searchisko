/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.indexer;

import java.io.IOException;

import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.elasticsearch.river.jira.mgm.fullupdate.FullUpdateAction;
import org.jboss.elasticsearch.river.jira.mgm.fullupdate.FullUpdateRequest;
import org.jboss.elasticsearch.river.jira.mgm.fullupdate.FullUpdateResponse;
import org.jboss.elasticsearch.river.jira.mgm.fullupdate.NodeFullUpdateResponse;
import org.jboss.elasticsearch.river.jira.mgm.lifecycle.JRLifecycleAction;
import org.jboss.elasticsearch.river.jira.mgm.lifecycle.JRLifecycleCommand;
import org.jboss.elasticsearch.river.jira.mgm.lifecycle.JRLifecycleRequest;
import org.jboss.elasticsearch.river.jira.mgm.lifecycle.JRLifecycleResponse;
import org.jboss.elasticsearch.river.jira.mgm.lifecycle.NodeJRLifecycleResponse;
import org.jboss.elasticsearch.river.jira.mgm.state.JRStateAction;
import org.jboss.elasticsearch.river.jira.mgm.state.JRStateRequest;
import org.jboss.elasticsearch.river.jira.mgm.state.JRStateResponse;
import org.jboss.elasticsearch.river.jira.mgm.state.NodeJRStateResponse;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.util.SearchUtils;

/**
 * {@link IndexerHandler} implementation for <a
 * href="https://github.com/searchisko/elasticsearch-river-jira">elasticsearch-river-jira</a>.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@LocalBean
@Lock(LockType.WRITE)
public class EsRiverJiraIndexerHandler implements IndexerHandler {

	@Inject
	protected SearchClientService searchClientService;

	@Override
	public void forceReindex(String indexerName) throws ObjectNotFoundException {
		FullUpdateRequest actionRequest = new FullUpdateRequest(indexerName, null);

		FullUpdateResponse resp = searchClientService.getClient().admin().cluster()
				.execute(FullUpdateAction.INSTANCE, actionRequest).actionGet();
		NodeFullUpdateResponse nr = resp.getSuccessNodeResponse();
		if (nr == null) {
			throw new ObjectNotFoundException();
		}
	}

	@Override
	@Lock(LockType.READ)
	public Object getStatus(String indexerName) throws ObjectNotFoundException {

		JRStateRequest actionRequest = new JRStateRequest(indexerName);

		JRStateResponse resp = searchClientService.getClient().admin().cluster()
				.execute(JRStateAction.INSTANCE, actionRequest).actionGet();

		final NodeJRStateResponse nr = resp.getSuccessNodeResponse();
		if (nr == null) {
			throw new ObjectNotFoundException();
		}

		String si = nr.getStateInformation();
		try {
			return SearchUtils.convertToJsonMap(si);
		} catch (IOException e) {
			return si;
		}
	}

	@Override
	public void stop(String indexerName) throws ObjectNotFoundException {
		performLifecycleCommand(indexerName, JRLifecycleCommand.STOP);
	}

	@Override
	public void restart(String indexerName) throws ObjectNotFoundException {
		performLifecycleCommand(indexerName, JRLifecycleCommand.RESTART);
	}

	private void performLifecycleCommand(String indexerName, JRLifecycleCommand command) throws ObjectNotFoundException {
		JRLifecycleRequest actionRequest = new JRLifecycleRequest(indexerName, command);
		JRLifecycleResponse resp = searchClientService.getClient().admin().cluster()
				.execute(JRLifecycleAction.INSTANCE, actionRequest).actionGet();

		final NodeJRLifecycleResponse nr = resp.getSuccessNodeResponse();
		if (nr == null) {
			throw new ObjectNotFoundException();
		}
	}

}
