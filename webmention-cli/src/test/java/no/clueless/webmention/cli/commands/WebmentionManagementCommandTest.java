package no.clueless.webmention.cli.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WebmentionManagementCommandTest {
    WebmentionManagementCommand sut;

    public static Stream<Arguments> execute_shouldThrowIllegalArgumentException_whenArgsIsNullOrEmpty() {
        return Stream.of(
                Arguments.of(new String[]{}, "args must contain at least 2 elements"),
                Arguments.of(new String[]{"foo"}, "args must contain at least 2 elements")
        );
    }

    @BeforeEach
    void setUp() {
        sut = new WebmentionManagementCommand();
    }

    @ParameterizedTest
    @MethodSource
    void execute_shouldThrowIllegalArgumentException_whenArgsIsNullOrEmpty(String[] args, String expectedMessage) {
        var exception = assertThrows(IllegalArgumentException.class, () -> sut.execute(args));
        assertEquals(expectedMessage, exception.getMessage());
    }
}