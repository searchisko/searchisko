/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.elasticsearch.common.Strings;

/**
 * JPA based implementation of {@link TaskPersister}. Uses shared RDBMS to persist tasks and synchronize execution in
 * the cluster.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Stateless
@LocalBean
public class TaskPersisterJpa implements TaskPersister {

	@Inject
	protected EntityManager em;

	protected static final Logger log = Logger.getLogger(TaskPersisterJpa.class.getName());

	/**
	 * Create persister.
	 * 
	 */
	public TaskPersisterJpa() {
		super();
	}

	@Override
	public String createTask(String taskType, Map<String, Object> taskConfig) {
		String id = Strings.randomBase64UUID();
		TaskStatusInfo t = new TaskStatusInfo();
		t.setId(id);
		t.setTaskType(taskType);
		t.setTaskCreatedAt(new Date());
		t.setTaskStatus(TaskStatus.NEW);
		t.setTaskConfig(taskConfig);
		em.persist(t);
		return id;
	}

	@Override
	public List<TaskStatusInfo> listTasks(String taskTypeFilter, List<TaskStatus> taskStatusFilter, int from, int size) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<TaskStatusInfo> queryList = cb.createQuery(TaskStatusInfo.class);
		Root<TaskStatusInfo> root = queryList.from(TaskStatusInfo.class);
		queryList.select(root);
		List<Predicate> filter = new ArrayList<>();
		if (taskStatusFilter != null && !taskStatusFilter.isEmpty()) {
			Expression<String> taskStatus = root.get("taskStatus");
			filter.add(taskStatus.in(taskStatusFilter));
		}
		if (taskTypeFilter != null && !taskTypeFilter.trim().isEmpty()) {
			filter.add(cb.equal(root.get("taskType"), taskTypeFilter));
		}
		if (filter.size() > 0) {
			queryList.where(filter.toArray(new Predicate[filter.size()]));
		}
		queryList.orderBy(cb.desc(root.get("taskCreatedAt")));
		TypedQuery<TaskStatusInfo> q = em.createQuery(queryList);
		if (from >= 0)
			q.setFirstResult(from);
		if (size > 0)
			q.setMaxResults(size);
		return q.getResultList();
	}

	@Override
	public TaskStatusInfo getTaskStatusInfo(String id) {
		return em.find(TaskStatusInfo.class, id);
	}

	@Override
	public boolean markTaskToBeCancelled(String id) {
		TaskStatusInfo tis = em.find(TaskStatusInfo.class, id, LockModeType.PESSIMISTIC_WRITE);
		if (tis != null
				&& !tis.isCancelRequested()
				&& (tis.getTaskStatus() == TaskStatus.NEW || tis.getTaskStatus() == TaskStatus.RUNNING || tis.getTaskStatus() == TaskStatus.FAILOVER)) {
			tis.setCancelRequested(true);
			return true;
		}
		return false;
	}

	@Override
	public boolean changeTaskStatus(String id, TaskStatus taskStatusNew, String message) {
		TaskStatusInfo tis = em.find(TaskStatusInfo.class, id, LockModeType.PESSIMISTIC_WRITE);
		if (tis != null) {
			TaskStatus taskStatusCurrent = tis.getTaskStatus();
			if (taskStatusCurrent == TaskStatus.CANCELED || taskStatusCurrent == TaskStatus.FINISHED_OK
					|| taskStatusCurrent == TaskStatus.FINISHED_ERROR || taskStatusNew == TaskStatus.NEW)
				return false;
			if ((taskStatusCurrent == TaskStatus.NEW && (taskStatusNew == TaskStatus.RUNNING || taskStatusNew == TaskStatus.CANCELED))
					|| (taskStatusCurrent == TaskStatus.RUNNING && (taskStatusNew == TaskStatus.CANCELED
							|| taskStatusNew == TaskStatus.FAILOVER || taskStatusNew == TaskStatus.FINISHED_ERROR || taskStatusNew == TaskStatus.FINISHED_OK))
					|| (taskStatusCurrent == TaskStatus.FAILOVER && (taskStatusNew == TaskStatus.RUNNING || taskStatusNew == TaskStatus.CANCELED))) {
				tis.setTaskStatus(taskStatusNew);
				tis.appendProcessingLog(message);
				if (taskStatusCurrent == TaskStatus.RUNNING) {
					tis.setLastRunFinishedAt(new Date());
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void writeTaskLog(String id, String message) {
		if (message == null || message.trim().isEmpty())
			return;
		TaskStatusInfo tis = em.find(TaskStatusInfo.class, id, LockModeType.PESSIMISTIC_WRITE);
		if (tis != null) {
			tis.appendProcessingLog(message);
		}
	}

	private static final List<TaskStatus> toRunTaskStatusFilter = new ArrayList<>();
	static {
		toRunTaskStatusFilter.add(TaskStatus.NEW);
		toRunTaskStatusFilter.add(TaskStatus.FAILOVER);
	}

	@Override
	public TaskStatusInfo getTaskToRun(String nodeId) {
		List<TaskStatusInfo> tsi = listTasks(null, toRunTaskStatusFilter, 0, 0);
		if (tsi != null && !tsi.isEmpty()) {
			for (int i = tsi.size() - 1; i >= 0; i--) {
				TaskStatusInfo work = tsi.get(i);
				if (taskIsRunnableNow(nodeId, work)) {
					try {
						em.lock(work, LockModeType.PESSIMISTIC_WRITE);
						if (work.startTaskExecution(nodeId)) {
							return work;
						}
					} catch (LockTimeoutException e) {
						log.fine("Lock exception for task id=" + work.getId());
					}
				}
			}
		}
		return null;
	}

	public static final long FAILOVER_DELAY_10 = 10 * 1000L;
	public static final long FAILOVER_DELAY_30 = 60 * 1000L;
	public static final long FAILOVER_DELAY_100 = 5 * 60 * 1000L;

	protected boolean taskIsRunnableNow(String nodeId, TaskStatusInfo work) {
		if (TaskStatus.FAILOVER == work.getTaskStatus()) {
			// #189 - give some timeout for failover attempt
			Date d = work.getLastRunFinishedAt();
			if (d != null) {
				if (nodeId.equals(work.getExecutionNodeId()) || work.getRunCount() > 1) {
					// we run on same node or more failover attempts so use longer timeouts as we do not cope with cluster
					// failover probably, there is some more serious runtime problem

					long delay = FAILOVER_DELAY_10;
					if (work.getRunCount() > 40) {
						delay = FAILOVER_DELAY_100;
					} else if (work.getRunCount() > 10) {
						delay = FAILOVER_DELAY_30;
					}

					return System.currentTimeMillis() >= (d.getTime() + delay);
				} else {
					// first failover on other node so it is real cluster failover probably, so use no timeout.
					return true;
				}
			}
		}
		return true;
	}

	private static final List<TaskStatus> runningTaskStatusFilter = new ArrayList<>();
	static {
		runningTaskStatusFilter.add(TaskStatus.RUNNING);
	}

	@Override
	public void heartbeat(String nodeId, Set<String> runningTasksId, long failoverTimeout) {
		List<TaskStatusInfo> tsi = listTasks(null, runningTaskStatusFilter, 0, 0);
		if (tsi != null && !tsi.isEmpty()) {
			long ct = System.currentTimeMillis();
			long ft = ct - failoverTimeout;
			for (TaskStatusInfo task : tsi) {
				try {
					em.lock(task, LockModeType.PESSIMISTIC_WRITE);
					if (task.getTaskStatus() == TaskStatus.RUNNING) {
						if (runningTasksId != null && runningTasksId.contains(task.getId())) {
							task.setHeartbeat(ct);
						} else {
							if (task.getHeartbeat() < ft) {
								changeTaskStatus(task.getId(), TaskStatus.FAILOVER, "Failover necessity detected by node '" + nodeId
										+ "' at " + ct + " due heartbeat timestamp " + task.getHeartbeat());
							}
						}
					}
				} catch (LockTimeoutException e) {
					log.fine("Lock exception for task id=" + task.getId());
				}
			}
		}
	}
}
