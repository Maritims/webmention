package no.clueless.webmention.receiver;

import no.clueless.webmention.WebmentionException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;

public class WebmentionRequestVerifier {
    private static final Set<String>              SUPPORTED_SCHEMES = Set.of("http", "https");
    private final        WebmentionTargetVerifier webmentionTargetVerifier;

    public WebmentionRequestVerifier(WebmentionTargetVerifier webmentionTargetVerifier) {
        this.webmentionTargetVerifier = Objects.requireNonNull(webmentionTargetVerifier, "targetVerifier cannot be null");
    }

    private void validateScheme(URI uri) throws WebmentionException {
        var scheme = uri.getScheme();
        if (scheme == null || !SUPPORTED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new WebmentionException("The scheme of URI " + uri + " is not supported");
        }
    }

    private URI stripFragment(URI uri) throws URISyntaxException {
        return uri.getFragment() == null ? uri : new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
    }

    public boolean verify(String source, String target) throws WebmentionException {
        if (source == null) {
            throw new WebmentionException("Source URL cannot be null");
        }
        if (target == null) {
            throw new WebmentionException("Target URL cannot be null");
        }

        try {
            var sourceUri = new URI(source);
            var targetUri = new URI(target);

            validateScheme(sourceUri);
            validateScheme(targetUri);

            if (sourceUri.equals(targetUri)) {
                throw new WebmentionException("Source and targetUrl URLs cannot be equal");
            }

            if (!webmentionTargetVerifier.verify(stripFragment(targetUri))) {
                throw new WebmentionException("Target URL " + targetUri + " is not a valid resource for this receiver");
            }

            return true;
        } catch (URISyntaxException e) {
            throw new WebmentionException("Invalid URL format: " + e.getMessage(), e);
        }
    }

    public static class Builder {
        private WebmentionTargetVerifier targetVerifier;

        private Builder() {}

        public Builder targetVerifier(WebmentionTargetVerifier targetVerifier) {
            this.targetVerifier = targetVerifier;
            return this;
        }

        public WebmentionRequestVerifier build() {
            return new WebmentionRequestVerifier(targetVerifier);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
