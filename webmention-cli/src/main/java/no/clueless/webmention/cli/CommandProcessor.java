package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class CommandProcessor {
    private static final Logger          log = LoggerFactory.getLogger(CommandProcessor.class);
    @NotNull
    private final        CommandRegistry registry;

    public CommandProcessor(@NotNull CommandRegistry registry) {
        this.registry = registry;
    }

    public void run(@NotNull String[] args) {
        if (args.length < 2) {
            log.error("Missing command");
            registry.printHelp();
            return;
        }

        var commandName = args[1];
        registry.find(commandName).ifPresentOrElse(commandFactory -> {
            try {
                var command = commandFactory.createCommand(Arrays.copyOfRange(args, 2, args.length));
                command.run();
            } catch (IllegalArgumentException e) {
                log.error("An exception occurred", e);
                registry.printHelp();
            } catch (Command.Factory.FactoryException e) {
                log.error("Command \"{}\" could not be created: {} (the args were {})", commandName, e.getMessage(), e.getArgs());
                registry.printHelp();
            }
        }, () -> log.error("Unknown command: {}", commandName));
    }
}
