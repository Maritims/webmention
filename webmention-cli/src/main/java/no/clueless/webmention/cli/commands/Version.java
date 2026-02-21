package no.clueless.webmention.cli.commands;

import no.clueless.webmention.cli.ApplicationProperties;
import no.clueless.webmention.cli.Command;
import no.clueless.webmention.cli.CommandResult;

/**
 * Prints the version number.
 */
public class Version implements Command {
    /**
     * Constructs a new Version command.
     */
    public Version() {
    }

    public CommandResult execute(String[] args) {
        System.out.println(ApplicationProperties.VERSION);
        return null;
    }
}
