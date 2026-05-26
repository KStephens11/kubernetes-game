package com.k8sgame.model;

/**
 * Result of executing a kubectl or game command.
 *
 * @param success whether the command executed without error
 * @param output  the text output to display in the terminal
 */
public record CommandResponse(boolean success, String output) {

    public static CommandResponse success(String output) {
        return new CommandResponse(true, output);
    }

    public static CommandResponse error(String message) {
        return new CommandResponse(false, message);
    }
}
