package no.clueless.webmention.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.ContentType;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.Plugin;
import no.clueless.oauth.*;
import no.clueless.webmention.persistence.WebmentionRepository;
import no.clueless.webmention.receiver.WebmentionProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * A Javalin plugin acting as a webmention endpoint.
 */
public class WebmentionPlugin extends Plugin<Void> {
    private static final Logger log = LoggerFactory.getLogger(WebmentionPlugin.class);

    @NotNull
    private final String               endpoint;
    @NotNull
    private final WebmentionProcessor  webmentionProcessor;
    @NotNull
    private final WebmentionRepository webmentionRepository;
    private final boolean              testMode;
    @Nullable
    private final Set<String>          testPages;

    public WebmentionPlugin(@Nullable String endpoint, @NotNull WebmentionProcessor webmentionProcessor, @NotNull WebmentionRepository webmentionRepository, boolean testMode, @Nullable Set<String> testPages) {
        this.endpoint             = endpoint == null || endpoint.isBlank() ? "/webmention" : endpoint;
        this.webmentionProcessor  = webmentionProcessor;
        this.webmentionRepository = webmentionRepository;
        this.testMode             = testMode;
        this.testPages            = testPages;
    }

    public WebmentionPlugin(@NotNull String endpoint, @NotNull WebmentionProcessor webmentionProcessor, @NotNull WebmentionRepository webmentionRepository) {
        this(endpoint, webmentionProcessor, webmentionRepository, false, null);
    }

    public WebmentionPlugin(@NotNull WebmentionProcessor webmentionProcessor, @NotNull WebmentionRepository webmentionRepository) {
        this("/webmention", webmentionProcessor, webmentionRepository);
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        OAuthSecurityGuard.requireActiveGuard(config);

        config.router.mount(router -> {
        }).apiBuilder(() -> {
            if (testMode) {
                log.info("Plugin is running in test mode. Test endpoints will be available!");

                get("/test-source-page", ctx -> ctx.status(200).contentType(ContentType.TEXT_HTML).html("""
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <title>Webmention Test Source Page</title>
                            </head>
                            <body>
                                <a href="http://localhost:8080/test-target-page" rel="webmention">This is a link to the test target page.</a>
                            </body>
                        </html>
                        """));

                get("/test-target-page", ctx -> ctx.status(200).header("Link", "</webmention-endpoint>; rel=webmention").contentType(ContentType.TEXT_HTML));

                if (testPages != null)
                    testPages.forEach(page -> {
                        log.info("Test page {} will be available", page);
                        try (var is = getClass().getClassLoader().getResourceAsStream(page)) {
                            if (is == null) {
                                throw new RuntimeException("Test page " + page + " not found");
                            }

                            log.info("Test page {} found", page);
                            var html = new String(is.readAllBytes());
                            get("/" + page, ctx -> ctx.status(200).contentType(ContentType.TEXT_HTML).html(html));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }

            path(endpoint, () -> {
                path("manage", () -> {
                    get(ctx -> {
                        var page                  = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
                        var size                  = ctx.queryParamAsClass("size", Integer.class).getOrDefault(10);
                        var isApproved            = ctx.queryParamAsClass("isApproved", Boolean.class).allowNullable().get();
                        var unapprovedWebmentions = webmentionRepository.getWebmentionsByIsApproved(page, size, "id", "desc", isApproved);
                        ctx.json(unapprovedWebmentions);
                    }, Scope.WEBMENTIONS_MANAGE);

                    patch("publish/{webmentionId}", ctx -> {
                        var webmentionId = ctx.pathParamAsClass("webmentionId", Integer.class).getOrThrow(id -> new BadRequestResponse("webmentionId must be a valid integer"));
                        var webmention   = webmentionRepository.findById(webmentionId).orElseThrow(() -> new NotFoundResponse("Webmention not found"));

                        if (webmention.isApproved()) {
                            throw new ConflictResponse("Webmention is already approved");
                        }

                        webmentionRepository.updateApproval(webmention, true);
                        ctx.status(204);
                    }, Scope.WEBMENTIONS_MANAGE);

                    patch("unpublish/{webmentionId}", ctx -> {
                        var webmentionId = ctx.pathParamAsClass("webmentionId", Integer.class).getOrThrow(id -> new BadRequestResponse("webmentionId must be a valid integer"));
                        var webmention   = webmentionRepository.findById(webmentionId).orElseThrow(() -> new NotFoundResponse("Webmention not found"));

                        if (!webmention.isApproved()) {
                            throw new ConflictResponse("Webmention is already unapproved");
                        }

                        webmentionRepository.updateApproval(webmention, false);
                        ctx.status(204);
                    }, Scope.WEBMENTIONS_MANAGE);

                    delete("{webmentionId}", ctx -> {
                        var webmention = ctx.pathParamAsClass("webmentionId", Integer.class).getOrThrow(id -> new BadRequestResponse("webmentionId must be a valid integer"));
                        webmentionRepository.deleteWebmention(webmention);
                        ctx.status(204);
                    }, Scope.WEBMENTIONS_MANAGE);
                });

                post(ctx -> {
                    var sourceUrl = Optional.ofNullable(ctx.formParam("source")).filter(param -> !param.isBlank()).orElseThrow(() -> new BadRequestResponse("source cannot be null or blank"));
                    var targetUrl = Optional.ofNullable(ctx.formParam("target")).filter(param -> !param.isBlank()).orElseThrow(() -> new BadRequestResponse("target cannot be null or blank"));
                    webmentionProcessor.queue(sourceUrl, targetUrl);
                    ctx.status(202);
                });

                // The specification dictates the parameters are named "source" and "target".
                get(ctx -> {
                    var pageNumber          = ctx.queryParamAsClass("pageNumber", Integer.class).getOrDefault(0);
                    var pageSize            = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(10);
                    var orderByColumn       = Optional.ofNullable(ctx.queryParamAsClass("orderByColumn", String.class)
                            .allowNullable()
                            .check(str -> "id".equals(str) || "name".equals(str) || "message".equals(str) || "timestamp".equals(str), "orderByColumn must be one of: id, name, message, timestamp")
                            .get()).orElseGet(webmentionRepository::getOrderByColumn);
                    var orderByDirection    = ctx.queryParamAsClass("orderByDirection", String.class).getOrDefault(webmentionRepository.getOrderByDirection());
                    var approvedWebmentions = webmentionRepository.getWebmentionsByIsApproved(pageNumber, pageSize, orderByColumn, orderByDirection, true);
                    ctx.json(approvedWebmentions);
                });
            });
        });
    }
}
