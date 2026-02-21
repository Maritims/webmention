package no.clueless.webmention.cli.commands;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import no.clueless.oauth.InMemoryClientStore;
import no.clueless.oauth.OAuthServerPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GetPendingWebmentionsTest {
    GetPendingWebmentions sut;

    public static Stream<Arguments> executeShouldThrowIllegalArgumentExceptionWhenArgsAreInvalid() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of((Object) new String[] { "whatever" })
        );
    }

    public static Stream<Arguments> executeShouldThrowIllegalArgumentExceptionWhenUriIsInvalid() {
        return Stream.of(
                Arguments.of(new String[] { "foo", null }, "Invalid URI: null"),
                Arguments.of(new String[] { "foo", "" }, "Invalid URI: "),
                Arguments.of(new String[] { "foo", "bar" }, "Invalid URI: bar")
        );
    }

    @BeforeEach
    void setUp() {
        sut = new GetPendingWebmentions("foo", "bar", "baz", "loremipsum");
    }

    @ParameterizedTest
    @MethodSource
    void executeShouldThrowIllegalArgumentExceptionWhenArgsAreInvalid(String[] args) {
        var exception = assertThrows(IllegalArgumentException.class, () -> sut.execute(args));
        assertEquals("Invalid number of arguments. Usage: get-pending-webmentions <uri>", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource
    void executeShouldThrowIllegalArgumentExceptionWhenUriIsInvalid(String[] args, String expectedMessage) {
        var exception = assertThrows(IllegalArgumentException.class, () -> sut.execute(args));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void foo() {
        var app = Javalin.create(config -> config.registerPlugin(new OAuthServerPlugin(new InMemoryClientStore(), (client, scopes) -> "fake-jwt")));
        JavalinTest.test(app, (server, httpClient) -> {
            httpClient.get("/webmention/pending");
        });
    }
}