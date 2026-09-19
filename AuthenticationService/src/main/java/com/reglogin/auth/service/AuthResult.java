package com.reglogin.auth.service;

import com.reglogin.auth.entity.User;

public record AuthResult(String token, User user) {
}