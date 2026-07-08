package org.acme.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class LearningPlatformResourceTest {

    @Test
    void getsProjectsForDefaultUser() {
        given()
                .when().get("/projects")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].name", is("Teamarbeit in Java"));
    }

    @Test
    void createsProjectFromRequestBody() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "name": "Neue Lerngruppe",
                          "description": "Aufgaben gemeinsam organisieren",
                          "ownerId": 1,
                          "memberIds": [1, 2]
                        }
                        """)
                .when().post("/projects")
                .then()
                .statusCode(201)
                .body("name", is("Neue Lerngruppe"))
                .body("ownerId", is(1));
    }

    @Test
    void updatesTaskFromPathIdAndRequestBody() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "title": "API-Endpunkte finalisieren",
                          "status": "DONE"
                        }
                        """)
                .when().put("/tasks/1")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("title", is("API-Endpunkte finalisieren"))
                .body("status", is("DONE"));
    }

    @Test
    void deletesTaskWithMessage() {
        given()
                .when().delete("/tasks/2")
                .then()
                .statusCode(200)
                .body("message", is("Aufgabe wurde geloescht"));
    }
}
