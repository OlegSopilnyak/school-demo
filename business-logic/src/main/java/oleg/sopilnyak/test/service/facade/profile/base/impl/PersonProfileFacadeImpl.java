package oleg.sopilnyak.test.service.facade.profile.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
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

    protected PersonProfileFacadeImpl(
            CommandsFactory<P> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
        this.toPayload = profile -> profile instanceof BaseProfilePayload ? profile : mapper.toPayload(profile);
    }

    protected abstract String findByIdCommandId();

    protected abstract String createOrUpdateCommandId();

    protected abstract String deleteByIdCommandId();

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
        if (actionId.equals(findByIdCommandId())) {
            final Long id = (Long) actionParameters[0];
            return (T) findById(id);
        } else if (actionId.equals(createOrUpdateCommandId())) {
            final PersonProfile profile = (PersonProfile) actionParameters[0];
            return (T) createOrUpdate(profile);
        } else if (actionId.equals(deleteByIdCommandId())) {
            final Long id = (Long) actionParameters[0];
            deleteById(id);
            return null;
        }
        throw new IllegalArgumentException("Unknown actionId: " + actionId);
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
}
