package com.chatapp.authservice.service;

import com.chatapp.authservice.client.CognitoClient;
import com.chatapp.authservice.dto.LoginRequestDto;
import com.chatapp.authservice.dto.LoginResponseDto;
import com.chatapp.authservice.dto.RegisterRequestDto;
import com.chatapp.authservice.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CognitoClient cognitoClient;

    public UserResponseDto register(RegisterRequestDto request) {
        String userId = cognitoClient.registerUser(request);
        return new UserResponseDto(userId, request.getUsername(), request.getEmail());
    }

    public LoginResponseDto login(LoginRequestDto request) {
        return cognitoClient.authenticate(request);
    }

    public UserResponseDto getCurrentUser(String accessToken) {
        return cognitoClient.getUserByAccessToken(accessToken);
    }
}
