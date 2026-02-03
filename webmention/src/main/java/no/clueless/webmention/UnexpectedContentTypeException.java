package no.clueless.webmention;

import java.util.Objects;

public class UnexpectedContentTypeException extends Exception {
    private final String url;
    private final String contentType;

    public UnexpectedContentTypeException(String url, String contentType) {
        super(contentType == null ? String.format("The URL %s did not return any Content-Type header", url) : String.format("The URL %s returned Content-Type header with the unexpected value %s", url, contentType));
        this.url         = url;
        this.contentType = contentType;
    }

    public UnexpectedContentTypeException(String message) {
        super(message);
        this.url         = null;
        this.contentType = null;
    }

    public String getUrl() {
        return url;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnexpectedContentTypeException that = (UnexpectedContentTypeException) o;
        return Objects.equals(url, that.url) && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, contentType);
    }

    @Override
    public String toString() {
        return "no.clueless.webmention.UnexpectedContentTypeException{" +
                "url='" + url + '\'' +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
