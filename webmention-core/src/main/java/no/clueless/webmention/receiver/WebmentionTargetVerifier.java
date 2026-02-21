package no.clueless.webmention.receiver;

import no.clueless.webmention.WebmentionException;

import java.net.URI;

/**
 * An interface for verifying that a target URI is valid.
 */
@FunctionalInterface
public interface WebmentionTargetVerifier {
    /**
     * Verify that the target URI is valid.
     *
     * @param targetUri the target URI to verify
     * @return true if the target URI is valid, false otherwise
     * @throws WebmentionException if the verification failed
     */
    boolean verify(URI targetUri) throws WebmentionException;
}
