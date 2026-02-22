package no.clueless.oauth;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthManagementPluginTest {
    ClientStore clientStore;
    Javalin     app;
    static final String administratorAccessToken = "foobar";

    public static Stream<Arguments> getClientsShouldReturnExpectedStatusCodeBasedOnAuthorizationHeader() {
        return Stream.of(
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorAccessToken), 200),
                Arguments.of(null, 401)
        );
    }

    public static Stream<Arguments> patchClientsEnableShouldReturnExpectedStatusCodeBasedOnAuthorizationHeader() {
        return Stream.of(
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorAccessToken), 204),
                Arguments.of(null, 401)
        );
    }

    public static Stream<Arguments> patchClientsDisableShouldReturnUnauthorizedForUnauthorizedUsers() {
        return Stream.of(
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorAccessToken), 204),
                Arguments.of(null, 401)
        );
    }

    public static Stream<Arguments> deleteClientsShouldReturnUnauthorizedForUnauthorizedUsers() {
        return Stream.of(
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorAccessToken), 204),
                Arguments.of(null, 401)
        );
    }

    @BeforeEach
    void setUp() {
        clientStore = new InMemoryClientStore();
        clientStore.registerClient("test-id", "test-secret", Set.of(Scope.WEBMENTIONS_MANAGE));

        var tokenValidator = mock(TokenValidator.class);
        var oauthPrincipal = new OAuthPrincipal("test-id", Set.of(Scope.CLIENTS_MANAGE), "client_credentials");
        when(tokenValidator.validate(administratorAccessToken)).thenReturn(oauthPrincipal);

        app = Javalin.create(config -> {
            config.registerPlugin(new OAuthResourceServerPlugin(tokenValidator));
            config.registerPlugin(new OAuthManagementPlugin(clientStore, true));
        });
    }

    @ParameterizedTest
    @MethodSource
    void getClientsShouldReturnExpectedStatusCodeBasedOnAuthorizationHeader(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.get("/oauth/manage/clients", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(expectedStatusCode, response.code(), "Unexpected response code: " + responseString);
        });
    }

    @ParameterizedTest
    @MethodSource
    void patchClientsEnableShouldReturnExpectedStatusCodeBasedOnAuthorizationHeader(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.patch("/oauth/manage/clients/test-id/enable", "", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(expectedStatusCode, response.code(), "Unexpected response code: " + responseString);
        });
    }

    @ParameterizedTest
    @MethodSource
    void patchClientsDisableShouldReturnUnauthorizedForUnauthorizedUsers(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.patch("/oauth/manage/clients/test-id/disable", "", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(expectedStatusCode, response.code(), "Unexpected response code: " + responseString);
        });
    }

    @ParameterizedTest
    @MethodSource
    void deleteClientsShouldReturnUnauthorizedForUnauthorizedUsers(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.delete("/oauth/manage/clients/test-id", "", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(expectedStatusCode, response.code(), "Unexpected response code: " + responseString);
        });
    }
}