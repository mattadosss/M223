package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }

    @Test
    void testCalculatorAddEndpoint() {
        given()
          .when().get("/calculator/add/12/30")
          .then()
             .statusCode(200)
             .body(is("42"));
    }

    @Test
    void testCalculatorMinusEndpoint() {
        given()
          .when().get("/calculator/minus/42/30")
          .then()
             .statusCode(200)
             .body(is("12"));
    }

}