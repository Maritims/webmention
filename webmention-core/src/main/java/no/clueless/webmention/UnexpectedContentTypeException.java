package no.clueless.webmention;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class UnexpectedContentTypeException extends Exception {
    @Nullable
    private final String url;
    @Nullable
    private final String contentType;

    public UnexpectedContentTypeException(@NotNull String url, @Nullable String contentType) {
        super(contentType == null ? String.format("The URL %s did not return any Content-Type header", url) : String.format("The URL %s returned Content-Type header with the unexpected value %s", url, contentType));
        this.url         = url;
        this.contentType = contentType;
    }

    public UnexpectedContentTypeException(@NotNull String message) {
        super(message);
        this.url         = null;
        this.contentType = null;
    }

    public @Nullable String getUrl() {
        return url;
    }

    public @Nullable String getContentType() {
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
