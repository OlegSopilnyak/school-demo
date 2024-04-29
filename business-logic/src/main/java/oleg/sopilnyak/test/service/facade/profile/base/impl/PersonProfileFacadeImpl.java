package oleg.sopilnyak.test.service.facade.profile.base.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.business.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.CommandExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service: To process commands for school's person profiles facade
 */
@Slf4j
@AllArgsConstructor
public abstract class PersonProfileFacadeImpl<T> implements PersonProfileFacade {
    private final CommandsFactory<T> factory;

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
        return doSimpleCommand(findByIdCommandId(), id, factory);
    }

    /**
     * To create person-profile
     *
     * @param profile instance to create or update
     * @return created instance or Optional#empty()
     * @see PersonProfileFacadeImpl#createOrUpdateCommandId()
     * @see CommandExecutor#doSimpleCommand(String, Object, CommandsFactory)
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> createOrUpdate(PersonProfile profile) {
        return doSimpleCommand(createOrUpdateCommandId(), profile, factory);
    }

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws NotExistProfileException throws if the profile with system-id does not exist in the database
     * @see PersonProfileFacadeImpl#deleteByIdCommandId()
     * @see ProfileCommand#createContext(Object)
     * @see ProfileCommand#doCommand(Context)
     * @see Context
     * @see Context#getState()
     */
    @Override
    public void deleteById(Long id) throws NotExistProfileException {
        final SchoolCommand<Boolean> command = takeValidCommand(deleteByIdCommandId(), factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted profile with ID:{} successfully.", id);
        } else {
            deleteProfileExceptionProcessing(context.getException());
        }
    }

    // private methods
    private void deleteProfileExceptionProcessing(Exception deleteException) throws NotExistProfileException {
        final String commandId = deleteByIdCommandId();
        log.warn("Something went wrong with profile deletion", deleteException);
        if (deleteException instanceof NotExistProfileException exception) {
            throw exception;
        } else if (nonNull(deleteException)) {
            throwFor(commandId, deleteException);
        } else {
            log.error("For command-id:'{}' there is not exception after wrong command execution.", commandId);
            throwFor(commandId, new NullPointerException("Exception is not stored!!!"));
        }
    }

}
