package no.clueless.webmention.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class ApplicationTest {
    PrintWriter      writer;
    CommandProcessor commandProcessor;
    Application      sut;

    @BeforeEach
    void setUp() {
        writer           = new PrintWriter(System.out);
        commandProcessor = mock(CommandProcessor.class);
        sut              = spy(new Application(commandProcessor));

        doNothing().when(sut).handleEmptyArgs();
    }

    @Test
    void run_should_call_printCommandNotSpecified_and_printHelp_and_handleEmptyArgs_when_args_are_empty() throws CommandNotFoundException, MissingRequiredParameter, InvalidParameterValueException {
        // arrange
        // act
        sut.run(new String[0]);

        // assert
        verify(sut, times(1)).printCommandNotSpecified();
        verify(sut, times(1)).printHelp();
        verify(sut, times(1)).handleEmptyArgs();
        verify(commandProcessor, never()).run(any());
    }

    @Test
    void run_should_call_commandProcessor_when_there_is_only_one_arg() throws CommandNotFoundException, MissingRequiredParameter, InvalidParameterValueException {
        // arrange
        var args = new String[]{"foo"};

        // act
        sut.run(args);

        // assert
        verify(sut, never()).handleEmptyArgs();
        verify(commandProcessor, times(1)).run(any());
    }

    @Test
    void run_should_call_commandProcessor_when_there_is_more_than_one_arg() throws CommandNotFoundException, MissingRequiredParameter, InvalidParameterValueException {
        // arrange
        var args = new String[]{"foo"};

        // act
        sut.run(args);

        // assert
        verify(sut, never()).handleEmptyArgs();
        verify(commandProcessor, times(1)).run(eq(new String[]{"foo"}));
    }

    @Test
    void run_should_print_command_not_found_and_help_when_CommandNotFoundException_is_thrown() throws CommandNotFoundException, MissingRequiredParameter, InvalidParameterValueException {
        // arrange
        doThrow(new CommandNotFoundException("foo")).when(commandProcessor).run(any());

        // act
        sut.run(new String[]{"webmention-cli", "foo"});

        // assert
        verify(sut, times(1)).printCommandNotFound("foo");
        verify(sut, times(1)).printHelp();
    }
}