package com.chatapp.authservice.client;

import com.chatapp.authservice.config.CognitoConfig;
import com.chatapp.authservice.dto.LoginRequestDto;
import com.chatapp.authservice.dto.LoginResponseDto;
import com.chatapp.authservice.dto.RegisterRequestDto;
import com.chatapp.authservice.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CognitoClient {

    private final CognitoConfig cognitoConfig;
    private final CognitoIdentityProviderClient identityProviderClient;

    public String registerUser(RegisterRequestDto request) {
        // Cognito integration: user registration with SignUp.
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(cognitoConfig.getClientId())
                .username(request.getUsername())
                .password(request.getPassword())
                .userAttributes(AttributeType.builder().name("email").value(request.getEmail()).build())
                .build();
        SignUpResponse response = identityProviderClient.signUp(signUpRequest);
        return response.userSub();
    }

    public LoginResponseDto authenticate(LoginRequestDto request) {
        // Cognito integration: password-based login with InitiateAuth.
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", request.getUsername());
        authParameters.put("PASSWORD", request.getPassword());

        InitiateAuthRequest initiateAuthRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(cognitoConfig.getClientId())
                .authParameters(authParameters)
                .build();
        InitiateAuthResponse response = identityProviderClient.initiateAuth(initiateAuthRequest);
        AuthenticationResultType result = response.authenticationResult();
        return new LoginResponseDto(result.accessToken(), result.tokenType(), result.expiresIn());
    }

    public UserResponseDto getUserByAccessToken(String accessToken) {
        // Cognito integration: fetch current user profile using access token.
        GetUserResponse response = identityProviderClient.getUser(GetUserRequest.builder()
                .accessToken(accessToken)
                .build());
        Map<String, String> attributes = response.userAttributes().stream()
                .collect(Collectors.toMap(AttributeType::name, AttributeType::value, (first, second) -> second));
        String email = attributes.get("email");
        String userId = attributes.getOrDefault("sub", response.username());
        return new UserResponseDto(userId, response.username(), email);
    }
}
