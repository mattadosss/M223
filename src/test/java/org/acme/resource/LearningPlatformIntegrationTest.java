package org.acme.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LearningPlatformIntegrationTest {

    @Test
    void registerUserThroughApiAndReadItBackFromServiceState() {
        // Given
        String username = "integration-user";
        String email = "integration-user@example.com";

        // When
        Integer userId = given()
                .contentType("application/json")
                .body("""
                        {
                          "username": "%s",
                          "email": "%s"
                        }
                        """.formatted(username, email))
                .when().post("/register")
                .then()
                .statusCode(201)
                .body("username", is(username))
                .body("email", is(email))
                .extract().path("id");

        // Then
        given()
                .when().get("/users/{id}", userId)
                .then()
                .statusCode(200)
                .body("id", is(userId))
                .body("username", is(username))
                .body("email", is(email));
    }

    @Test
    void createProjectThroughApiAndReadItBackFromServiceState() {
        // Given
        String projectName = "Integration Projekt";

        // When
        Integer projectId = given()
                .contentType("application/json")
                .body("""
                        {
                          "name": "%s",
                          "description": "Controller und Service gemeinsam testen",
                          "ownerId": 1,
                          "memberIds": [1, 2]
                        }
                        """.formatted(projectName))
                .when().post("/projects")
                .then()
                .statusCode(201)
                .body("name", is(projectName))
                .body("ownerId", is(1))
                .extract().path("id");

        // Then
        given()
                .when().get("/projects/{id}", projectId)
                .then()
                .statusCode(200)
                .body("id", is(projectId))
                .body("name", is(projectName))
                .body("memberIds", is(java.util.List.of(1, 2)));
    }
}
