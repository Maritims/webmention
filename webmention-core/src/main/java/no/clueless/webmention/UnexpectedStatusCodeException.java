package no.clueless.webmention;

import java.util.Objects;

public class UnexpectedStatusCodeException extends RuntimeException {
    private final String url;
    private final int    statusCode;

    public UnexpectedStatusCodeException(String url, int statusCode) {
        super(String.format("URL %s returned unexpected status code %d", url, statusCode));
        this.url        = url;
        this.statusCode = statusCode;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnexpectedStatusCodeException that = (UnexpectedStatusCodeException) o;
        return statusCode == that.statusCode && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, statusCode);
    }

    @Override
    public String toString() {
        return "no.clueless.webmention.UnexpectedStatusCodeException{" +
                "url='" + url + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
