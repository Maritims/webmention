package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Application {
    @NotNull
    private final CommandProcessor processor;

    public Application(@NotNull CommandProcessor processor) {
        this.processor = processor;
    }

    protected void handleEmptyArgs() {
        System.out.println("webmention-cli: try 'webmention-cli help' for more information");
        System.exit(2);
    }

    public void run(String[] args) {
        if (args.length <= 1) {
            handleEmptyArgs();
            return;
        }

        processor.run(Arrays.copyOfRange(args, 1, args.length));
    }

    public static void main(String[] args) {
        var registry = new CommandRegistry()
                .register("get-webmentions", new GetWebmentionsCommand.Factory())
                .register("send-webmentions", new SendWebmentionsCommand.Factory())
                .register("publish-webmention", new PublishWebmentionCommand.Factory())
                .register("unpublish-webmention", new UnpublishWebmentionCommand.Factory())
                .register("delete-webmention", new DeleteWebmentionCommand.Factory());

        registry.register("help", new Command.Factory<>() {
            @Override
            public @NotNull List<Command.Parameter<?>> parameters() {
                return List.of();
            }

            @Override
            public @NotNull Command createCommand(@NotNull String[] args) {
                return new Command.CommandBase("", registry::printHelp) {
                };
            }

            @Override
            public @NotNull String description() {
                return "";
            }
        });

        var processor = new CommandProcessor(registry);

        new Application(processor).run(args);
    }
}
