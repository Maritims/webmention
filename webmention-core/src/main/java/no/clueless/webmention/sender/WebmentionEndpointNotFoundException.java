package no.clueless.webmention.sender;

import java.util.Objects;

public class WebmentionEndpointNotFoundException extends Exception {
    private final String url;

    public WebmentionEndpointNotFoundException(String url) {
        super(String.format("No webmention endpoint was referenced in the response from URL %s", url));
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WebmentionEndpointNotFoundException that = (WebmentionEndpointNotFoundException) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    @Override
    public String toString() {
        return "no.clueless.webmention.sender.WebmentionEndpointNotFoundException{" +
                "url='" + url + '\'' +
                '}';
    }
}
