package no.clueless.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a token response from the OAuth2 provider.
 * @param accessToken the access token
 * @param tokenType the token type
 * @param expiresIn the number of seconds until the token expires
 * @param scope the scopes granted to the client
 */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("scope") String scope
) {
    /**
     * Constructor.
     * @param accessToken the access token
     * @param tokenType the token type
     * @param expiresIn the number of seconds until the token expires
     * @param scope  the scopes granted to the client
     * @throws IllegalArgumentException if accessToken, tokenType, expiresIn or scope is null or blank.
     */
    public TokenResponse {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken cannot be null or blank");
        }
        if (tokenType == null || tokenType.isBlank()) {
            throw new IllegalArgumentException("tokenType cannot be null or blank");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("expiresIn must be greater than 0");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope cannot be null or blank");
        }
    }
}
