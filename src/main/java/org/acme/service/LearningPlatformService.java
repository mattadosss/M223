package org.acme.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.acme.dto.CommentRequest;
import org.acme.dto.LoginRequest;
import org.acme.dto.LoginResponse;
import org.acme.dto.MemberRequest;
import org.acme.dto.MessageResponse;
import org.acme.dto.ProjectRequest;
import org.acme.dto.TaskRequest;
import org.acme.dto.UserRequest;
import org.acme.model.Comment;
import org.acme.model.Project;
import org.acme.model.Task;
import org.acme.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class LearningPlatformService {

    private final AtomicLong userIds = new AtomicLong(3);
    private final AtomicLong projectIds = new AtomicLong(3);
    private final AtomicLong taskIds = new AtomicLong(4);
    private final AtomicLong commentIds = new AtomicLong(3);

    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<Long, Project> projects = new LinkedHashMap<>();
    private final Map<Long, Task> tasks = new LinkedHashMap<>();
    private final Map<Long, Comment> comments = new LinkedHashMap<>();

    public LearningPlatformService() {
        users.put(1L, new User(1, "maria", "maria@example.com"));
        users.put(2L, new User(2, "jonas", "jonas@example.com"));

        projects.put(1L, new Project(1, "Teamarbeit in Java", "REST-API fuer ein Gruppenprojekt planen", 1, List.of(1L, 2L)));
        projects.put(2L, new Project(2, "Praesentation Datenbanken", "Folien und Demo gemeinsam vorbereiten", 2, List.of(2L)));

        tasks.put(1L, new Task(1, 1, "API-Endpunkte definieren", "Routen und JSON-Strukturen abstimmen", "IN_PROGRESS", LocalDate.parse("2026-07-15"), 1L));
        tasks.put(2L, new Task(2, 1, "Klassendiagramm aktualisieren", "Modelle fuer Projekt, Aufgabe und Kommentar ergaenzen", "OPEN", LocalDate.parse("2026-07-18"), 2L));
        tasks.put(3L, new Task(3, 2, "Demo vorbereiten", "Kurzen Ablauf fuer die Vorstellung schreiben", "DONE", LocalDate.parse("2026-07-20"), 2L));

        comments.put(1L, new Comment(1, 1, 2, "Ich uebernehme die Beispiel-Requests.", LocalDateTime.parse("2026-07-07T10:15:00")));
        comments.put(2L, new Comment(2, 1, 1, "Super, ich pruefe danach die Responses.", LocalDateTime.parse("2026-07-07T10:30:00")));
    }

    public synchronized User register(UserRequest request) {
        requireRequest(request);
        requireText(request.username(), "username");
        requireText(request.email(), "email");
        long id = userIds.getAndIncrement();
        User user = new User(id, request.username(), request.email());
        users.put(id, user);
        return user;
    }

    public LoginResponse login(LoginRequest request) {
        requireRequest(request);
        requireText(request.username(), "username");
        return new LoginResponse("Login erfolgreich", "demo-token-" + request.username(), findUserByUsername(request.username()));
    }

    public User getUser(long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Benutzer wurde nicht gefunden");
        }
        return user;
    }

    public List<Project> getProjects(Long userId) {
        long effectiveUserId = userId == null ? 1L : userId;
        return projects.values().stream()
                .filter(project -> project.ownerId() == effectiveUserId || project.memberIds().contains(effectiveUserId))
                .toList();
    }

    public synchronized Project createProject(ProjectRequest request) {
        requireRequest(request);
        requireText(request.name(), "name");
        long id = projectIds.getAndIncrement();
        long ownerId = request.ownerId() == null ? 1L : request.ownerId();
        List<Long> memberIds = mergeMembers(ownerId, request.memberIds());
        Project project = new Project(id, request.name(), request.description(), ownerId, memberIds);
        projects.put(id, project);
        return project;
    }

    public Project getProject(long id) {
        Project project = projects.get(id);
        if (project == null) {
            throw new NotFoundException("Projekt wurde nicht gefunden");
        }
        return project;
    }

    public synchronized Project updateProject(long id, ProjectRequest request) {
        requireRequest(request);
        Project existing = getProject(id);
        String name = hasText(request.name()) ? request.name() : existing.name();
        String description = request.description() == null ? existing.description() : request.description();
        long ownerId = request.ownerId() == null ? existing.ownerId() : request.ownerId();
        List<Long> memberIds = request.memberIds() == null ? existing.memberIds() : mergeMembers(ownerId, request.memberIds());
        Project updated = new Project(id, name, description, ownerId, memberIds);
        projects.put(id, updated);
        return updated;
    }

    public synchronized MessageResponse deleteProject(long id) {
        if (projects.remove(id) == null) {
            throw new NotFoundException("Projekt wurde nicht gefunden");
        }
        tasks.values().removeIf(task -> task.projectId() == id);
        comments.values().removeIf(comment -> !tasks.containsKey(comment.taskId()));
        return new MessageResponse("Projekt wurde geloescht");
    }

    public synchronized Project addProjectMember(long projectId, MemberRequest request) {
        requireRequest(request);
        Project project = getProject(projectId);
        if (request.userId() == null) {
            throw new BadRequestException("userId darf nicht leer sein");
        }
        List<Long> memberIds = new ArrayList<>(project.memberIds());
        if (!memberIds.contains(request.userId())) {
            memberIds.add(request.userId());
        }
        Project updated = new Project(project.id(), project.name(), project.description(), project.ownerId(), memberIds);
        projects.put(projectId, updated);
        return updated;
    }

    public List<Task> getProjectTasks(long projectId) {
        getProject(projectId);
        return tasks.values().stream()
                .filter(task -> task.projectId() == projectId)
                .toList();
    }

    public synchronized Task createTask(TaskRequest request) {
        requireRequest(request);
        requireText(request.title(), "title");
        if (request.projectId() == null) {
            throw new BadRequestException("projectId darf nicht leer sein");
        }
        getProject(request.projectId());
        long id = taskIds.getAndIncrement();
        Task task = new Task(id, request.projectId(), request.title(), request.description(),
                defaultText(request.status(), "OPEN"), request.dueDate(), request.assignedUserId());
        tasks.put(id, task);
        return task;
    }

    public synchronized Task updateTask(long id, TaskRequest request) {
        requireRequest(request);
        Task existing = getTask(id);
        Long projectId = request.projectId() == null ? existing.projectId() : request.projectId();
        getProject(projectId);
        Task updated = new Task(id, projectId,
                hasText(request.title()) ? request.title() : existing.title(),
                request.description() == null ? existing.description() : request.description(),
                defaultText(request.status(), existing.status()),
                request.dueDate() == null ? existing.dueDate() : request.dueDate(),
                request.assignedUserId() == null ? existing.assignedUserId() : request.assignedUserId());
        tasks.put(id, updated);
        return updated;
    }

    public synchronized MessageResponse deleteTask(long id) {
        if (tasks.remove(id) == null) {
            throw new NotFoundException("Aufgabe wurde nicht gefunden");
        }
        comments.values().removeIf(comment -> comment.taskId() == id);
        return new MessageResponse("Aufgabe wurde geloescht");
    }

    public List<Comment> getTaskComments(long taskId) {
        getTask(taskId);
        return comments.values().stream()
                .filter(comment -> comment.taskId() == taskId)
                .toList();
    }

    public synchronized Comment createComment(long taskId, CommentRequest request) {
        requireRequest(request);
        getTask(taskId);
        if (request.userId() == null) {
            throw new BadRequestException("userId darf nicht leer sein");
        }
        requireText(request.text(), "text");
        long id = commentIds.getAndIncrement();
        Comment comment = new Comment(id, taskId, request.userId(), request.text(), LocalDateTime.now());
        comments.put(id, comment);
        return comment;
    }

    private Task getTask(long id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException("Aufgabe wurde nicht gefunden");
        }
        return task;
    }

    private User findUserByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst()
                .orElse(new User(1, username, username + "@example.com"));
    }

    private static List<Long> mergeMembers(long ownerId, List<Long> memberIds) {
        List<Long> merged = new ArrayList<>();
        merged.add(ownerId);
        if (memberIds != null) {
            memberIds.stream()
                    .filter(memberId -> !merged.contains(memberId))
                    .forEach(merged::add);
        }
        return List.copyOf(merged);
    }

    private static void requireRequest(Object request) {
        if (request == null) {
            throw new BadRequestException("Request-Body darf nicht leer sein");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new BadRequestException(fieldName + " darf nicht leer sein");
        }
    }

    private static String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
