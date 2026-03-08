package no.clueless.webmention.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CommandRegistryTest {
    @Test
    void isEmpty_should_return_true_when_registry_is_empty() {
        assertTrue(new CommandRegistry().isEmpty());
    }

    @Test
    void isEmpty_should_return_false_when_registry_is_not_empty() {
        assertFalse(new CommandRegistry().register("foobar", mock()).isEmpty());
    }
}