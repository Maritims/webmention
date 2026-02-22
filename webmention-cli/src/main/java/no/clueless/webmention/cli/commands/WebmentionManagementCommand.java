package no.clueless.webmention.cli.commands;

import no.clueless.webmention.api_client.Pagination;
import no.clueless.webmention.api_client.WebmentionManagementApiClient;
import no.clueless.webmention.cli.Command;
import no.clueless.webmention.cli.CommandResult;
import no.clueless.webmention.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.*;

/**
 * Gets pending webmentions from a webmention endpoint.
 */
public class WebmentionManagementCommand implements Command {
    private static final Logger               log         = org.slf4j.LoggerFactory.getLogger(WebmentionManagementCommand.class);
    @NotNull
    private final        Map<String, Command> subcommands = new HashMap<>();
    @Nullable
    private final        String               webmentionApiEndpoint;

    public WebmentionManagementCommand() {
        subcommands.put("get-webmentions", this::getWebmentions);
        subcommands.put("publish-command", this::publishWebmention);
        subcommands.put("unpublish-command", this::unpublishWebmention);
        subcommands.put("delete-command", this::deleteWebmention);
        webmentionApiEndpoint = System.getenv("WEBMENTION_API_ENDPOINT");
    }

    @NotNull
    private String readInput(@NotNull String label, @NotNull Scanner reader, boolean allowEmptyInput) {
        String input = null;
        while (input == null || input.isBlank()) {
            System.out.print("Enter " + label + ": ");
            input = reader.nextLine();
            if (allowEmptyInput || (input != null && !input.isBlank())) {
                return input;
            }
            System.err.println(label + " cannot be empty. Please try again.");
        }
        return input;
    }

    @NotNull
    List<Webmention> getWebmentions(@NotNull URI uri, int page, int size, @Nullable Boolean isApproved) {
        try (var httpClient = HttpClient.newBuilder().build()) {
            var webmentionManagementApiClient = new WebmentionManagementApiClient(uri, httpClient);
            return webmentionManagementApiClient.getWebmentions(new Pagination(page, size), isApproved);
        }
    }

    @NotNull
    CommandResult getWebmentions(@NotNull String[] args) {
        URI     uri        = null;
        var     page       = 0;
        var     size       = 10;
        Boolean isApproved = null;

        for (var i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--uri", "-u" -> {
                    if (++i < args.length) {
                        uri = URI.create(args[i]);
                        log.debug("Setting uri to: {}", uri);
                    }
                }
                case "--page", "-p" -> {
                    if (++i < args.length) {
                        page = Integer.parseInt(args[i]);
                        log.debug("Setting page to: {}", page);
                    }
                }
                case "--size", "-s" -> {
                    if (++i < args.length) {
                        size = Integer.parseInt(args[i]);
                        log.debug("Setting size to: {}", size);
                    }
                }
                case "--approved", "-a" -> {
                    if (++i < args.length) {
                        isApproved = Boolean.parseBoolean(args[i]);
                        log.debug("Setting isApproved to: {}", isApproved);
                    }
                }
                default -> {
                    return CommandResult.UNKNOWN_ARGUMENT;
                }
            }
        }

        if (uri == null) {
            return CommandResult.MISSING_REQUIRED_ARGUMENT;
        }

        var reader                = new Scanner(System.in);
        var webmentionApiEndpoint = Optional.ofNullable(this.webmentionApiEndpoint).filter(value -> !value.isBlank()).orElseGet(() -> Optional.of(readInput("API endpoint (leave empty for default: /webmention/manage)", reader, true)).filter(value -> !value.isBlank()).orElse("/webmention/manage"));
        var webmentions           = getWebmentions(uri.resolve(webmentionApiEndpoint), page, size, isApproved);
        webmentions.forEach(webmention -> log.info("Webmention #{}: {} -> {} (isApproved: {})", webmention.id(), webmention.sourceUrl(), webmention.targetUrl(), webmention.isApproved()));

        return CommandResult.SUCCESS;
    }

    @NotNull
    CommandResult publishWebmention(@NotNull String[] args) {
        return CommandResult.SUCCESS;
    }

    @NotNull
    CommandResult unpublishWebmention(@NotNull String[] args) {
        return CommandResult.SUCCESS;
    }

    @NotNull
    CommandResult deleteWebmention(@NotNull String[] args) {
        return CommandResult.SUCCESS;
    }

    @Override
    public @NotNull CommandResult execute(@NotNull String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("args must contain at least 2 elements");
        }

        var label   = args[1];
        var command = subcommands.get(label);
        return command == null ? CommandResult.UNKNOWN_ARGUMENT : command.execute(args);
    }
}
