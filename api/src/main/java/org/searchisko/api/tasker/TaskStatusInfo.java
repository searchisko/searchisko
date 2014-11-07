/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.searchisko.api.util.SearchUtils;

/**
 * Information about task existing in {@link TaskManager} and about status of execution.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Entity
@XmlRootElement
public class TaskStatusInfo {

	/**
	 * Maximal number of failover attempts for task
	 */
	public static final int FAILOVER_MAX_NUM = 100;

	@Id
	protected String id;

	@NotNull
	protected String taskType;

	@Column(length = 65000)
	protected String taskConfigSerialized;

	@NotNull
	protected Date taskCreatedAt;

	protected Date lastRunStartedAt;

	/**
	 * Counter how much times this task was started.
	 */
	protected int runCount;

	protected Date lastRunFinishedAt;

	/**
	 * Identifier of cluster node where task runs or ran last time.
	 */
	protected String executionNodeId;

	@NotNull
	@Enumerated(EnumType.STRING)
	protected TaskStatus taskStatus;

	@Column(length = 65000)
	protected String processingLog;

	protected boolean cancelRequested = false;

	/**
	 * Timestamp of last heartbeat for this task. Used to failover tasks from failed cluater nodes.
	 */
	protected long heartbeat;

	/**
	 * @param message
	 */
	public void appendProcessingLog(String message) {
		if (message == null || message.trim().length() == 0)
			return;
		if (processingLog == null || processingLog.isEmpty()) {
			processingLog = message;
		} else {
			processingLog = processingLog + "\n" + message;
		}
		if (processingLog.length() > 65000) {
			processingLog = processingLog.substring(processingLog.length() - 65000);
		}
	}

	/**
	 * Check if object is in status to start task execution. Change object fields into state for task execution if yes.
	 * 
	 * @param executionNodeId identifier of cluster node where task is started
	 * @return true if task was in status which allow start execution, so execution was started. False if object is not in
	 *         state for execution (but some fields may be changed even in this).
	 */
	public boolean startTaskExecution(String executionNodeId) {
		if (taskStatus == TaskStatus.NEW || taskStatus == TaskStatus.FAILOVER) {
			if (cancelRequested) {
				taskStatus = TaskStatus.CANCELED;
				return false;
			} else if (taskStatus == TaskStatus.FAILOVER && runCount >= FAILOVER_MAX_NUM) {
				taskStatus = TaskStatus.CANCELED;
				appendProcessingLog("Cancelled due too much failover attempts");
				return false;
			} else {
				taskStatus = TaskStatus.RUNNING;
				lastRunStartedAt = new Date();
				lastRunFinishedAt = null;
				runCount++;
				this.executionNodeId = executionNodeId;
				heartbeat = System.currentTimeMillis();
				return true;
			}
		} else {
			return false;
		}
	}

	@JsonIgnore
	public Map<String, Object> getTaskConfig() {
		try {
			return SearchUtils.convertToJsonMap(taskConfigSerialized);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTaskConfig(Map<String, Object> taskConfig) {
		try {
			final String tmp = SearchUtils.convertJsonMapToString(taskConfig);
			this.taskConfigSerialized = tmp.trim().isEmpty() ? null : tmp;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getTaskConfigSerialized() {
		return taskConfigSerialized;
	}

	public void setTaskConfigSerialized(String taskConfigSerialized) {
		this.taskConfigSerialized = taskConfigSerialized;
	}

	public Date getTaskCreatedAt() {
		return taskCreatedAt;
	}

	public void setTaskCreatedAt(Date taskCreatedAt) {
		this.taskCreatedAt = taskCreatedAt;
	}

	public Date getLastRunStartedAt() {
		return lastRunStartedAt;
	}

	public void setLastRunStartedAt(Date lastRunStartedAt) {
		this.lastRunStartedAt = lastRunStartedAt;
	}

	public int getRunCount() {
		return runCount;
	}

	public void setRunCount(int runCount) {
		this.runCount = runCount;
	}

	public Date getLastRunFinishedAt() {
		return lastRunFinishedAt;
	}

	public void setLastRunFinishedAt(Date lastRunFinishedAt) {
		this.lastRunFinishedAt = lastRunFinishedAt;
	}

	public String getExecutionNodeId() {
		return executionNodeId;
	}

	public void setExecutionNodeId(String executionNodeId) {
		this.executionNodeId = executionNodeId;
	}

	public TaskStatus getTaskStatus() {
		return taskStatus;
	}

	public void setTaskStatus(TaskStatus taskStatus) {
		this.taskStatus = taskStatus;
	}

	public String getProcessingLog() {
		return processingLog;
	}

	public void setProcessingLog(String processingLog) {
		this.processingLog = processingLog;
	}

	public boolean isCancelRequested() {
		return cancelRequested;
	}

	public void setCancelRequested(boolean cancelRequested) {
		this.cancelRequested = cancelRequested;
	}

	public long getHeartbeat() {
		return heartbeat;
	}

	public void setHeartbeat(long heartbeat) {
		this.heartbeat = heartbeat;
	}

	@Override
	public String toString() {
		return "TaskStatusInfo [id=" + id + ", taskType=" + taskType + ", taskStatus=" + taskStatus
				+ ", taskConfigSerialized=" + taskConfigSerialized + ", taskCreatedAt=" + taskCreatedAt + ", lastRunStartedAt="
				+ lastRunStartedAt + ", runCount=" + runCount + ", lastRunFinishedAt=" + lastRunFinishedAt
				+ ", executionNodeId=" + executionNodeId + ", cancelRequested=" + cancelRequested + "]";
	}

}
