package no.clueless.webmention;

import org.jetbrains.annotations.NotNull;

public class ContentLengthExceededException extends RuntimeException {
    public ContentLengthExceededException(@NotNull String message) {
        super(message);
    }
}
