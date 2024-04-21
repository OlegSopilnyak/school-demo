package oleg.sopilnyak.test.service.facade.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.StudentsGroupNotExistsException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Collection;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service-Facade: Service for manage organization in the school (school students groups)
 *
 * @see OrganizationFacade
 * @see StudentsGroup
 */
@Slf4j
public class StudentsGroupFacadeImpl extends OrganizationFacadeImpl implements StudentsGroupFacade {
    public StudentsGroupFacadeImpl(CommandsFactory<?> factory) {
        super(factory);
    }

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see StudentsGroup
     */
    @Override
    public Collection<StudentsGroup> findAllStudentsGroups() {
        return executeSimpleCommand(StudentsGroupCommand.FIND_ALL, null, factory);
    }

    /**
     * To get the students group by ID
     *
     * @param id system-id of the students group
     * @return StudentsGroup instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> getStudentsGroupById(Long id) {
        return executeSimpleCommand(StudentsGroupCommand.FIND_BY_ID, id, factory);
    }

    /**
     * To create or update students group instance
     *
     * @param instance students group should be created or updated
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> createOrUpdateStudentsGroup(StudentsGroup instance) {
        return executeSimpleCommand(StudentsGroupCommand.CREATE_OR_UPDATE, instance, factory);
    }

    /**
     * To delete students group instance from the school
     *
     * @param id system-id of the faculty to delete
     * @throws StudentsGroupNotExistsException   throws when students group is not exists
     * @throws StudentGroupWithStudentsException throws when students group has students
     */
    @Override
    public void deleteStudentsGroupById(Long id) throws StudentsGroupNotExistsException, StudentGroupWithStudentsException {
        String commandId = StudentsGroupCommand.DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof StudentsGroupNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof StudentGroupWithStudentsException exception) {
                throw exception;
            } else {
                throwFor(commandId, cmdResult.getException());
            }
        }
    }
}
