package com.bcp.workshop.auth;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(LoginFailureTestProfile.class)
class AuthResourceFailureTest {
    @Test
    void returnsDocumented500WhenLoginFlowThrows() {
        given()
                .contentType("application/json")
                .body(Map.of("username", "student", "password", "Workshop123!", "client", "angular-workshop"))
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("message", notNullValue());
    }
}

@Alternative
@Priority(1)
@ApplicationScoped
@IfBuildProfile("login-failure-test")
class FailingLegacyDatabase extends LegacyDatabase {
    @Override
    public UserRecord findByCredentials(String username, String password) {
        throw new IllegalStateException("simulated login failure");
    }
}