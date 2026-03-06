package no.clueless.webmention.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandRegistry {
    private static final Logger                                    log              = LoggerFactory.getLogger(CommandRegistry.class);
    private final        LinkedHashMap<String, Command.Factory<?>> commandFactories = new LinkedHashMap<>();

    public <T extends Command> CommandRegistry register(String name, Command.Factory<T> commandFactory) {
        commandFactories.put(name, commandFactory);
        return this;
    }

    public Optional<Command.Factory<?>> find(String name) {
        return Optional.ofNullable(commandFactories.get(name));
    }

    public void printHelp() {
        log.info("Available commands:\n{}", commandFactories.entrySet()
                .stream()
                .map(entry -> String.format("%s - %s", entry.getKey(), entry.getValue().help()))
                .collect(Collectors.joining("\n")));
    }

    public boolean isEmpty() {
        return commandFactories.isEmpty();
    }
}
