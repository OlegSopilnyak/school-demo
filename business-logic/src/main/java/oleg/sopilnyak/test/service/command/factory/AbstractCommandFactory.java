package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract commands factory
 */
public abstract class AbstractCommandFactory implements CommandsFactory {
    private final Map<String, SchoolCommand> commandMap = new HashMap<>();

    /**
     * To apply commands collection to the factory
     *
     * @param commands commands collection
     */
    protected void applyFactoryCommands(Collection<? extends SchoolCommand<?>> commands) {
        this.commandMap.putAll(commands.stream().collect(Collectors.toMap(SchoolCommand::getId, Function.identity())));
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
    public <T> SchoolCommand<T> command(String commandId) {
        return commandMap.get(commandId);
    }
}
