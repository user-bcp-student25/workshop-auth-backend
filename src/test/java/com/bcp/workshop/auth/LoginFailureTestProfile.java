package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTestProfile;

public class LoginFailureTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "login-failure-test";
    }
}