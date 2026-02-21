package no.clueless.oauth;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents an authenticated OAuth client.
 *
 * @param clientId  the client id
 * @param scopes    the scopes granted to the client
 * @param grantType the grant type used to authenticate the client
 */
public record OAuthPrincipal(String clientId, Set<Scope> scopes, String grantType) {
    /**
     * Constructor.
     *
     * @param clientId  the client id
     * @param scopes    the scopes granted to the client
     * @param grantType the grant type used to authenticate the client
     * @throws IllegalArgumentException if clientId or grantType is null or blank.
     */
    public OAuthPrincipal {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (grantType == null || grantType.isBlank()) {
            throw new IllegalArgumentException("grantType cannot be null or blank");
        }
    }

    /**
     * Checks whether this principal has any of the given scopes.
     *
     * @param requiredScopes the set of scopes (Either as Set of Scope or Set of String) to check against
     * @return true if the principal has at least one of the given scopes, false otherwise
     */
    public boolean hasAnyScope(Either<Set<Scope>, Set<String>> requiredScopes) {
        if (requiredScopes == null) {
            return false;
        }

        return requiredScopes.map(
                scopes -> scopes != null && !scopes.isEmpty() && this.scopes != null && this.scopes.stream().anyMatch(scopes::contains),
                labels -> {
                    if (labels == null || labels.isEmpty()) {
                        return false;
                    }
                    Set<Scope> resolvedScopes = labels.stream()
                            .map(Scope::fromLabel)
                            .flatMap(java.util.Optional::stream)
                            .collect(Collectors.toSet());
                    return hasAnyScope(Either.left(resolvedScopes));
                }
        );
    }

}
