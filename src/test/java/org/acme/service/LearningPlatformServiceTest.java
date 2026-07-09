package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.acme.dto.LoginRequest;
import org.acme.dto.ProjectRequest;
import org.acme.dto.TaskRequest;
import org.acme.dto.UserRequest;
import org.acme.model.Project;
import org.acme.model.Task;
import org.acme.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

class LearningPlatformServiceTest {

    private LearningPlatformService service;

    @BeforeEach
    void setUp() {
        service = new LearningPlatformService();
    }

    @Test
    void registerCreatesUserWithNextId() {
        User created = service.register(new UserRequest("lea", "lea@example.com"));

        assertEquals(3L, created.id());
        assertEquals("lea", created.username());
        assertEquals("lea@example.com", created.email());
        assertEquals(created, service.getUser(3L));
    }

    @Test
    void registerRejectsBlankUsername() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.register(new UserRequest(" ", "leer@example.com")));

        assertEquals("username darf nicht leer sein", exception.getMessage());
    }

    @Test
    void loginReturnsTokenAndExistingUser() {
        var response = service.login(new LoginRequest("maria", "secret"));

        assertEquals("Login erfolgreich", response.message());
        assertEquals("demo-token-maria", response.token());
        assertEquals(1L, response.user().id());
        assertEquals("maria", response.user().username());
    }

    @Test
    void getProjectsUsesDefaultUserWhenNoUserIdIsProvided() {
        List<Project> projects = service.getProjects(null);

        assertFalse(projects.isEmpty());
        assertEquals("Teamarbeit in Java", projects.getFirst().name());
        assertEquals(1L, projects.getFirst().ownerId());
    }

    @Test
    void createProjectMergesOwnerIntoMembers() {
        Project created = service.createProject(
                new ProjectRequest("Sprint Planung", "Backlog ordnen", 2L, List.of(1L, 2L, 1L)));

        assertEquals(3L, created.id());
        assertEquals("Sprint Planung", created.name());
        assertEquals(2L, created.ownerId());
        assertIterableEquals(List.of(2L, 1L), created.memberIds());
    }

    @Test
    void createTaskRejectsMissingProjectId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.createTask(new TaskRequest(null, "Ohne Projekt", null, null, null, null)));

        assertEquals("projectId darf nicht leer sein", exception.getMessage());
    }

    @Test
    void updateTaskKeepsExistingValuesWhenFieldsAreEmpty() {
        Task updated = service.updateTask(1L, new TaskRequest(null, " ", null, "DONE", null, null));

        assertEquals(1L, updated.id());
        assertEquals(1L, updated.projectId());
        assertEquals("API-Endpunkte definieren", updated.title());
        assertEquals("Routen und JSON-Strukturen abstimmen", updated.description());
        assertEquals("DONE", updated.status());
        assertEquals("2026-07-15", updated.dueDate().toString());
        assertEquals(1L, updated.assignedUserId());
    }

    @Test
    void deleteTaskRemovesTaskAndItsComments() {
        var response = service.deleteTask(1L);

        assertEquals("Aufgabe wurde geloescht", response.message());
        assertThrows(NotFoundException.class, () -> service.getTaskComments(1L));
    }
}
