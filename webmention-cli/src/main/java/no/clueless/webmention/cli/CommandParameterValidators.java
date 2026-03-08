package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class CommandParameterValidators {
    static class BooleanValidator implements Predicate<String> {
        @Override
        public boolean test(String string) {
            return string.equalsIgnoreCase("true") || string.equalsIgnoreCase("false");
        }
    }

    static class IntegerValidator implements Predicate<String> {
        @Override
        public boolean test(String string) {
            try {
                Integer.parseInt(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    static class PathValidator implements Predicate<String> {
        @Override
        public boolean test(String string) {
            return Path.of(string).toFile().exists();
        }
    }

    static class URIValidator implements Predicate<String> {
        @Override
        public boolean test(String s) {
            return s.matches("^https?://.*");
        }
    }

    private static final ConcurrentHashMap<Class<?>, Predicate<String>> validators = new ConcurrentHashMap<>();

    @NotNull
    public static Predicate<String> getValidator(@NotNull Class<?> type) {
        return validators.computeIfAbsent(type, t -> switch (t) {
            case Class<?> c when c == Boolean.class -> new BooleanValidator();
            case Class<?> c when c == Integer.class -> new IntegerValidator();
            case Class<?> c when c == Path.class -> new PathValidator();
            case Class<?> c when c == URI.class -> new URIValidator();
            default -> throw new IllegalArgumentException("No validator for type " + t.getName());
        });
    }
}
