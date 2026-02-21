package no.clueless.oauth;

import java.util.Set;

@FunctionalInterface
public interface TokenGenerator {
    /**
     * Generates a JSON Web Token for the given client.
     *
     * @param client The client to generate a token for.
     * @param scopes The scopes to include in the token.
     * @return The generated token.
     */
    String generate(OAuthClient client, Set<Scope> scopes);
}
