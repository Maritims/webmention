package no.clueless.webmention.cli;

import no.clueless.webmention.cli.commands.CommandBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandProcessorTest {
    CommandRegistry  commandRegistry;
    CommandProcessor sut;

    @BeforeEach
    void setUp() {
        commandRegistry = mock(CommandRegistry.class);
        sut             = spy(new CommandProcessor(commandRegistry));
    }

    @Test
    void constructor_should_throw_when_registry_is_empty() {
        assertThrows(IllegalArgumentException.class, () -> new CommandProcessor(new CommandRegistry()));
    }

    @Test
    void constructor_should_not_throw_when_registry_is_not_empty() {
        assertDoesNotThrow(() -> new CommandProcessor(new CommandRegistry().register("foobar", mock())));
    }

    @Test
    void run_should_throw_IllegalArgumentException_when_args_are_empty() {
        assertThrows(IllegalArgumentException.class, () -> sut.run(new String[0]));
    }

    @Test
    void run_should_call_printUnknownArgHelp_and_printGeneralHelp_when_command_is_not_found() {
        assertThrows(CommandNotFoundException.class, () -> sut.run(new String[]{"bar"}));
    }

    @Test
    void run_should_rethrow_when_createCommand_throws_anything() {
        // arrange
        doThrow(new RuntimeException("foo")).when(commandRegistry).find(anyString());

        // act
        assertThrows(RuntimeException.class, () -> sut.run(new String[]{"bar"}));

        // assert
        verify(commandRegistry, atLeastOnce().description("No attempt was made at invoking the command \"bar\"")).find(eq("bar"));
    }

    @Test
    void run_should_create_and_run_command_when_command_factory_is_found() throws CommandNotFoundException, MissingRequiredParameter, InvalidParameterValueException {
        // arrange
        var command = mock(CommandBase.class);
        var commandFactory = mock(CommandBase.Creator.class);
        when(commandFactory.create(any())).thenReturn(command);
        //noinspection unchecked
        when(commandRegistry.find(eq("foo"))).thenReturn(Optional.of(commandFactory));

        // act
        sut.run(new String[]{"foo"});

        // assert
        verify(commandFactory, times(1)).create(eq(new String[]{}));
        verify(command, times(1)).run();
    }
}