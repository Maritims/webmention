package no.clueless.webmention.cli;

import no.clueless.webmention.cli.commands.Command;
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

    @Test
    void createWithAllCommands() {
        var commandRegistry = CommandRegistry.createWithAllCommands();
        var commands        = commandRegistry.registeredCommands();

        assertTrue(commands.stream().anyMatch(command -> command.name().equals("get-webmentions")), () -> "get-webmentions should be registered. The registered commands were " + commands.stream().map(Command::name).collect(java.util.stream.Collectors.joining(", ")));
        assertTrue(commands.stream().anyMatch(command -> command.name().equals("send-webmention")), "send-webmention should be registered. The registered commands were " + commands.stream().map(Command::name).collect(java.util.stream.Collectors.joining(", ")));
        assertTrue(commands.stream().anyMatch(command -> command.name().equals("publish-webmention")), "publish-webmention should be registered. The registered commands were " + commands.stream().map(Command::name).collect(java.util.stream.Collectors.joining(", ")));
        assertTrue(commands.stream().anyMatch(command -> command.name().equals("unpublish-webmention")), "unpublish-webmention should be registered. The registered commands were " + commands.stream().map(Command::name).collect(java.util.stream.Collectors.joining(", ")));
        assertTrue(commands.stream().anyMatch(command -> command.name().equals("delete-webmention")), "delete-webmention should be registered. The registered commands were " + commands.stream().map(Command::name).collect(java.util.stream.Collectors.joining(", ")));
        assertTrue(commands.stream().anyMatch(command -> command.name().equals("help")), "help should be registered. The registered commands were " + commands.stream().map(Command::name).collect(java.util.stream.Collectors.joining(", ")));
    }
}