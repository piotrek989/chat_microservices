package com.chatapp.authservice.controller;

import com.chatapp.authservice.dto.LoginRequestDto;
import com.chatapp.authservice.dto.LoginResponseDto;
import com.chatapp.authservice.dto.RegisterRequestDto;
import com.chatapp.authservice.dto.UserResponseDto;
import com.chatapp.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(@RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring("Bearer ".length());
        }
        return ResponseEntity.ok(authService.getCurrentUser(accessToken));
    }
}
