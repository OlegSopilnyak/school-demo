package oleg.sopilnyak.test.service.command.factory.farm;

import static java.util.Objects.isNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Container: all commands factories farm
 *
 * @see CommandsFactory
 * @see RootCommand
 */
public class CommandsFactoriesFarm<T extends RootCommand<?>> implements CommandsFactory<T> {
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
        return isNull(factoryName) || factoryName.isBlank() ?
                Optional.empty()
                :
                commandsFactories.stream().filter(factory -> factoryName.equals(factory.getName())).findFirst();
    }

    /**
     * To get sorted ASC command ids of registered commands
     *
     * @return sorted ids of registered commands
     */
    @Override
    public Collection<String> commandIds() {
        return commandsFactories.stream()
                .flatMap(factory -> factory.commandIds().stream())
                .distinct().sorted().toList();
    }

    /**
     * To get command instance by command-id
     *
     * @param commandId command-id value
     * @return command instance or null if not exists
     * @see RootCommand
     * @see Optional
     */
    public T command(String commandId) {
        return commandsFactories.stream()
                .map(factory -> factory.command(commandId))
                .filter(Objects::nonNull).findFirst()
                .orElse(null);
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
