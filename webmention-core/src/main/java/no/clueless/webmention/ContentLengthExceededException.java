package no.clueless.webmention;

public class ContentLengthExceededException extends RuntimeException {
    public ContentLengthExceededException(String message) {
        super(message);
    }
}
