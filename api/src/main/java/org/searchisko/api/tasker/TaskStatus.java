package org.searchisko.api.tasker;

/**
 * Status of task execution. See {@link TaskStatusInfo} and {@link TaskManager}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum TaskStatus {

	/**
	 * new task, not executed yet
	 */
	NEW,
	/**
	 * Task runs just now
	 */
	RUNNING,
	/**
	 * Task execution failed (system error during execution), will be executed again later
	 */
	FAILOVER,
	/**
	 * Task execution canceled - final status
	 */
	CANCELED,
	/**
	 * Task execution finished OK - final status
	 */
	FINISHED_OK,
	/**
	 * Task execution finished with some errors - final status
	 */
	FINISHED_ERROR;

	public static TaskStatus fromString(String ts) {
		if (ts != null) {
			for (TaskStatus t : TaskStatus.values()) {
				if (ts.equalsIgnoreCase(t.name()))
					return t;
			}
		}
		return null;
	}

}
