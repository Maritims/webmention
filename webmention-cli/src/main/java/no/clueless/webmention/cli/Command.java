package no.clueless.webmention.cli;

/**
 * A command that can be executed.
 */
@FunctionalInterface
public interface Command {
    /**
     * Executes the command.
     *
     * @param args the command arguments
     * @return the command result
     */
    CommandResult execute(String[] args);
}
