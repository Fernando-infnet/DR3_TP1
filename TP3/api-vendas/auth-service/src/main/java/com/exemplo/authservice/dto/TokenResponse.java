package com.exemplo.authservice.dto;

/**
 * @param expiresIn validade do access token, em segundos
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
