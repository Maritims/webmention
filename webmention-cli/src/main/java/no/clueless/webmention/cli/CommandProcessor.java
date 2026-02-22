package no.clueless.webmention.cli;

import no.clueless.webmention.cli.commands.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CommandProcessor {
    private static final Logger log = LoggerFactory.getLogger(CommandProcessor.class);
    @NotNull
    private final Map<String, Supplier<Command>> commands = new HashMap<>();

    public CommandProcessor() {
        commands.put("find-and-send-webmentions", FindAndSendWebmentions::new);
        commands.put("get-webmentions", WebmentionManagementCommand::new);
        commands.put("publish-webmention", WebmentionManagementCommand::new);
        commands.put("unpublish-webmention", WebmentionManagementCommand::new);
        commands.put("delete-webmention", WebmentionManagementCommand::new);
        commands.put("version", Version::new);
        commands.put("help", Help::new);
    }

    public void process(@NotNull String[] args) {
        if (args.length == 0) {
            commands.get("help").get().execute(args);
            return;
        }

        var label   = args[0].toLowerCase();
        var command = commands.get(label);

        if (command == null) {
            commands.get("help").get().execute(args);
            System.exit(1);
        }

        log.debug("Executing command {}", label);

        var result = command.get().execute(args);
        switch (result) {
            case MISSING_REQUIRED_ARGUMENT, UNKNOWN_ARGUMENT -> {
                log.error("Invalid arguments for command {}. See logs for details", label);

                commands.get("help").get().execute(args);
                System.exit(1);
            }
            case FAILURE -> {
                log.error("Failed to execute command {}. See logs for details", label);
                System.exit(1);
            }
            case SUCCESS -> System.exit(0);
        }
    }
}
