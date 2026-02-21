package no.clueless.oauth;

/**
 * Represents a token validator.
 *
 * @param <ValidatedToken> The type of the validated token.
 */
@FunctionalInterface
public interface TokenValidator<ValidatedToken> {
    /**
     * Validates the given token.
     *
     * @param token The token to validate.
     * @return The validated token.
     */
    ValidatedToken validate(String token);
}
