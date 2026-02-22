package no.clueless.webmention.javalin;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import no.clueless.oauth.*;
import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;
import no.clueless.webmention.receiver.WebmentionProcessor;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebmentionPluginTest {
    static final String administratorBearerToken = "foo";
    Javalin app;

    public static Stream<Arguments> delete_webmentionManageId_shouldReturnExpectedStatusCodeByAuthorizationHeader() {
        return Stream.of(
                Arguments.of(null, 401),
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorBearerToken), 204)
        );
    }

    public static Stream<Arguments> get_webmentionManage_shouldReturnExpectedStatusByAuthorizationHeader() {
        return Stream.of(
                Arguments.of(null, 401),
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorBearerToken), 200)
        );
    }

    public static Stream<Arguments> patch_webmentionManagePublishId_shouldReturnExpectedStatusCodeByAuthorizationHeader() {
        return Stream.of(
                Arguments.of(null, 401),
                Arguments.of(Headers.of("Authorization", "Bearer " + administratorBearerToken), 204)
        );
    }

    @BeforeEach
    void setUp() {
        var tokenValidator         = mock(TokenValidator.class);
        var administratorPrincipal = new OAuthPrincipal("administrator", Set.of(Scope.WEBMENTIONS_MANAGE), "client_credentials");
        when(tokenValidator.validate(administratorBearerToken)).thenReturn(administratorPrincipal);

        var webmentionRepository = mock(WebmentionRepository.class);
        var webmention           = mock(Webmention.class);
        when(webmentionRepository.findById(1)).thenReturn(Optional.of(webmention));

        app = Javalin.create(config -> {
            config.registerPlugin(new OAuthResourceServerPlugin(tokenValidator));
            config.registerPlugin(new WebmentionPlugin(mock(WebmentionProcessor.class), webmentionRepository));
        });
    }

    @Test
    void get_webmention_shouldReturnSuccessForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.get("/webmention");
            var responseBody = response.body();
            assertEquals(200, response.code());
            assertNotNull(responseBody);
        });
    }

    @Test
    void post_webmention_ShouldReturnSuccessForUnauthorizedUsers() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response     = httpClient.post("/webmention", "source=foo&target=bar", req -> req.header("Content-Type", "application/x-www-form-urlencoded"));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(202, response.code(), responseString);
        });
    }

    @ParameterizedTest
    @MethodSource
    void get_webmentionManage_shouldReturnExpectedStatusByAuthorizationHeader(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.get("/webmention/manage", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            assertEquals(expectedStatusCode, response.code());
        });
    }

    @ParameterizedTest
    @MethodSource
    void patch_webmentionManagePublishId_shouldReturnExpectedStatusCodeByAuthorizationHeader(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.patch("/webmention/manage/publish/1", "", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            assertEquals(expectedStatusCode, response.code());
        });
    }

    @ParameterizedTest
    @MethodSource
    void delete_webmentionManageId_shouldReturnExpectedStatusCodeByAuthorizationHeader(Headers authorizationHeader, int expectedStatusCode) {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.delete("/webmention/manage/1", "", req -> Optional.ofNullable(authorizationHeader).ifPresent(req::headers));
            var responseBody = response.body();
            assertNotNull(responseBody);

            var responseString = responseBody.string();
            assertEquals(expectedStatusCode, response.code(), "Unexpected response code: " + responseString);
        });
    }

    @Test
    void post_webmentionManage_shouldReturnNotFound() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.post("/webmention/manage");
            assertEquals(404, response.code());
        });
    }

    @Test
    void put_webmentionManage_shouldReturnNotFound() {
        JavalinTest.test(app, (server, httpClient) -> {
            var response = httpClient.put("/webmention/manage");
            assertEquals(404, response.code());
        });
    }
}