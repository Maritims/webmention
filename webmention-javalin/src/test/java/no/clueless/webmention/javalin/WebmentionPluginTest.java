package no.clueless.webmention.javalin;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import no.clueless.oauth.*;
import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;
import no.clueless.webmention.receiver.WebmentionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebmentionPluginTest {
    static final String administratorBearerToken = "foo";
    Javalin app;

    @BeforeEach
    void setUp() {
        var tokenValidator         = mock(TokenValidator.class);
        var administratorPrincipal = mock(OAuthPrincipal.class);
        when(tokenValidator.validate(administratorBearerToken)).thenReturn(administratorPrincipal);

        var scopeExtractor = mock(ScopeExtractor.class);
        when(scopeExtractor.extractScopes(administratorPrincipal)).thenReturn(Set.of(Scope.WEBMENTIONS_MANAGE));

        var webmentionRepository = mock(WebmentionRepository.class);
        var webmention           = mock(Webmention.class);
        when(webmentionRepository.getById(1)).thenReturn(Optional.of(webmention));

        app = Javalin.create(config -> {
            config.registerPlugin(new OAuthResourceServerPlugin<OAuthPrincipal>(tokenValidator, scopeExtractor));
            config.registerPlugin(new WebmentionPlugin(mock(WebmentionProcessor.class), webmentionRepository));
        });
    }

    @DisplayName("GET /webmention should return 200 for authorized users")
    @Test
    void getWebmentionShouldReturnSuccessForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.get("/webmention");
            var responseBody = response.body();
            assertEquals(200, response.code());
            assertNotNull(responseBody);
        });
    }

    @DisplayName("POST /webmention should return 202 for authorized users")
    @Test
    void postWebmentionShouldReturnSuccessForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.post("/webmention", "source=foo&target=bar", req -> req.header("Content-Type", "application/x-www-form-urlencoded"));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(202, response.code(), responseString);
        });
    }

    @DisplayName("GET /webmention/pending should return 401 for unauthorized users")
    @Test
    void getPendingWebmentionsShouldReturnUnauthorizedForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.get("/webmention/pending");
            assertEquals(401, response.code());
        });
    }

    @DisplayName("PATCH /webmention/pending/{id} should return 401 for unauthorized users")
    @Test
    void patchPendingWebmentionsShouldReturnUnauthorizedForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.patch("/webmention/pending/1");
            assertEquals(401, response.code());
        });
    }

    @DisplayName("GET /webmention/pending should return 200 for authorized users")
    @Test
    void getPendingWebmentionsShouldReturnSuccessForAuthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.get("/webmention/pending", req -> req.header("Authorization", "Bearer " + administratorBearerToken));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(200, response.code(), responseString);
        });
    }

    @DisplayName("PATCH /webmention/pending/{id} should return 204 for authorized users")
    @Test
    void patchPendingWebmentionsShouldReturnSuccessForAuthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.patch("/webmention/pending/1", "", req -> req.header("Authorization", "Bearer " + administratorBearerToken));
            assertEquals(204, response.code());
        });
    }

    @DisplayName("GET /webmention/pending should return 404 for unauthorized users")
    @Test
    void postPendingWebmentionsShouldReturnNotFound() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.post("/webmention/pending");
            assertEquals(404, response.code());
        });
    }

    @DisplayName("DELETE /webmention/pending should return 404 for unauthorized users")
    @Test
    void deletePendingWebmentionsShouldReturnNotFound() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.delete("/webmention/pending");
            assertEquals(404, response.code());
        });
    }

    @DisplayName("PUT /webmention/pending should return 404 for unauthorized users")
    @Test
    void putPendingWebmentionsShouldReturnNotFound() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.put("/webmention/pending");
            assertEquals(404, response.code());
        });
    }
}