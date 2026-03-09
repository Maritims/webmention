package no.clueless.webmention.cli;

import no.clueless.webmention.cli.commands.Command;
import no.clueless.webmention.cli.commands.CommandParameter;
import no.clueless.webmention.cli.commands.HelpCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelpCommandTest {
    CommandParameter mockCommandParameter(String shortName, String longName, String description) {
        var parameter = mock(CommandParameter.class);
        when(parameter.shortName()).thenReturn(shortName);
        when(parameter.longName()).thenReturn(longName);
        when(parameter.description()).thenReturn(description);
        when(parameter.required()).thenReturn(true);
        return parameter;
    }

    Command mockCommand(String name, String description, CommandParameter... parameters) {
        var command = mock(Command.class);
        when(command.name()).thenReturn(name);
        when(command.description()).thenReturn(description);
        when(command.parameters()).thenReturn(parameters);
        return command;
    }

    @Test
    void run() {
        var sut = new HelpCommand(List.of(
                mockCommand("get-webmentions", "Get webmentions on behalf of a base URI", mockCommandParameter("u", "uri", "The base URI")),
                mockCommand("send-webmentions", "Send webmentions on behalf of a base URI", mockCommandParameter("u", "uri", "The base URI")),
                mockCommand("publish-webmention", "Publish a webmention by its id", mockCommandParameter("u", "uri", "The base URI"), mockCommandParameter("i", "id", "The webmention id")),
                mockCommand("unpublish-webmention", "Unpublish a webmention by its id", mockCommandParameter("u", "uri", "The base URI"), mockCommandParameter("i", "id", "The webmention id")),
                mockCommand("help", "Shows this help text")
        ));
        sut.run();
    }
}