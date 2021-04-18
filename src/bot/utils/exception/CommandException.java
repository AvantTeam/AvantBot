package bot.utils.exception;

import java.io.*;

import bot.content.*;

public class CommandException extends IllegalArgumentException {
    private static final @Serial long serialVersionUID = 1L;

    public final Command command;
    
    public CommandException(Command command, String message) {
        super(message);
        this.command = command;
    }
}
