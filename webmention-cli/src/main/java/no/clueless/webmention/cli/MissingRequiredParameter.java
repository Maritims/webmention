package no.clueless.webmention.cli;

public class MissingRequiredParameter extends Exception {
    private final String commandName;
    private final String parameterName;

    public MissingRequiredParameter(String commandName, String parameterName) {
        super(String.format("Missing required parameter: %s for command: %s", parameterName, commandName));
        this.commandName   = commandName;
        this.parameterName = parameterName;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getParameterName() {
        return parameterName;
    }
}
