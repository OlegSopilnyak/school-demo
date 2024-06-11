package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract commands factory
 *
 * @param <T> type of commands in the factory
 */
public abstract class AbstractCommandFactory<T extends RootCommand> implements CommandsFactory<T> {
    private final Map<String, T> commandMap = new HashMap<>();

    /**
     * To apply commands collection to the factory
     *
     * @param commands commands collection
     */
    protected void applyFactoryCommands(Collection<T> commands) {
        this.commandMap.clear();
        this.commandMap.putAll(commands.stream().collect(Collectors.toMap(RootCommand::getId, Function.identity())));
    }

    /**
     * To get the commandIds of registered commands
     *
     * @return commandIds of registered commands
     */
    @Override
    public Collection<String> commandIds() {
        return List.of(commandMap.keySet().stream().sorted().toArray(String[]::new));
    }

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance or null if not registered
     * @see RootCommand
     * @see RootCommand#getId()
     */
    @Override
    public T command(String commandId) {
        return commandMap.get(commandId);
    }

    /**
     * To get the quantity of commands in the factory
     *
     * @return quantity of commands in the factory
     */
    @Override
    public int getSize() {
        return commandMap.size();
    }
}
