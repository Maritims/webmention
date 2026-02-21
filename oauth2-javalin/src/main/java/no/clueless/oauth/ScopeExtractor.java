package no.clueless.oauth;

import java.util.Set;

/**
 * Responsible for extracting scopes from a principal.
 *
 * @param <Principal> The type of the principal.
 */
@FunctionalInterface
public interface ScopeExtractor<Principal> {
    /**
     * Extracts scopes from the given principal.
     *
     * @param principal The principal to extract scopes from.
     * @return The extracted scopes.
     */
    Set<Scope> extractScopes(Principal principal);
}
