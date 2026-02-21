package no.clueless.oauth;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OAuthManagementPluginTest {
    ClientStore clientStore;
    Javalin     app;

    @BeforeEach
    void setUp() {
        clientStore = new InMemoryClientStore();
        clientStore.registerClient("test-id", "test-secret", Set.of(Scope.WEBMENTIONS_MANAGE));
        app = Javalin.create(config -> {
            config.registerPlugin(new OAuthResourceServerPlugin<>(mock(TokenValidator.class), mock(ScopeExtractor.class)));
            config.registerPlugin(new OAuthManagementPlugin(clientStore, true));
        });
    }

    @Test
    void postClientsShouldReturnUnauthorizedForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.post("/oauth/manage/clients", "");
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(401, response.code(), "Unexpected response code: " + responseString);
        });
    }

}