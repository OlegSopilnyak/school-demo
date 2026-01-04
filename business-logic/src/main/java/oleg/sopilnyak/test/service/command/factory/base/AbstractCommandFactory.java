package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract commands factory
 *
 * @param <T> type of commands in the factory
 */
public abstract class AbstractCommandFactory<T extends RootCommand<?>> implements CommandsFactory<T> {
    private final Map<String, T> commandMap = new HashMap<>();

    /**
     * To accept commands collection to the factory
     *
     * @param commands commands collection
     */
    protected void applyFactoryCommands(Collection<T> commands) {
        this.commandMap.clear();
        this.commandMap.putAll(
                commands.stream().collect(Collectors.toMap(RootCommand::getId, Function.identity()))
        );
    }

    /**
     * To get sorted ASC command ids of registered commands
     *
     * @return sorted ids of registered commands
     */
    @Override
    public Collection<String> commandIds() {
        return commandMap.keySet().stream().sorted().toList();
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
}
