package no.clueless.webmention.cli.commands;

import no.clueless.webmention.cli.ApplicationProperties;
import no.clueless.webmention.cli.Command;
import no.clueless.webmention.cli.CommandResult;
import org.jetbrains.annotations.NotNull;

public class Version implements Command {
    public Version() {
    }

    public @NotNull CommandResult execute(@NotNull String[] args) {
        System.out.println(ApplicationProperties.VERSION);
        return CommandResult.SUCCESS;
    }
}
