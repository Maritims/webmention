package no.clueless.oauth.javalin;

import io.javalin.security.RouteRole;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum Scope implements RouteRole {
    CLIENTS_MANAGE("clients:manage"),
    WEBMENTIONS_MANAGE("webmentions:manage");

    @NotNull
    private final String label;

    Scope(@NotNull String label) {
        if (label.isBlank()) {
            throw new IllegalArgumentException("label cannot be blank");
        }
        this.label = label;
    }

    private static final Set<Scope> ALL = Set.of(values());

    public @NotNull String getLabel() {
        return label;
    }

    @NotNull
    public static Optional<Scope> fromString(@NotNull String str) {
        if (str.isBlank()) {
            throw new IllegalArgumentException("label cannot be blank");
        }
        return ALL.stream()
                .filter(s -> s.label.equals(str))
                .findFirst();
    }

    @NotNull
    public static Set<Scope> fromLabels(@NotNull String labels) {
        if (labels.isBlank()) {
            throw new IllegalArgumentException("labels cannot be blank");
        }
        return Arrays.stream(labels.split("\\s+|,"))
                .filter(scope -> !scope.isBlank())
                .map(Scope::fromString)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }
}
