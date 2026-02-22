package no.clueless.webmention;

import org.jetbrains.annotations.NotNull;

public class WebmentionException extends Exception {
    public WebmentionException(@NotNull String message) {
        super(message);
    }

    public WebmentionException(@NotNull String message, @NotNull Exception innerException) {
        super(message, innerException);
    }
}
