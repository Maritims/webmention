package no.clueless.webmention_javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ContentType;
import io.javalin.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

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
        config.router.mount(router -> {
            if (pluginConfig.isTestMode()) {
                log.info("Plugin is running in test mode. Test endpoints will be available!");

                router.get("/test-source-page", ctx -> ctx.status(200).contentType(ContentType.TEXT_HTML).html("""
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
                router.get("/test-target-page", ctx -> ctx.status(200).header("Link", "</webmention-endpoint>; rel=webmention").contentType(ContentType.TEXT_HTML));
            }

            // The specification dictates the parameters are named "source" and "target".
            router.post(pluginConfig.getEndpoint(), ctx -> {
                var sourceUrl = Optional.ofNullable(ctx.formParam("source")).filter(param -> !param.isBlank()).orElseThrow(() -> new BadRequestResponse("source cannot be null or blank"));
                var targetUrl = Optional.ofNullable(ctx.formParam("target")).filter(param -> !param.isBlank()).orElseThrow(() -> new BadRequestResponse("target cannot be null or blank"));
                pluginConfig.getProcessor().queue(sourceUrl, targetUrl);
                ctx.status(202);
            });

            router.get(pluginConfig.getEndpoint(), ctx -> {
                var pageNumber          = ctx.queryParamAsClass("pageNumber", Integer.class).getOrDefault(0);
                var pageSize            = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(10);
                var orderByColumn       = ctx.queryParamAsClass("orderByColumn", String.class).getOrDefault(pluginConfig.getWebmentionRepository().getOrderByColumn());
                var orderByDirection    = ctx.queryParamAsClass("orderByDirection", String.class).getOrDefault(pluginConfig.getWebmentionRepository().getOrderByDirection());
                var approvedWebmentions = pluginConfig.getWebmentionRepository().getApprovedWebmentions(pageNumber, pageSize, orderByColumn, orderByDirection);
                ctx.json(approvedWebmentions);
            });
        });
    }
}
