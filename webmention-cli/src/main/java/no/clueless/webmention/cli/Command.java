package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Command {
    @NotNull
    CommandResult execute(@NotNull String[] args);
}
