package com.reglogin.user.dto;

public class SignupResponse {

    private final String message;
    private final String name;
    private final String email;

    public SignupResponse(String message, String name, String email) {
        this.message = message;
        this.name = name;
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}