package oleg.sopilnyak.test.service.facade.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.FacultyNotExistsException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Collection;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see OrganizationFacadeImpl
 * @see Faculty
 * @see FacultyCommand
 */
@Slf4j
public class FacultyFacadeImpl extends OrganizationFacadeImpl implements FacultyFacade {

    public FacultyFacadeImpl(CommandsFactory<?> factory) {
        super(factory);
    }

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see Faculty
     */
    @Override
    public Collection<Faculty> findAllFaculties() {
        return executeSimpleCommand(FacultyCommand.FIND_ALL, null, factory);
    }

    /**
     * To get the faculty by ID
     *
     * @param id system-id of the faculty
     * @return Faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Faculty> getFacultyById(Long id) {
        return executeSimpleCommand(FacultyCommand.FIND_BY_ID, id, factory);
    }

    /**
     * To create or update faculty instance
     *
     * @param instance faculty should be created or updated
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Faculty> createOrUpdateFaculty(Faculty instance) {
        return executeSimpleCommand(FacultyCommand.CREATE_OR_UPDATE, instance, factory);
    }

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws FacultyNotExistsException  throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     */
    @Override
    public void deleteFacultyById(Long id) throws FacultyNotExistsException, FacultyIsNotEmptyException {
        String commandId = FacultyCommand.DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof FacultyNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof FacultyIsNotEmptyException exception) {
                throw exception;
            } else {
                throwFor(commandId, cmdResult.getException());
            }
        }
    }
}
