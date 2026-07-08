package org.acme.resource;

import java.net.URI;
import java.util.List;

import org.acme.dto.CommentRequest;
import org.acme.dto.LoginRequest;
import org.acme.dto.MemberRequest;
import org.acme.dto.ProjectRequest;
import org.acme.dto.TaskRequest;
import org.acme.dto.UserRequest;
import org.acme.model.Comment;
import org.acme.model.Project;
import org.acme.model.Task;
import org.acme.model.User;
import org.acme.service.LearningPlatformService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LearningPlatformResource {

    @Inject
    LearningPlatformService service;

    @POST
    @Path("/register")
    public Response register(UserRequest request) {
        User user = service.register(request);
        return Response.created(URI.create("/users/" + user.id())).entity(user).build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        return Response.ok(service.login(request)).build();
    }

    @GET
    @Path("/users/{id}")
    public Response getUser(@PathParam("id") long id) {
        return Response.ok(service.getUser(id)).build();
    }

    @GET
    @Path("/projects")
    public List<Project> getProjects(@QueryParam("userId") Long userId) {
        return service.getProjects(userId);
    }

    @POST
    @Path("/projects")
    public Response createProject(ProjectRequest request) {
        Project project = service.createProject(request);
        return Response.created(URI.create("/projects/" + project.id())).entity(project).build();
    }

    @GET
    @Path("/projects/{id}")
    public Response getProject(@PathParam("id") long id) {
        return Response.ok(service.getProject(id)).build();
    }

    @PUT
    @Path("/projects/{id}")
    public Response updateProject(@PathParam("id") long id, ProjectRequest request) {
        return Response.ok(service.updateProject(id, request)).build();
    }

    @DELETE
    @Path("/projects/{id}")
    public Response deleteProject(@PathParam("id") long id) {
        return Response.ok(service.deleteProject(id)).build();
    }

    @POST
    @Path("/projects/{id}/members")
    public Response addProjectMember(@PathParam("id") long id, MemberRequest request) {
        return Response.ok(service.addProjectMember(id, request)).build();
    }

    @GET
    @Path("/projects/{id}/tasks")
    public List<Task> getProjectTasks(@PathParam("id") long id) {
        return service.getProjectTasks(id);
    }

    @POST
    @Path("/tasks")
    public Response createTask(TaskRequest request) {
        Task task = service.createTask(request);
        return Response.created(URI.create("/tasks/" + task.id())).entity(task).build();
    }

    @PUT
    @Path("/tasks/{id}")
    public Response updateTask(@PathParam("id") long id, TaskRequest request) {
        return Response.ok(service.updateTask(id, request)).build();
    }

    @DELETE
    @Path("/tasks/{id}")
    public Response deleteTask(@PathParam("id") long id) {
        return Response.ok(service.deleteTask(id)).build();
    }

    @GET
    @Path("/tasks/{id}/comments")
    public List<Comment> getTaskComments(@PathParam("id") long id) {
        return service.getTaskComments(id);
    }

    @POST
    @Path("/tasks/{id}/comments")
    public Response createComment(@PathParam("id") long id, CommentRequest request) {
        Comment comment = service.createComment(id, request);
        return Response.created(URI.create("/tasks/" + id + "/comments/" + comment.id())).entity(comment).build();
    }
}
