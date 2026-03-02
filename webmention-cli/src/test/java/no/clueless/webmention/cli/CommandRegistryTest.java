package no.clueless.webmention.cli;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    public static Stream<Arguments> find() {
        return Stream.of(
                Arguments.of("discover"),
                Arguments.of("send")
        );
    }

    @ParameterizedTest
    @MethodSource
    void find(String name) {
        var actual = new CommandRegistry().find(name).orElse(null);
        assertNotNull(actual);
    }
}