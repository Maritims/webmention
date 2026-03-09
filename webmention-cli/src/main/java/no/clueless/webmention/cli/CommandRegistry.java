package no.clueless.webmention.cli;

import no.clueless.webmention.cli.commands.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class CommandRegistry {
    private final LinkedHashMap<String, CommandBase.Creator<?>> commandFactories = new LinkedHashMap<>();
    private final LinkedHashMap<String, Command>                commands         = new LinkedHashMap<>();

    @NotNull
    public static CommandRegistry createWithAllCommands() {
        var commandregistry = new CommandRegistry()
                .register(GetWebmentionsCommand.class, GetWebmentionsCommand::new)
                .register(SendWebmentionsCommand.class, SendWebmentionsCommand::new)
                .register(PublishWebmentionCommand.class, PublishWebmentionCommand::new)
                .register(UnpublishWebmentionCommand.class, UnpublishWebmentionCommand::new)
                .register(DeleteWebmentionCommand.class, DeleteWebmentionCommand::new);
        commandregistry.register(HelpCommand.class, (ignored) -> new HelpCommand(commandregistry.registeredCommands()));
        return commandregistry;
    }

    public CommandRegistry register(@NotNull String name, @NotNull CommandBase.Creator<?> creator) {
        commandFactories.put(name, creator);
        return this;
    }

    @NotNull
    public <T extends CommandBase> CommandRegistry register(@NotNull Class<T> commandType, @NotNull CommandBase.Creator<T> creator) {
        var command = commandType.getAnnotation(Command.class);
        commandFactories.put(command.name(), creator);
        commands.put(command.name(), command);
        return this;
    }

    @NotNull
    public List<Command> registeredCommands() {
        return List.copyOf(commands.values());
    }

    @NotNull
    public Optional<CommandBase.Creator<?>> find(@NotNull String name) {
        return Optional.ofNullable(commandFactories.get(name));
    }

    public boolean isEmpty() {
        return commandFactories.isEmpty();
    }
}
