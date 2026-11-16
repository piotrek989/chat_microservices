package com.chatapp.authservice.service;

import com.chatapp.authservice.client.CognitoClient;
import com.chatapp.authservice.dto.RegisterRequestDto;
import com.chatapp.authservice.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private CognitoClient cognitoClient;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerReturnsUserDetailsFromRequestAndCognitoId() {
        RegisterRequestDto request = new RegisterRequestDto("alice", "alice@example.com", "Password123");
        when(cognitoClient.registerUser(request)).thenReturn("cognito-user-id");

        UserResponseDto result = authService.register(request);

        assertEquals("cognito-user-id", result.getId());
        assertEquals("alice", result.getUsername());
        assertEquals("alice@example.com", result.getEmail());
        verify(cognitoClient).registerUser(request);
    }
}
