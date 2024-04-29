package oleg.sopilnyak.test.service.command.factory.farm;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.*;

import static java.util.Objects.isNull;

/**
 * Container: all commands factories farm
 *
 * @see CommandsFactory
 * @see SchoolCommand
 */
public class CommandsFactoriesFarm {
    // The list of registered factories
    private final List<CommandsFactory<?>> commandsFactories;

    public CommandsFactoriesFarm(final Collection<CommandsFactory<?>> factories) {
        commandsFactories = new LinkedList<>(factories);
    }

    /**
     * To find factory by name
     *
     * @param factoryName the name of the commands factory
     * @return found factory or empty
     * @see CommandsFactory
     * @see Optional
     */
    public Optional<CommandsFactory<?>> findCommandFactory(final String factoryName) {
        return isNull(factoryName) ? Optional.empty() :
                commandsFactories.stream().filter(factory -> factoryName.equals(factory.getName())).findFirst();
    }

    /**
     * To get command instance by command-id
     *
     * @param commandId command-id value
     * @return command instance or empty
     * @see SchoolCommand
     * @see Optional
     */
    public Optional<SchoolCommand<?>> command(String commandId) {
        final SchoolCommand<?> command = commandsFactories.stream()
                .map(factory -> factory.command(commandId)).filter(Objects::nonNull)
                .findFirst().orElse(null);
        return Optional.ofNullable(command);
    }
}
