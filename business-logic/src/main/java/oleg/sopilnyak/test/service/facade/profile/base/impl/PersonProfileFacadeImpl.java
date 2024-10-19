package oleg.sopilnyak.test.service.facade.profile.base.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.CommandExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseProfilePayload;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service: To process commands for school's person profiles facade
 */
@Slf4j
public abstract class PersonProfileFacadeImpl<P extends ProfileCommand> implements PersonProfileFacade {
    protected static final String WRONG_COMMAND_EXECUTION = "For command-id:'{}' there is not exception after wrong command execution.";
    protected static final String EXCEPTION_IS_NOT_STORED = "Exception is not stored!!!";
    protected static final String SOMETHING_WENT_WRONG = "Something went wrong";
    private final CommandsFactory<P> factory;
    private final BusinessMessagePayloadMapper mapper;
    // semantic data to payload converter
    private final UnaryOperator<PersonProfile> convert;

    protected PersonProfileFacadeImpl(final CommandsFactory<P> factory,
                                      final BusinessMessagePayloadMapper mapper) {
        this.factory = factory;
        this.mapper = mapper;
        this.convert = profile -> profile instanceof BaseProfilePayload ? profile : this.mapper.toPayload(profile);
    }

    protected abstract String findByIdCommandId();

    protected abstract String createOrUpdateCommandId();

    protected abstract String deleteByIdCommandId();

    /**
     * To get the person's profile by ID
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfileFacadeImpl#findByIdCommandId()
     * @see CommandExecutor#doSimpleCommand(String, Object, CommandsFactory)
     * @see PersonProfile
     * @see PersonProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findById(Long id) {
        log.debug("Find profile by ID:{}", id);
        final Optional<PersonProfile> result = doSimpleCommand(findByIdCommandId(), id, factory);
        log.debug("Found profile {}", result);
        return result.map(convert);
    }

    /**
     * To create person-profile
     *
     * @param instance instance to create or update
     * @return created instance or Optional#empty()
     * @see PersonProfileFacadeImpl#createOrUpdateCommandId()
     * @see CommandExecutor#doSimpleCommand(String, Object, CommandsFactory)
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> createOrUpdate(PersonProfile instance) {
        log.debug("Create or Update profile {}", instance);
        final Optional<PersonProfile> result = doSimpleCommand(createOrUpdateCommandId(), convert.apply(instance), factory);
        log.debug("Changed profile {}", result);
        return result.map(convert);
    }

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws ProfileIsNotFoundException throws if the profile with system-id does not exist in the database
     * @see PersonProfileFacadeImpl#deleteByIdCommandId()
     * @see ProfileCommand#createContext(Object)
     * @see ProfileCommand#doCommand(Context)
     * @see Context
     * @see Context#getState()
     */
    @Override
    public void deleteById(Long id) throws ProfileIsNotFoundException {
        log.debug("Delete profile with ID:{}", id);
        final String commandId = deleteByIdCommandId();
        final RootCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Deleted profile with ID:{} successfully.", id);
            return;
        }

        // fail processing
        final Exception deleteException = context.getException();
        log.warn(SOMETHING_WENT_WRONG + " with profile deletion", deleteException);
        if (deleteException instanceof ProfileIsNotFoundException profileException) {
            throw profileException;
        } else if (nonNull(deleteException)) {
            throwFor(commandId, deleteException);
        } else {
            wrongCommandExecution(commandId);
        }
    }

    // private methods
    private static void wrongCommandExecution(String commandId) {
        log.error(WRONG_COMMAND_EXECUTION, commandId);
        throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
    }
}
