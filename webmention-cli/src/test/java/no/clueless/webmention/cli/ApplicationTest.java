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
        sut              = spy(new Application(writer, commandProcessor));

        doNothing().when(sut).handleEmptyArgs();
    }

    @Test
    void run_should_call_handleEmptyArgs_when_args_are_empty() throws CommandNotSpecifiedException, CommandNotFoundException {
        // arrange
        // act
        sut.run(new String[0]);

        // assert
        verify(sut, times(1)).handleEmptyArgs();
        verify(commandProcessor, never()).run(any());
    }

    @Test
    void run_should_call_handleEmptyArgs_when_there_is_only_one_arg() throws CommandNotSpecifiedException, CommandNotFoundException {
        // arrange
        var args = new String[]{"webmention-cli"};

        // act
        sut.run(args);

        // assert
        verify(sut, times(1)).handleEmptyArgs();
        verify(commandProcessor, never()).run(any());
    }

    @Test
    void run_should_call_commandProcessor_when_there_is_more_than_one_arg() throws CommandNotSpecifiedException, CommandNotFoundException {
        // arrange
        var args = new String[]{"webmention-cli", "bar"};

        // act
        sut.run(args);

        // assert
        verify(sut, never()).handleEmptyArgs();
        verify(commandProcessor, times(1)).run(eq(new String[]{"bar"}));
    }

    @Test
    void run_should_print_command_not_found_and_help_when_CommandNotFoundException_is_thrown() throws CommandNotSpecifiedException, CommandNotFoundException {
        // arrange
        doThrow(new CommandNotFoundException("foo")).when(commandProcessor).run(any());

        // act
        sut.run(new String[]{"webmention-cli", "foo"});

        // assert
        verify(sut, times(1)).printCommandNotFound("foo");
        verify(sut, times(1)).printHelp();
    }

    @Test
    void run_should_print_command_not_specified_and_help_when_CommandNotSpecifiedException_is_thrown() throws CommandNotSpecifiedException, CommandNotFoundException {
        // arrange
        doThrow(new CommandNotSpecifiedException()).when(commandProcessor).run(any());

        // act
        sut.run(new String[]{"webmention-cli", "foo"});

        // assert
        verify(sut, times(1)).printCommandNotSpecified();
        verify(sut, times(1)).printHelp();
    }

    @Test
    void run_should_invoke_deletion_webmention_command_when_command_is_delete() {
        sut.run(new String[]{"webmention-cli", "delete", "123"});
    }
}