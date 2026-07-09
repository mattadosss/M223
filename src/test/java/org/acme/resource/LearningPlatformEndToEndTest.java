package org.acme.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LearningPlatformEndToEndTest {

    @Test
    void createProjectTaskAndCommentWorkflowMakesTaskAndCommentAvailable() {
        // Given
        Integer projectId = given()
                .contentType("application/json")
                .body("""
                        {
                          "name": "E2E Lernprojekt",
                          "description": "Workflow vom API-Startpunkt bis zur gespeicherten Liste",
                          "ownerId": 1,
                          "memberIds": [1, 2]
                        }
                        """)
                .when().post("/projects")
                .then()
                .statusCode(201)
                .extract().path("id");

        // When
        Integer taskId = given()
                .contentType("application/json")
                .body("""
                        {
                          "projectId": %d,
                          "title": "E2E Aufgabe",
                          "description": "Aufgabe ueber API anlegen",
                          "status": "OPEN",
                          "dueDate": "2026-07-31",
                          "assignedUserId": 2
                        }
                        """.formatted(projectId))
                .when().post("/tasks")
                .then()
                .statusCode(201)
                .extract().path("id");

        Integer commentId = given()
                .contentType("application/json")
                .body("""
                        {
                          "userId": 1,
                          "text": "E2E Kommentar"
                        }
                        """)
                .when().post("/tasks/{id}/comments", taskId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Then
        List<Map<String, Object>> tasks = given()
                .when().get("/projects/{id}/tasks", projectId)
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        Map<String, Object> createdTask = tasks.stream()
                .filter(task -> taskId.equals(task.get("id")))
                .findFirst()
                .orElseThrow();

        assertEquals("E2E Aufgabe", createdTask.get("title"));
        assertEquals("OPEN", createdTask.get("status"));
        assertEquals(2, createdTask.get("assignedUserId"));

        List<Map<String, Object>> comments = given()
                .when().get("/tasks/{id}/comments", taskId)
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        Map<String, Object> createdComment = comments.stream()
                .filter(comment -> commentId.equals(comment.get("id")))
                .findFirst()
                .orElseThrow();

        assertEquals("E2E Kommentar", createdComment.get("text"));
        assertEquals(1, createdComment.get("userId"));
        assertNotNull(createdComment.get("createdAt"));
    }
}
