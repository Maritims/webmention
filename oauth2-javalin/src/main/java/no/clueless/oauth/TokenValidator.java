package no.clueless.oauth;

/**
 * Represents a token validator.
 */
@FunctionalInterface
public interface TokenValidator {
    /**
     * Validates the given token.
     *
     * @param token The token to validate.
     * @return The validated token.
     */
    OAuthPrincipal validate(String token);
}
