package no.clueless.webmention.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.config.NoValueForKeyException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.ContentType;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.Plugin;
import no.clueless.oauth.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebmentionPlugin extends Plugin<WebmentionConfig> {
    private static final Logger log = LoggerFactory.getLogger(WebmentionPlugin.class);

    @NotNull
    @Override
    public String name() {
        return "Clueless Webmention Javalin Plugin";
    }

    public WebmentionPlugin(@Nullable Consumer<WebmentionConfig> userConfig) {
        super(userConfig, new WebmentionConfig());
    }

    @Override
    public void onInitialize(@NotNull JavalinConfig config) {
        try {
            if(!config.pvt.appDataManager.get(OAuthSecurity.GUARD_ACTIVE)) {
                throw new RuntimeException("OAuth security has been explicitly disabled. Please enable OAuth security by adding the OAuthSecurity plugin to your Javalin config");
            }
        }
        catch (NoValueForKeyException e) {
            throw new RuntimeException("OAuth security is not enabled because it has not been configured. Please configure OAuth security by adding the OAuthSecurity plugin to your Javalin config", e);
        }

        config.router.mount(router -> {
        }).apiBuilder(() -> {
            if (pluginConfig.isTestMode()) {
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

                if (pluginConfig.getTestPages() != null)
                    pluginConfig.getTestPages().forEach(page -> {
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

            path(pluginConfig.getEndpoint(), () -> {
                path("pending", () -> {
                    get(ctx -> {
                        var pageNumber            = ctx.queryParamAsClass("pageNumber", Integer.class).getOrDefault(0);
                        var pageSize              = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(10);
                        var webmentionRepository  = pluginConfig.getWebmentionRepository();
                        var unapprovedWebmentions = webmentionRepository.getUnapprovedWebmentions(pageNumber, pageSize);
                        ctx.json(unapprovedWebmentions);
                    }, Scope.WEBMENTIONS_MANAGE);

                    patch("{webmentionId}", ctx -> {
                        var webmentionId = ctx.pathParamAsClass("webmentionId", Integer.class).getOrThrow(id -> new BadRequestResponse("webmentionId must be a valid integer"));
                        var webmention   = pluginConfig.getWebmentionRepository().getById(webmentionId).orElseThrow(() -> new NotFoundResponse("Webmention not found"));

                        if (webmention.isApproved()) {
                            throw new ConflictResponse("Webmention is already approved");
                        }

                        pluginConfig.getWebmentionRepository().approveWebmention(webmention);
                        ctx.status(204);
                    }, Scope.WEBMENTIONS_MANAGE);
                });

                post(ctx -> {
                    var sourceUrl = Optional.ofNullable(ctx.formParam("source")).filter(param -> !param.isBlank()).orElseThrow(() -> new BadRequestResponse("source cannot be null or blank"));
                    var targetUrl = Optional.ofNullable(ctx.formParam("target")).filter(param -> !param.isBlank()).orElseThrow(() -> new BadRequestResponse("target cannot be null or blank"));
                    pluginConfig.getProcessor().queue(sourceUrl, targetUrl);
                    ctx.status(202);
                });

                // The specification dictates the parameters are named "source" and "target".
                get(ctx -> {
                    var pageNumber          = ctx.queryParamAsClass("pageNumber", Integer.class).getOrDefault(0);
                    var pageSize            = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(10);
                    var orderByColumn       = ctx.queryParamAsClass("orderByColumn", String.class).getOrDefault(pluginConfig.getWebmentionRepository().getOrderByColumn());
                    var orderByDirection    = ctx.queryParamAsClass("orderByDirection", String.class).getOrDefault(pluginConfig.getWebmentionRepository().getOrderByDirection());
                    var approvedWebmentions = pluginConfig.getWebmentionRepository().getApprovedWebmentions(pageNumber, pageSize, orderByColumn, orderByDirection);
                    ctx.json(approvedWebmentions);
                });
            });
        });
    }
}
