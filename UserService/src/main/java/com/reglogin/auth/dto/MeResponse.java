package com.reglogin.auth.dto;

public class MeResponse {

    private final String username;
    private final String email;
    private final String phone;

    public MeResponse(String username, String email, String phone) {
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}