package no.clueless.webmention.cli;

public class InvalidParameterValueException extends Exception {
    private final String commandName;
    private final String parameterName;

    public InvalidParameterValueException(String commandName, String parameterName) {
        super(String.format("Invalid value for parameter: %s for command: %s", parameterName, commandName));
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
