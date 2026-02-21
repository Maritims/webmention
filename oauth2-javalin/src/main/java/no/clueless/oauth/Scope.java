package no.clueless.oauth;

import io.javalin.security.RouteRole;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth scopes.
 */
public enum Scope implements RouteRole {
    /**
     * This scope permits managing clients.
     */
    CLIENTS_MANAGE("clients:manage"),
    /**
     * This scope permits managing webmentions.
     */
    WEBMENTIONS_MANAGE("webmentions:manage");

    /**
     * The scope label, used in the OAuth token.
     */
    private final String label;

    /**
     * Constructor.
     *
     * @param label the scope label
     * @throws IllegalArgumentException if the label is null or blank
     */
    Scope(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label cannot be null or blank");
        }
        this.label = label;
    }

    private static final Set<Scope> ALL = Set.of(values());

    /**
     * Returns the scope label.
     *
     * @return the scope label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Resolves a scope from the given label.
     *
     * @param label the label
     * @return the scope
     * @throws IllegalArgumentException if label is null or blank
     */
    public static Optional<Scope> fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label cannot be null or blank");
        }
        return ALL.stream()
                .filter(s -> s.label.equals(label))
                .findFirst();
    }

    /**
     * Resolves scopes from the given labels.
     *
     * @param labels the labels
     * @return the scopes
     * @throws IllegalArgumentException if labels is null or blank
     */
    public static Set<Scope> fromLabels(String labels) {
        if (labels == null || labels.isBlank()) {
            throw new IllegalArgumentException("labels cannot be null or blank");
        }
        return Arrays.stream(labels.split("\\s+|,"))
                .filter(scope -> !scope.isBlank())
                .map(Scope::fromLabel)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }
}
