package no.clueless.webmention.cli;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class ApplicationTest {

    public static Stream<Arguments> run_main() {
        return Stream.of(
                Arguments.of(new String[] { "wm-cli", "get-webmentions", "--uri", "foobar" }, ""),
                Arguments.of(new String[] { "wm-cli", "get-webmentions", "--uri" }, ""),
                Arguments.of(new String[] { "wm-cli", "get-webmentions", "--uri", "http://localhost:7070/webmention/manage" }, "")
        );
    }

    @ParameterizedTest
    @MethodSource
    void run_main(String[] args, String ignored) {
        Application.main(args);
    }
}