package no.clueless.webmention.cli;

import java.util.LinkedHashMap;
import java.util.Optional;

public class CommandRegistry {
    private final LinkedHashMap<String, CommandBase.Creator> commandFactories = new LinkedHashMap<>();

    public <T extends CommandBase> CommandRegistry register(String name, CommandBase.Creator creator) {
        commandFactories.put(name, creator);
        return this;
    }

    public Optional<CommandBase.Creator> find(String name) {
        return Optional.ofNullable(commandFactories.get(name));
    }

    public boolean isEmpty() {
        return commandFactories.isEmpty();
    }
}
