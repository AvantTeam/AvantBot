package com.github.avant.bot.utils.exception;

import java.io.*;

import com.github.avant.bot.content.*;

public class CommandException extends IllegalArgumentException {
    private static final @Serial long serialVersionUID = 1L;

    public final Command command;
    
    public CommandException(Command command, String message) {
        super(message);
        this.command = command;
    }
}
