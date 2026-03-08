package no.clueless.oauth;

import no.clueless.oauth.javalin.Scope;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OAuthClientTest {
    @Test
    void whenClientIdIsBlank_thenThrow() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new no.clueless.oauth2.core.OAuthClient("", "foo bar", Set.of(Scope.CLIENTS_MANAGE.getLabel()), true));
        assertEquals("clientId cannot be blank", exception.getMessage());
    }

    @Test
    void whenHashedClientSecretIsBlank_thenThrow() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new no.clueless.oauth2.core.OAuthClient("foo bar", "", Set.of(Scope.CLIENTS_MANAGE.getLabel()), true));
        assertEquals("hashedClientSecret cannot be blank", exception.getMessage());
    }

    @Test
    void whenScopesIsEmpty_thenSucceed() {
        assertDoesNotThrow(() -> new no.clueless.oauth2.core.OAuthClient("foo bar", "foo bar", Set.of(), true));
    }

    @Test
    void whenEnabledIsFalse_thenSucceed() {
        assertDoesNotThrow(() -> new no.clueless.oauth2.core.OAuthClient("foo bar", "foo bar", Set.of(Scope.CLIENTS_MANAGE.getLabel()), false));
    }
}