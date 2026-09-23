package com.reglogin.user.service;

import com.reglogin.auth.repository.UserRepository;
import com.reglogin.user.dto.SignupRequest;
import com.reglogin.user.dto.SignupResponse;
import com.reglogin.user.entity.User;
import com.reglogin.user.exception.UserAlreadyExistsException;
import com.reglogin.user.exception.UserValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse register(SignupRequest request) {
        String name = request.getName().trim();
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new UserValidationException("Password and Confirm Password do not match");
        }

        if (userRepository.existsByName(name)) {
            throw new UserAlreadyExistsException("Username is already registered: " + name);
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already registered: " + email);
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);

        // Never log or return the password (hash or plain text).
        log.info("Registered user id={} with name={} email={}", saved.getId(), saved.getName(), saved.getEmail());

        return new SignupResponse(
                "Registration successful! Please login.",
                saved.getName(),
                saved.getEmail());
    }
}