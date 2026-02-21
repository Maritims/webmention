package no.clueless.webmention.cli.commands;

import no.clueless.webmention.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Find and send webmentions from a given directory.
 */
public class FindAndSendWebmentions implements Command {
    private static final Logger log = LoggerFactory.getLogger(FindAndSendWebmentions.class);

    /**
     * Default constructor.
     */
    public FindAndSendWebmentions() {}

    public CommandResult execute(String[] args) {
        String uriStr      = null;
        String dirStr      = null;
        var    dryRun      = false;
        var    restrictive = false;

        for (var i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--uri", "-u" -> {
                    if (++i < args.length) {
                        uriStr = args[i];
                        log.debug("Setting uri to: {}", uriStr);
                    }
                }
                case "--dir", "-d" -> {
                    if (++i < args.length) {
                        dirStr = args[i];
                        log.debug("Setting dir to: {}", dirStr);
                    }
                }
                case "--dry-run", "-dr" -> {
                    dryRun = true;
                    log.debug("Dry run enabled");
                }
                case "--restrictive", "-r" -> {
                    restrictive = true;
                    log.debug("Restrictive mode enabled");
                }
                default -> {
                    return CommandResult.UNKNOWN_ARGUMENT;
                }
            }
        }

        if (uriStr == null || dirStr == null) {
            log.error("Both --uri and --dir are required");
        } else {
            try {
                final var rootDir = Path.of(dirStr);
                final var baseUri = new URI(uriStr);

                new WebmentionCli().findAndSendWebmentions(baseUri, rootDir, dryRun, restrictive);
            } catch (URISyntaxException e) {
                log.error("Invalid URI: {}", uriStr);
            } catch (FileNotFoundException e) {
                log.error("Directory does not exist: {}", dirStr);
            } catch (IOException e) {
                log.error("Error reading directory: {}", dirStr, e);
            }
        }
        return null;
    }
}
