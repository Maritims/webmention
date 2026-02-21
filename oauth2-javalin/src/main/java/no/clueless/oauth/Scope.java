package no.clueless.oauth;

import io.javalin.security.RouteRole;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum Scope implements RouteRole {
    CLIENTS_MANAGE("clients:manage"),
    WEBMENTIONS_MANAGE("webmentions:manage");

    private final String label;

    Scope(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label cannot be null or blank");
        }
        this.label = label;
    }

    private static final Set<Scope> ALL = Set.of(values());

    public String getLabel() {
        return label;
    }

    public static Optional<Scope> fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label cannot be null or blank");
        }
        return ALL.stream()
                .filter(s -> s.label.equals(label))
                .findFirst();
    }

    public static Set<Scope> fromLabels(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label cannot be null or blank");
        }
        return Arrays.stream(label.split("\\s+|,"))
                .filter(scope -> !scope.isBlank())
                .map(Scope::fromLabel)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }
}
