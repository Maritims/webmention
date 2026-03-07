package no.clueless.webmention.cli;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Function;

public class CommandRegistry {
    private final LinkedHashMap<String, Function<String[], ? extends CommandBase>> commandFactories = new LinkedHashMap<>();

    public <T extends CommandBase> CommandRegistry register(String name, Function<String[], T> commandFactory) {
        commandFactories.put(name, commandFactory);
        return this;
    }

    public Optional<Function<String[], ? extends CommandBase>> find(String name) {
        return Optional.ofNullable(commandFactories.get(name));
    }

    public boolean isEmpty() {
        return commandFactories.isEmpty();
    }
}
