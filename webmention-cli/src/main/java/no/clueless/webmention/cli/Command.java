package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Command extends Runnable {
    class Parameter<T> {
        private final @NotNull  String              longName;
        private final @NotNull  String              shortName;
        private final @NotNull  String              description;
        private final           boolean             required;
        private final           boolean             requiresValue;
        private final @Nullable T                   defaultValue;
        private final @NotNull  Function<String, T> parser;
        private final @Nullable Predicate<Object>   validator;

        public Parameter(@NotNull String longName,
                         @NotNull String shortName,
                         @NotNull String description,
                         boolean required,
                         boolean requiresValue,
                         @Nullable T defaultValue,
                         @NotNull Function<String, T> parser,
                         @Nullable Predicate<Object> validator) {
            this.longName      = longName;
            this.shortName     = shortName;
            this.description   = description;
            this.required      = required;
            this.requiresValue = requiresValue;
            this.defaultValue  = defaultValue;
            this.parser        = parser;
            this.validator     = validator;
        }

        public @NotNull String longName() {
            return longName;
        }

        public @NotNull String shortName() {
            return shortName;
        }

        public @NotNull String description() {
            return description;
        }

        public boolean required() {
            return required;
        }

        public boolean requiresValue() {
            return requiresValue;
        }

        public @Nullable T defaultValue() {
            return defaultValue;
        }

        public @NotNull Function<String, T> parser() {
            return parser;
        }

        public @Nullable Predicate<Object> validator() {
            return validator;
        }
    }

    class URIParameter extends Parameter<URI> {
        public URIParameter(@NotNull String longName, @NotNull String shortName, @NotNull String description, boolean required, boolean requiresValue) {
            super(longName, shortName, description, required, requiresValue, null, URI::create, uri -> ((URI) uri).getScheme() != null && ((URI) uri).getScheme().startsWith("http"));
        }

        public static URIParameter required(@NotNull String longName, @NotNull String shortName, @NotNull String description) {
            return new URIParameter(longName, shortName, description, true, true);
        }
    }

    interface Factory<T extends Command> {
        class FactoryException extends Exception {
            private final String[] args;

            public FactoryException(@NotNull String message, String[] args) {
                super(message);
                this.args = args;
            }

            public String[] getArgs() {
                return args;
            }
        }

        @NotNull List<Parameter<?>> parameters();

        @NotNull
        default Map<String, Object> getArgs(@NotNull String[] args) throws FactoryException {
            var map = new HashMap<String, Object>();

            for (var i = 0; i < args.length; i++) {
                var cleanArg = args[i].replaceAll("^--", "").replaceAll("^-", "");

                for (var parameter : parameters()) {
                    if (!cleanArg.equalsIgnoreCase(parameter.shortName()) && !cleanArg.equalsIgnoreCase(parameter.longName())) {
                        continue;
                    }

                    if (parameter.requiresValue()) {
                        if (i == args.length - 1) {
                            throw new FactoryException("missing value for parameter \"" + parameter.longName() + "\" (quotes added)", args);
                        }

                        var rawArg = args[++i];

                        Object parsedArg;
                        try {
                            parsedArg = parameter.parser().apply(rawArg);
                        } catch (Exception e) {
                            throw new FactoryException("argument \"" + rawArg + "\" (quotes added) could not be parsed", args);
                        }

                        if (parameter.validator() != null && !Objects.requireNonNull(parameter.validator()).test(parsedArg)) {
                            throw new FactoryException("argument \"" + rawArg + "\" (quotes added) was successfully parsed, but failed validation", args);
                        }

                        map.put(parameter.longName(), parsedArg);
                    } else {
                        map.put(parameter.longName(), parameter.defaultValue());
                    }

                    break;
                }
            }

            for (var parameter : parameters()) {
                if (parameter.required() && !map.containsKey(parameter.longName())) {
                    throw new FactoryException("missing required parameter \"" + parameter.longName() + "\" (quotes added)", args);
                } else if (!parameter.required() && !map.containsKey(parameter.longName())) {
                    map.put(parameter.longName(), parameter.defaultValue());
                }
            }

            if (!map.containsKey("uri")) {
                throw new FactoryException("uri is required", args);
            }

            return map;
        }

        @NotNull
        T createCommand(@NotNull String[] args) throws FactoryException;

        @NotNull
        String description();

        default @NotNull String help() {
            return description() + String.join(" ", parameters().stream()
                    .map(parameter -> String.format("\n\t%s, %s\n\t\t%s %s", parameter.shortName(), parameter.longName(), parameter.description(), parameter.required ? "Required." : "Optional, defaults to " + parameter.defaultValue + "."))
                    .toList());
        }
    }

    String name();
}
