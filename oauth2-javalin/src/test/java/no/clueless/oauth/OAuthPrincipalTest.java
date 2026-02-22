package no.clueless.oauth;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OAuthPrincipalTest {

    @Test
    void hasRequiredScope_shouldReturnTrue_whenClientIsGrantedRequiredScope() {
        // arrange
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");

        // act
        var result = principal.hasRequiredScope(Scope.CLIENTS_MANAGE);

        // assert
        assertTrue(result);
    }

    @Test
    void hasRequiredScope_shouldReturnFalse_whenClientIsNotGrantedRequiredScope() {
        // arrange
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");

        // act
        var result = principal.hasRequiredScope(Scope.WEBMENTIONS_MANAGE);

        // assert
        assertFalse(result);
    }

    @Test
    void hasRequiredScope_shouldReturnTrue_whenClientIsGrantedAnyScope_butRequiredScopesIsEmpty() {
        // arrange
        var principal = new OAuthPrincipal("client", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");

        // act
        var result = principal.hasRequiredScope();

        // assert
        assertTrue(result);
    }

    @Test
    void hasRequiredScope_shouldReturnTrue_whenClientIsNotGrantedAnyScope_andRequiredScopesIsEmpty() {
        // arrange
        var principal = new OAuthPrincipal("client", Set.of(), "client_credentials");

        // act
        var result = principal.hasRequiredScope();

        // assert
        assertTrue(result);
    }
}
