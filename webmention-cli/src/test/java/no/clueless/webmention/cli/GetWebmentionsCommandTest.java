package no.clueless.webmention.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetWebmentionsCommandTest {
    @Test
    void constructor_should_succeed_when_only_required_args_are_present() {
        assertDoesNotThrow(() -> new GetWebmentionsCommand(new String[]{"--uri", "http://localhost"}));
    }

    @Test
    void constructor_should_succeed_when_all_args_are_present() {
        assertDoesNotThrow(() -> new GetWebmentionsCommand(new String[]{"--uri", "http://localhost", "--page", "1", "--size", "10", "--is-approved", "true"}));
    }
}