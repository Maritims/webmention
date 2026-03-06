package no.clueless.webmention.cli;

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

        doNothing().when(sut).printGeneralHelp();
        doNothing().when(sut).printUnknownArgHelp(anyString());
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
    void run_should_call_printGeneralHelp_when_args_are_empty() {
        // arrange
        // act
        sut.run(new String[0]);

        // assert
        verify(commandRegistry, never()).find(anyString());
        verify(sut, times(1)).printGeneralHelp();
    }

    @Test
    void run_should_call_printGeneralHelp_when_there_is_only_one_arg() {
        // arrange
        // act
        sut.run(new String[]{"foobar"});

        // assert
        verify(commandRegistry, never()).find(anyString());
        verify(sut, times(1)).printGeneralHelp();
    }

    @Test
    void run_should_call_printUnknownArgHelp_and_printGeneralHelp_when_command_is_not_found() {
        // arrange
        // act
        sut.run(new String[]{"foo", "bar"});

        // assert
        verify(commandRegistry, times(1)).find(eq("bar"));
        verify(sut, times(1)).printUnknownArgHelp(eq("bar"));
        verify(sut, times(1)).printGeneralHelp();
    }

    @Test
    void run_should_rethrow_printException_when_createCommand_throws_anything() {
        // arrange
        doThrow(new RuntimeException("foo")).when(commandRegistry).find(anyString());

        // act
        assertThrows(RuntimeException.class, () -> sut.run(new String[]{"foo", "bar"}));

        // assert
        verify(sut, never()).printGeneralHelp();
        verify(sut, never()).printUnknownArgHelp(anyString());
        verify(commandRegistry, times(1)).find(eq("bar"));
    }

    @Test
    void run_should_call_create_and_run_command_when_command_factory_is_found() throws Command.Factory.FactoryException {
        // arrange
        var command        = mock(Command.class);
        var commandFactory = mock(Command.Factory.class);
        when(commandFactory.createCommand(any())).thenReturn(command);
        //noinspection unchecked
        when(commandRegistry.find(eq("bar"))).thenReturn(Optional.of(commandFactory));

        // act
        sut.run(new String[]{"foo", "bar", "baz"});

        // assert
        verify(commandFactory, times(1)).createCommand(eq(new String[]{"baz"}));
        verify(command, times(1)).run();
    }
}