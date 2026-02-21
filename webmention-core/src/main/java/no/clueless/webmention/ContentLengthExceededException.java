package no.clueless.webmention;

/**
 * Thrown to indicate that the content length of a webmention response exceeds the configured limit.
 */
public class ContentLengthExceededException extends RuntimeException {
    /**
     * Constructs an instance of ContentLengthExceededException with the given message.
     *
     * @param message the message
     */
    public ContentLengthExceededException(String message) {
        super(message);
    }
}
