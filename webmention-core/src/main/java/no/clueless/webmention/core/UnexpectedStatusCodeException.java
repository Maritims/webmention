package no.clueless.webmention.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UnexpectedStatusCodeException extends RuntimeException {
    @NotNull
    private final String url;
    private final int    statusCode;

    public UnexpectedStatusCodeException(@NotNull String url, int statusCode) {
        super(String.format("URL %s returned unexpected status code %d", url, statusCode));
        this.url        = url;
        this.statusCode = statusCode;
    }

    public @NotNull String getUrl() {
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
        return "no.clueless.webmention.core.UnexpectedStatusCodeException{" +
                "url='" + url + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
