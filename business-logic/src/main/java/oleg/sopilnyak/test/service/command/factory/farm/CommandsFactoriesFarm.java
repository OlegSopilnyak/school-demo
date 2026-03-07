package oleg.sopilnyak.test.service.command.factory.farm;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container: all commands factories farm
 *
 * @param <T> the type of root command
 *
 * @see CommandsFactory
 * @see RootCommand
 */
public class CommandsFactoriesFarm<T extends RootCommand<?>> implements CommandsFactory<T> {
    public static final String FARM_BEAN_NAME = "commandFactoriesFarm";
    public static final String NAME = "CommandFactories-Farm";
    // the map of registered commands factories
    private final Map<Class<T>, CommandsFactory<T>> commandsFactoriesMap = new ConcurrentHashMap<>();

    public CommandsFactoriesFarm(Collection<CommandsFactory<T>> factories) {
        factories.forEach(this::register);
    }

    /**
     * To register/re-register commands factory in the farm
     *
     * @param factory to register/re-register
     */
    public void register(final CommandsFactory<T> factory) {
        commandsFactoriesMap.put(factory.commandFamily(), factory);
    }

    /**
     * The class of commands family, the commands are belonged to
     *
     * @return command family class value
     * @see RootCommand#commandFamily()
     */
    @Override
    @SuppressWarnings("unchecked")
    public <F extends RootCommand> Class<F> commandFamily() {
        return (Class<F>) RootCommand.class;
    }

    /**
     * To get command instance by command-id
     *
     * @param commandId command-id value
     * @return command instance or null if not exists
     * @see RootCommand
     * @see Optional
     */
    @Override
    public T command(String commandId) {
        return commandsFactoriesMap.values().stream()
                .map(factory -> factory.command(commandId))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * To make command execution context by command-id and input
     *
     * @param commandId command-id
     * @param input     input parameter for the command execution
     * @param <I>       type of input parameter
     * @param <R>       type of command result
     * @return command context or empty if not registered
     * @see CommandsFactory#command(String)
     * @see RootCommand#createContext(Input)
     * @see Context
     */
    @Override
    @SuppressWarnings("unchecked")
    public <I, R> Context<R> makeCommandContext(final String commandId, final Input<I> input) {
        return (Context<R>) commandsFactoriesMap.values().stream()
                .map(factory -> factory.makeCommandContext(commandId, input))
                .filter(Objects::nonNull).findFirst().orElse(null);
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
                commandsFactoriesMap.values().stream()
                        .filter(factory -> Objects.equals(factoryName, factory.getName()))
                        .findFirst();
    }

    /**
     * To get sorted ASC command ids of registered commands
     *
     * @return sorted ids of registered commands
     */
    @Override
    public Collection<String> commandIds() {
        return commandsFactoriesMap.values().stream()
                .flatMap(factory -> factory.commandIds().stream())
                .distinct().sorted().toList();
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
        return commandsFactoriesMap.values().stream().mapToInt(CommandsFactory::getSize).sum();
    }
}
