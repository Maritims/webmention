package no.clueless.webmention;

public class WebmentionException extends Exception {
    public WebmentionException(String message) {
        super(message);
    }

    public WebmentionException(String message, Exception innerException) {
        super(message, innerException);
    }
}
