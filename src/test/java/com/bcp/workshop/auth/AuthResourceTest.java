package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue());
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown-workshop-user\",\"password\":\"wrong-password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void logsInWhenCredentialsContainQuoteCharacters() throws Exception {
        String username = "quoted-user-" + UUID.randomUUID() + "'";
        String password = "pass'word";
        String dbUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");

        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO legacy_users(username,password,display_name,role) VALUES(?,?,?,?)")) {
            insert.setString(1, username);
            insert.setString(2, password);
            insert.setString(3, "Quoted User");
            insert.setString(4, "STUDENT");
            insert.executeUpdate();
        }

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .body("user.username", is(username))
                    .body("token", notNullValue());
        } finally {
            try (Connection connection = DriverManager.getConnection(dbUrl);
                 PreparedStatement delete = connection.prepareStatement(
                         "DELETE FROM legacy_users WHERE username=?")) {
                delete.setString(1, username);
                delete.executeUpdate();
            }
        }
    }
}
