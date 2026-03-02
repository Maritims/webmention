package no.clueless.webmention.cli;

public class Application {
    public static void main(String[] args) {
        var registry = new CommandRegistry()
                .register("get-webmentions", new GetWebmentionsCommand.Factory())
                .register("send-webmentions", new SendWebmentionsCommand.Factory())
                .register("publish-webmention", new PublishWebmentionCommand.Factory())
                .register("unpublish-webmention", new UnpublishWebmentionCommand.Factory())
                .register("delete-webmention", new DeleteWebmentionCommand.Factory());

        new CommandProcessor(registry).run(args);
    }
}
