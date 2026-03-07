package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

public class CommandNotFoundException extends Exception {
    @NotNull
    private final String commandName;

    public CommandNotFoundException(@NotNull String commandName) {
        super(String.format("No command with name %s was found", commandName));
        this.commandName = commandName;
    }

    public @NotNull String getCommandName() {
        return commandName;
    }
}
