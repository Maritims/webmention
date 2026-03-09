package no.clueless.oauth2.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public record TokenResponse(
        @JsonProperty(value = "access_token", required = true) @NotNull String accessToken,
        @JsonProperty(value = "token_type", required = true) @NotNull String tokenType,
        @JsonProperty(value = "expires_in", required = true) long expiresIn,
        @JsonProperty(value = "scope", required = true) @NotNull String scope
) {
    public TokenResponse {
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken cannot be blank");
        }
        if (tokenType.isBlank()) {
            throw new IllegalArgumentException("tokenType cannot be blank");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("expiresIn must be greater than 0");
        }
        if (scope.isBlank()) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
    }
}
