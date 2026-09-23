package com.reglogin.auth.service;

import com.reglogin.user.entity.User;

public record AuthResult(String token, User user) {
}