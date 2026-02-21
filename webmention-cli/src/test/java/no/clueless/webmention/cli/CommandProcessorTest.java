package no.clueless.webmention.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandProcessorTest {
    CommandProcessor sut;

    @BeforeEach
    void setUp() {
        sut = new CommandProcessor();
    }

    @Test
    void processShouldThrowIllegalArgumentExceptionWhenCommandIsUnknown() {
        var exception = assertThrows(IllegalArgumentException.class, () -> sut.process(new String[] { "foobar" }));
        assertEquals("Unknown command: foobar", exception.getMessage());
    }
}