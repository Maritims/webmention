package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CommandProcessor {
    @NotNull
    private final CommandRegistry registry;

    public CommandProcessor(@NotNull CommandRegistry registry) {
        if (registry.isEmpty()) {
            throw new IllegalArgumentException("registry cannot be empty");
        }
        this.registry = registry;
    }

    /**
     * Runs the command processor.
     *
     * @param args the command line arguments. The first argument must always be the command name.
     */
    public void run(@NotNull String[] args) throws CommandNotFoundException, CommandNotSpecifiedException {
        if (args.length == 0) {
            throw new CommandNotSpecifiedException();
        }

        var commandName    = args[0];
        var commandFactory = registry.find(commandName).orElse(null);

        if (commandFactory == null) {
            throw new CommandNotFoundException(commandName);
        }

        var commandArgs = Arrays.copyOfRange(args, 1, args.length);
        var command     = commandFactory.apply(commandArgs);
        if (command == null) {
            throw new CommandNotFoundException(commandName);
        }
        command.run();
    }
}
