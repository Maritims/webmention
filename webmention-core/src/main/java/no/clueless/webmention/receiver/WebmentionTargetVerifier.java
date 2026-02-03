package no.clueless.webmention.receiver;

import no.clueless.webmention.WebmentionException;

import java.net.URI;

@FunctionalInterface
public interface WebmentionTargetVerifier {
    boolean verify(URI targetUri) throws WebmentionException;
}
