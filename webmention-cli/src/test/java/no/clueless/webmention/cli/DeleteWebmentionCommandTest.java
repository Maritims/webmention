package no.clueless.webmention.cli;

import no.clueless.webmention.cli.commands.DeleteWebmentionCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeleteWebmentionCommandTest {
    @Test
    void constructor_should_succeed_when_all_args_are_present() {
        assertDoesNotThrow(() -> new DeleteWebmentionCommand(new String[]{"--uri", "http://localhost", "--id", "1"}));
    }
}