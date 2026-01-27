package oleg.sopilnyak.test.service.facade.profile.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.BaseProfilePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
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
    // setting up action-methods by action-id
    private final Map<String, UnaryOperator<Object>> actions = new HashMap<>(
            Map.of(
                    findByIdActionId(), this::internalFindById,
                    createOrUpdateActionId(), this::internalCreateOrUpdate,
                    deleteByIdActionId(), this::internalDeleteById
            )
    );

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
     * Facade depends on the action's execution
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
    }

    @Deprecated
    @Override
    public Optional<PersonProfile> findById(Long id) {
        throw new UnsupportedOperationException("Deprecated method isn't supported already.");
    }

    @Deprecated
    @Override
    public Optional<PersonProfile> createOrUpdate(PersonProfile instance) {
        throw new UnsupportedOperationException("Deprecated method isn't supported already.");
    }

    @Deprecated
    @Override
    public void deleteById(Long id) throws ProfileNotFoundException {
        throw new UnsupportedOperationException("Deprecated method isn't supported already.");
    }

    // private methods
    // throws exception if actio-id is invalid
    private UnaryOperator<Object> throwsUnknownActionId(final String actionId) {
        final String expectedTypes = String.join(" or ", validActions());
        throw new InvalidParameterTypeException(expectedTypes, actionId);
    }

    // To get the person's profile by ID
    private Optional<PersonProfile> internalFindById(final Object argument) {
        if (argument instanceof Long id) {
            getLogger().debug("Getting profile by ID:{}", id);
            // running command execution
            final Input<Long> input = Input.of(id);
            final Optional<Optional<PersonProfile>> result = executeCommand(findByIdActionId(), factory, input);
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
            final Optional<Optional<PersonProfile>> result = executeCommand(createOrUpdateActionId(), factory, input);
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
            final String commandId = deleteByIdActionId();
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
