package no.clueless.webmention.receiver;

import no.clueless.webmention.WebmentionException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

@FunctionalInterface
public interface WebmentionTargetVerifier {
    boolean verify(@NotNull URI targetUri) throws WebmentionException;
}
