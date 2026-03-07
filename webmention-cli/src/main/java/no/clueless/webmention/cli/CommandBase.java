package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public abstract class CommandBase implements Runnable {
    private static final Map<Class<?>, Function<String, ?>>                                  PARSERS            = new HashMap<>(Map.of(
            Integer.class, Integer::parseInt,
            Boolean.class, Boolean::parseBoolean,
            URI.class, URI::create,
            String.class, s -> s
    ));
    private static final ConcurrentHashMap<Class<? extends CommandBase>, CommandParameter[]> COMMAND_PARAMETERS = new ConcurrentHashMap<>();

    @NotNull
    private final String name;
    @NotNull
    private final String description;

    protected CommandBase() {
        var annotation = this.getClass().getAnnotation(Command.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Command class must be annotated with @Command");
        }
        this.name        = annotation.name();
        this.description = annotation.description();
    }

    @NotNull
    public static CommandParameter[] getParameters(@NotNull Class<? extends CommandBase> commandClass) {
        if (!COMMAND_PARAMETERS.containsKey(commandClass)) {
            var annotation = commandClass.getAnnotation(Command.class);
            var params     = annotation.parameters();
            COMMAND_PARAMETERS.put(commandClass, params);
        }

        var commandParameters = COMMAND_PARAMETERS.get(commandClass);
        if (commandParameters.length == 0) {
            throw new IllegalArgumentException("Command class must have at least one parameter annotated with @CommandParameter");
        }
        return commandParameters;
    }

    @NotNull
    public static <T> T getArgOfTypeOrThrow(@NotNull Map<String, Object> args, @NotNull String key, @NotNull Class<T> type) {
        var value = args.get(key);
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Expected " + key + " to be " + type.getName() + " but got " + value.getClass().getName());
        }
        return type.cast(value);
    }

    @NotNull
    public static <T extends CommandBase> Map<String, Object> getArgs(@NotNull String[] args, @NotNull Class<T> commandClass) {
        var params = getParameters(commandClass);
        var map    = new HashMap<String, Object>();

        for (var i = 0; i < args.length; i++) {
            var cleanArg = args[i].replaceAll("^--", "").replaceAll("^-", "");

            for (var parameter : params) {
                if (!cleanArg.equalsIgnoreCase(parameter.shortName()) && !cleanArg.equalsIgnoreCase(parameter.longName())) {
                    continue;
                }

                var parser = PARSERS.get(parameter.type());
                if(parser == null) {
                    throw new RuntimeException("No parser for type " + parameter.type());
                }
                String rawArg;

                if (parameter.requiresValue()) {
                    if (i == args.length - 1) {
                        throw new IllegalArgumentException("missing value for parameter \"" + parameter.longName() + "\" (quotes added)");
                    }

                    rawArg = args[++i];
                } else {
                    rawArg = parameter.defaultValue();
                    map.put(parameter.longName(), parameter.defaultValue());
                }

                var parsedArg = parser.apply(rawArg);
                map.put(parameter.longName(), parsedArg);

                break;
            }
        }

        for (var parameter : params) {
            if (parameter.required() && !map.containsKey(parameter.longName())) {
                throw new IllegalArgumentException("missing required parameter \"" + parameter.longName() + "\" (quotes added)");
            } else if (!parameter.required() && !map.containsKey(parameter.longName())) {
                var parser = PARSERS.get(parameter.type());
                var parsedArg = parser.apply(parameter.defaultValue());
                map.put(parameter.longName(), parsedArg);
            }
        }

        if (!map.containsKey("uri")) {
            throw new IllegalArgumentException("uri is required");
        }

        return map;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public static <T extends CommandBase> @NotNull String help(@NotNull Class<T> commandClass) {
        return String.join(" ", Arrays.stream(getParameters(commandClass))
                .map(parameter -> String.format("\n\t%s, %s\n\t\t%s %s", parameter.shortName(), parameter.longName(), parameter.description(), parameter.required() ? "Required." : "Optional, defaults to " + parameter.defaultValue() + "."))
                .toList());
    }
}
