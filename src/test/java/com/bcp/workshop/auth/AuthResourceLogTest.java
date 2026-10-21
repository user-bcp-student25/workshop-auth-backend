package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class AuthResourceLogTest {
    private static final String TEST_SECRET = "test-only-auth-token-signing-secret-2026";

    @Test
    void successfulLoginDoesNotLogTokenSecretOrPassword() {
        CapturingHandler handler = attachHandler();
        String token;
        try {
            token = given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("token");
        } finally {
            detachHandler(handler);
        }

        String logs = handler.messages();
        assertNotNull(token);
        assertFalse(logs.contains(token));
        assertFalse(logs.contains(TEST_SECRET));
        assertFalse(logs.contains("Workshop123!"));
    }

    @Test
    void failedLoginDoesNotLogSecretOrPassword() {
        CapturingHandler handler = attachHandler();
        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"wrong-password\",\"client\":\"angular-workshop\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(401);
        } finally {
            detachHandler(handler);
        }

        String logs = handler.messages();
        assertFalse(logs.contains(TEST_SECRET));
        assertFalse(logs.contains("wrong-password"));
    }

    private static CapturingHandler attachHandler() {
        CapturingHandler handler = new CapturingHandler();
        Logger.getLogger("").addHandler(handler);
        return handler;
    }

    private static void detachHandler(CapturingHandler handler) {
        Logger.getLogger("").removeHandler(handler);
    }

    private static final class CapturingHandler extends Handler {
        private final StringBuilder messages = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                messages.append(record.getMessage()).append(System.lineSeparator());
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}

        String messages() {
            return messages.toString();
        }
    }
}