package no.clueless.oauth;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OAuthPrincipalTest {

    @Test
    void hasAnyScopeWithScopes() {
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");
        
        assertTrue(principal.hasAnyScope(Either.left(Set.of(Scope.CLIENTS_MANAGE))));
        assertFalse(principal.hasAnyScope(Either.left(Set.of(Scope.WEBMENTIONS_MANAGE))));
    }

    @Test
    void hasAnyScopeWithLabels() {
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");
        
        assertTrue(principal.hasAnyScope(Either.right(Set.of("clients:manage"))));
        assertFalse(principal.hasAnyScope(Either.right(Set.of("webmentions:manage"))));
    }

    @Test
    void hasAnyScopeWithMixedLabels() {
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE, Scope.WEBMENTIONS_MANAGE), "client_credentials");
        
        assertTrue(principal.hasAnyScope(Either.right(Set.of("clients:manage", "non-existent"))));
        assertFalse(principal.hasAnyScope(Either.right(Set.of("non-existent"))));
    }

    @Test
    void hasAnyScopeWithNullOrEmpty() {
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");
        
        assertFalse(principal.hasAnyScope(null));
        assertFalse(principal.hasAnyScope(Either.left(Set.of())));
        assertFalse(principal.hasAnyScope(Either.right(Set.of())));
    }
}
