package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract commands factory
 */
public abstract class AbstractCommandFactory<T> implements CommandsFactory<T> {
    private final Map<String, SchoolCommand<T>> commandMap = new HashMap<>();

    /**
     * To apply commands collection to the factory
     *
     * @param commands commands collection
     */
    protected void applyFactoryCommands(Collection<? extends SchoolCommand<T>> commands) {
        this.commandMap.clear();
        this.commandMap.putAll(commands.stream().collect(Collectors.toMap(SchoolCommand::getId, Function.identity())));
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
     * @see SchoolCommand
     * @see SchoolCommand#getId()
     */
    @Override
    public SchoolCommand<T> command(String commandId) {
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
