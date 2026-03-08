package no.clueless.webmention.core.receiver;

import no.clueless.webmention.core.WebmentionException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class WebmentionRequestVerifier {
    private static final Set<String>              SUPPORTED_SCHEMES = Set.of("http", "https");
    @NotNull
    private final        WebmentionTargetVerifier webmentionTargetVerifier;

    public WebmentionRequestVerifier(@NotNull WebmentionTargetVerifier webmentionTargetVerifier) {
        this.webmentionTargetVerifier = webmentionTargetVerifier;
    }

    private void validateScheme(@NotNull URI uri) throws WebmentionException {
        var scheme = uri.getScheme();
        if (scheme == null || !SUPPORTED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new WebmentionException("The scheme of URI " + uri + " is not supported");
        }
    }

    @NotNull
    private URI stripFragment(@NotNull URI uri) throws URISyntaxException {
        return uri.getFragment() == null ? uri : new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
    }

    public boolean verify(@NotNull String source, @NotNull String target) throws WebmentionException {
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
}
