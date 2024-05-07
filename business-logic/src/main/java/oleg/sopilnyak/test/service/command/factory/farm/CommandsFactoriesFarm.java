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
public class CommandsFactoriesFarm<T extends SchoolCommand> implements CommandsFactory<T> {
    public static final String FARM_BEAN_NAME = "commandFactoriesFarm";
    public static final String NAME = "CommandFactories-Farm";
    // The list of registered factories
    private final List<CommandsFactory<T>> commandsFactories;

    public CommandsFactoriesFarm(final Collection<CommandsFactory<T>> factories) {
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
    public Optional<CommandsFactory<T>> findCommandFactory(final String factoryName) {
        return isNull(factoryName) ? Optional.empty() :
                commandsFactories.stream().filter(factory -> factoryName.equals(factory.getName())).findFirst();
    }

    /**
     * To get the commandIds of registered commands
     *
     * @return commandIds of registered commands
     */
    @Override
    public Collection<String> commandIds() {
        return List.of(
                commandsFactories.stream()
                        .flatMap(factory -> factory.commandIds().stream())
                        .distinct()
                        .toArray(String[]::new));
    }

    /**
     * To get command instance by command-id
     *
     * @param commandId command-id value
     * @return command instance or null if not exists
     * @see SchoolCommand
     * @see Optional
     */
    public T command(String commandId) {
        return commandsFactories.stream()
                .map(factory -> factory.command(commandId)).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    /**
     * To get the name of the commands factory
     *
     * @return value
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * To get the quantity of commands in the factory
     *
     * @return quantity of commands in the factory
     */
    @Override
    public int getSize() {
        return commandsFactories.stream().mapToInt(CommandsFactory::getSize).sum();
    }
}
