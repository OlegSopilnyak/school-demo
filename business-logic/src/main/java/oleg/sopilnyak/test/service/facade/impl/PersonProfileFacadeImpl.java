package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.business.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.CommandExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.ProfileCommand.*;

/**
 * Service: To process commands for school's person profiles facade
 */
@Slf4j
@AllArgsConstructor
public class PersonProfileFacadeImpl<T> implements PersonProfileFacade {
    private final CommandsFactory<T> factory;

    /**
     * To get the person's profile by ID
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see CommandExecutor#doSimpleCommand(String, Object, CommandsFactory)
     * @see PersonProfile
     * @see PersonProfile#getId()
     * @see ProfileCommand#FIND_BY_ID_COMMAND_ID
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findById(Long id) {
        return doSimpleCommand(FIND_BY_ID_COMMAND_ID, id, factory);
    }

    /**
     * To create person-profile
     *
     * @param profile instance to create or update
     * @return created instance or Optional#empty()
     * @see CommandExecutor#doSimpleCommand(String, Object, CommandsFactory)
     * @see PersonProfile
     * @see ProfileCommand#CREATE_OR_UPDATE_COMMAND_ID
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> createOrUpdatePersonProfile(PersonProfile profile) {
        return doSimpleCommand(CREATE_OR_UPDATE_COMMAND_ID, profile, factory);
    }

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws ProfileNotExistsException throws if the profile with system-id does not exist in the database
     * @see ProfileCommand#DELETE_BY_ID_COMMAND_ID
     * @see ProfileCommand#createContext(Object)
     * @see ProfileCommand#doCommand(Context)
     * @see Context
     * @see Context#getState()
     */
    @Override
    public void deleteProfileById(Long id) throws ProfileNotExistsException {
        final SchoolCommand<Boolean> command = takeValidCommand(DELETE_BY_ID_COMMAND_ID, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted profile with ID:{} successfully.", id);
        } else {
            deleteProfileExceptionProcessing(context.getException());
        }
    }

    // private methods
    private static void deleteProfileExceptionProcessing(Exception deleteException) throws ProfileNotExistsException {
        final String commandId = DELETE_BY_ID_COMMAND_ID;
        log.warn("Something went wrong with profile deletion", deleteException);
        if (deleteException instanceof ProfileNotExistsException exception) {
            throw exception;
        } else if (nonNull(deleteException)) {
            throwFor(commandId, deleteException);
        } else {
            log.error("For command-id:'{}' there is not exception after wrong command execution.", commandId);
            throwFor(commandId, new NullPointerException("Exception is not stored!!!"));
        }
    }

}
