package no.clueless.webmention.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ApplicationTest {
    CommandProcessor commandProcessor;
    Application      sut;

    @BeforeEach
    void setUp() {
        commandProcessor = mock(CommandProcessor.class);
        sut              = spy(new Application(commandProcessor));
    }

    @Test
    void run_should_call_handleEmptyArgs_when_args_are_empty() {
        // arrange
        doNothing().when(sut).handleEmptyArgs();

        // act
        sut.run(new String[0]);

        // assert
        verify(sut, times(1)).handleEmptyArgs();
        verify(commandProcessor, never()).run(any());
    }

    @Test
    void run_should_call_handleEmptyArgs_when_there_is_only_one_arg() {
        // arrange
        doNothing().when(sut).handleEmptyArgs();
        var args = new String[]{"foo"};

        // act
        sut.run(args);

        // assert
        verify(sut, times(1)).handleEmptyArgs();
        verify(commandProcessor, never()).run(any());
    }

    @Test
    void run_should_call_commandProcessor_when_there_is_more_than_one_arg() {
        // arrange
        var args = new String[]{"foo", "bar"};

        // act
        sut.run(args);

        // assert
        verify(sut, never()).handleEmptyArgs();
        verify(commandProcessor, times(1)).run(eq(new String[]{"bar"}));
    }
}