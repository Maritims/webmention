package no.clueless.oauth;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthServerPluginTest {
    ClientStore clientStore;

    @BeforeEach
    void setUp() {
        clientStore = new InMemoryClientStore();
    }

    @Test
    void shouldReturnTokenForValidBasicAuth() {
        clientStore.registerClient("test-id", "test-secret");

        var app = Javalin.create(config -> config.registerPlugin(new OAuthServerPlugin(oauth -> {
            oauth.clientStore = clientStore;
            oauth.tokenGenerator = client -> "fake-jwt";
        })));

        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.post("/oauth/token", "grant_type=client_credentials&client_id=test-id&client_secret=test-secret", req -> {
                req.header("Authorization", "Basic dGVzdC1pZDp0ZXN0LXNlY3JldA==");
                req.header("Content-Type", "application/x-www-form-urlencoded");
            });

            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(200, response.code(), "Unexpected response code: " + responseString);
            assertTrue(responseString.contains("\"access_token\":\"fake-jwt\""));
        });
    }

    @Test
    void shouldReturnUnauthorizedForInvalidSecret() {
        var app = Javalin.create(config -> config.registerPlugin(new OAuthServerPlugin(oauth -> oauth.clientStore = clientStore)));

        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.post("/oauth/token", "grant_type=client_credentials&client_id=test-id&client_secret=invalid-secret", req -> req.header("Content-Type", "application/x-www-form-urlencoded"));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(401, response.code(), "Unexpected response code: " + responseString);
        });
    }
}