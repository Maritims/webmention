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

    protected void printGeneralHelp() {
        System.out.println("webmention-cli: try 'webmention-cli help' for more information");
        System.exit(2);
    }

    protected void printUnknownArgHelp(@NotNull String arg) {
        System.out.println("webmention-cli: argument '" + arg + "' (quotes added)");
    }

    public void run(@NotNull String[] args) {
        if (args.length <= 1) {
            printGeneralHelp();
            return;
        }

        var commandName = args[1];
        registry.find(commandName).ifPresentOrElse(commandFactory -> {
            try {
                var commandArgs = Arrays.copyOfRange(args, 2, args.length);
                var command     = commandFactory.createCommand(commandArgs);
                command.run();
            } catch (Command.Factory.FactoryException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            printUnknownArgHelp(commandName);
            printGeneralHelp();
        });
    }
}
