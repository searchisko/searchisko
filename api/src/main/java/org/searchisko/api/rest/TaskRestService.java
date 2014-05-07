/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.searchisko.api.security.Role;
import org.searchisko.api.service.TaskService;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.TaskStatus;
import org.searchisko.api.tasker.TaskStatusInfo;
import org.searchisko.api.tasker.UnsupportedTaskException;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Long running Tasks execution related REST API.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/tasks")
@RolesAllowed(Role.ADMIN)
public class TaskRestService extends RestServiceBase {

	@Inject
	protected TaskService taskService;

	@GET
	@Path("/type")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getTypes() {
		return taskService.getTaskManager().listSupportedTaskTypes();
	}

	@GET
	@Path("/task")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getTasks(@QueryParam("taskType") String taskType, @QueryParam("taskStatus") String[] taskStatus,
						   @QueryParam("from") Integer from, @QueryParam("size") Integer size) {

		List<TaskStatus> taskStatusFilter = null;
		if (taskStatus != null && taskStatus.length > 0) {
			taskStatusFilter = new ArrayList<TaskStatus>();
			for (String s : taskStatus) {
				TaskStatus ts = TaskStatus.fromString(s);
				if (ts != null)
					taskStatusFilter.add(ts);
			}
		}
		return taskService.getTaskManager().listTasks(taskType, taskStatusFilter, from != null ? from : 0,
				size != null ? size : 0);
	}

	@GET
	@Path("/task/{taskId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getTask(@PathParam("taskId") String taskId) {
		TaskStatusInfo tsi = taskService.getTaskManager().getTaskStatusInfo(taskId);
		return tsi != null ? tsi : Response.status(Status.NOT_FOUND).build();
	}

	@POST
	@Path("/task/{taskType}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Object createTask(@PathParam("taskType") String taskType, Map<String, Object> content) {
		try {
			String id = taskService.getTaskManager().createTask(taskType, content);
			return createResponseWithId(id);
		} catch (TaskConfigurationException e) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Configuration is invalid for used taskType: " + e.getMessage()).build();
		} catch (UnsupportedTaskException e) {
			return Response.status(Status.BAD_REQUEST).entity("Used taskType is not supported").build();
		}
	}

	@DELETE
	@Path("/task/{taskId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object cancelTask(@PathParam("taskId") String id) {
		boolean ret = taskService.getTaskManager().cancelTask(id);
		return Response.ok(ret ? "Task canceled" : "Task not canceled").build();
	}

}
