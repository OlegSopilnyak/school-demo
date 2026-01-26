package oleg.sopilnyak.test.service.facade.profile.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.BaseProfilePayload;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.Getter;

/**
 * Service: To process commands for school's person profiles facades
 *
 * @see oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl
 * @see oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl
 */
public abstract class PersonProfileFacadeImpl<P extends ProfileCommand<?>> implements PersonProfileFacade, ActionFacade {

    private final CommandsFactory<P> factory;
    // semantic data to payload converter
    private final UnaryOperator<PersonProfile> toPayload;
    @Getter
    private final CommandActionExecutor actionExecutor;
    //
    // setting up actions by command-id
    private final Map<String, Function<Object, Object>> actions = Map.of(
            findByIdCommandId(), this::internalFindById,
            createOrUpdateCommandId(), this::internalCreateOrUpdate,
            deleteByIdCommandId(), this::internalDeleteById
    );

    /**
     * To get the command-id of find by id command
     *
     * @return command-id value
     */
    protected abstract String findByIdCommandId();

    /**
     * To get the command-id of create or update command
     *
     * @return command-id value
     */
    protected abstract String createOrUpdateCommandId();

    /**
     * To get the command-id of delete command
     *
     * @return command-id value
     */
    protected abstract String deleteByIdCommandId();

    protected PersonProfileFacadeImpl(
            CommandsFactory<P> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
        this.toPayload = profile -> profile instanceof BaseProfilePayload ? profile : mapper.toPayload(profile);
    }

    /**
     * Facade depended, action's execution
     *
     * @param actionId         the id of the action
     * @param actionParameters the parameters of action to execute
     * @return action execution result value
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T concreteAction(final String actionId, final Object... actionParameters) {
        final Object argument = actionParameters.length > 0 ? actionParameters[0] : null;
        getLogger().debug("Trying to execute command {} with arguments {}", actionId, argument);
        return (T) actions.computeIfAbsent(actionId, this::throwsUnknownActionId).apply(argument);
//        if (actionId.equals(findByIdCommandId())) {
//            final Long id = (Long) actionParameters[0];
//            return (T) findById(id);
//        } else if (actionId.equals(createOrUpdateCommandId())) {
//            final PersonProfile profile = (PersonProfile) actionParameters[0];
//            return (T) createOrUpdate(profile);
//        } else if (actionId.equals(deleteByIdCommandId())) {
//            final Long id = (Long) actionParameters[0];
//            deleteById(id);
//            return null;
//        }
//        final String expectedTypes =
//                String.join(" or ", List.of(findByIdCommandId(), createOrUpdateCommandId(), deleteByIdCommandId()));
//        throw new InvalidParameterTypeException(expectedTypes, actionId);
    }

    /**
     * To get the person's profile by ID
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfileFacadeImpl#findByIdCommandId()
     * @see ActionFacade#executeCommand(String, CommandsFactory, Input)
     * @see PersonProfile
     * @see PersonProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findById(Long id) {
        getLogger().debug("Finding profile by ID:{}", id);
        final Optional<Optional<PersonProfile>> result;
        result = executeCommand(findByIdCommandId(), factory, Input.of(id));
        if (result.isPresent()) {
            final Optional<PersonProfile> profile = result.get();
            getLogger().debug("Found profile {}", profile);
            return profile.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To create person-profile
     *
     * @param instance instance to create or update
     * @return created instance or Optional#empty()
     * @see PersonProfileFacadeImpl#createOrUpdateCommandId()
     * @see ActionFacade#executeCommand(String, CommandsFactory, Input)
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> createOrUpdate(PersonProfile instance) {
        getLogger().debug("Creating or Updating profile {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<PersonProfile>> result;
        result = executeCommand(createOrUpdateCommandId(), factory, input);
        if (result.isPresent()) {
            final Optional<PersonProfile> profile = result.get();
            getLogger().debug("Changed profile {}", profile);
            return profile.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws ProfileNotFoundException throws if the profile with system-id does not exist in the database
     * @see PersonProfileFacadeImpl#deleteByIdCommandId()
     * @see ActionFacade#executeCommand(String, CommandsFactory, Input, Consumer)
     * @see ProfileCommand#createContext(Input)
     * @see ProfileCommand#doCommand(Context)
     * @see Context
     * @see Context#getState()
     */
    @Override
    public void deleteById(Long id) throws ProfileNotFoundException {
        final String commandId = deleteByIdCommandId();
        final Consumer<Exception> doThisOnError = exception -> {
            switch (exception) {
                case ProfileNotFoundException profileNotFoundException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw profileNotFoundException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        getLogger().debug("Deleting profile with ID:{}", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doThisOnError);
        result.ifPresent(executionResult ->
                getLogger().debug("Deleted profile with ID:{} successfully:{} .", id, executionResult)
        );
    }

    // private methods
    //
    private Function<Object, Object> throwsUnknownActionId(final String actionId) {
        final String expectedTypes =
                String.join(" or ", List.of(findByIdCommandId(), createOrUpdateCommandId(), deleteByIdCommandId()));
        throw new InvalidParameterTypeException(expectedTypes, actionId);
    }

    // To get the person's profile by ID
    private Optional<PersonProfile> internalFindById(final Object argument) {
        if (argument instanceof Long id) {
            getLogger().debug("Getting profile by ID:{}", id);
            // running command execution
            final Input<Long> input = Input.of(id);
            final Optional<Optional<PersonProfile>> result = executeCommand(findByIdCommandId(), factory, input);
            return result.flatMap(profile -> {
                getLogger().debug("Found profile {}", profile);
                return profile.map(toPayload);
            });
        } else {
            throw new InvalidParameterTypeException("Long", argument);
        }
    }

    // To create or update person-profile
    private Optional<PersonProfile> internalCreateOrUpdate(final Object argument) {
        if (argument instanceof PersonProfile instance) {
            getLogger().debug("Creating or Updating profile {}", instance);
            // running command execution
            final var input = Input.of(toPayload.apply(instance));
            final Optional<Optional<PersonProfile>> result = executeCommand(createOrUpdateCommandId(), factory, input);
            return result.flatMap(profile -> {
                getLogger().debug("Changed profile {}", profile);
                return profile.map(toPayload);
            });
        } else {
            throw new InvalidParameterTypeException("PersonProfile", argument);
        }
    }

    // To delete person-profile
    public Void internalDeleteById(final Object argument) throws ProfileNotFoundException {
        if (argument instanceof Long id) {
            final String commandId = deleteByIdCommandId();
            final Consumer<Exception> doOnError = exception -> {
                switch (exception) {
                    case ProfileNotFoundException profileNotFoundException -> {
                        logSomethingWentWrong(exception, commandId);
                        throw profileNotFoundException;
                    }
                    case null, default -> defaultDoOnError(commandId).accept(exception);
                }
            };
            // running command execution
            getLogger().debug("Deleting profile with ID:{}", id);
            final Input<Long> input = Input.of(id);
            final Optional<Boolean> result = executeCommand(commandId, factory, input, doOnError);
            result.ifPresent(success ->
                    getLogger().debug("Deleted profile with ID:{} successfully:{} .", id, success)
            );
            return null;
        } else {
            throw new InvalidParameterTypeException("Long", argument);
        }
    }
}
