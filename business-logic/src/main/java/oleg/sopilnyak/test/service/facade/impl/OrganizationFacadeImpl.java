package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.id.set.AuthorityPersonCommands;
import oleg.sopilnyak.test.service.command.id.set.FacultyCommands;
import oleg.sopilnyak.test.service.command.id.set.StudentsGroupCommands;

import java.util.Collection;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service: To process commands for school's organization structure
 */
@Slf4j
@AllArgsConstructor
public class OrganizationFacadeImpl implements OrganizationFacade {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    private final CommandsFactory<?> factory;

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     */
    public Collection<AuthorityPerson> findAllAuthorityPersons() {
        return executeTheCommand(AuthorityPersonCommands.FIND_ALL, null, factory);
    }

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> getAuthorityPersonById(Long id) {
        return executeTheCommand(AuthorityPersonCommands.FIND_BY_ID, id, factory);
    }

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance) {
        return executeTheCommand(AuthorityPersonCommands.CREATE_OR_UPDATE, instance, factory);
    }

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonIsNotExistsException   throws when authorityPerson is not exists
     * @throws AuthorityPersonManageFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    @Override
    public void deleteAuthorityPersonById(Long id) throws AuthorityPersonIsNotExistsException, AuthorityPersonManageFacultyException {
        String commandId = AuthorityPersonCommands.DELETE.id();
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof AuthorityPersonIsNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof AuthorityPersonManageFacultyException exception) {
                throw exception;
            } else {
                throwFor(commandId, cmdResult.getException());
            }
        }
    }

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see Faculty
     */
    @Override
    public Collection<Faculty> findAllFaculties() {
        return executeTheCommand(FacultyCommands.FIND_ALL, null, factory);
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
        return executeTheCommand(FacultyCommands.FIND_BY_ID, id, factory);
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
        return executeTheCommand(FacultyCommands.CREATE_OR_UPDATE, instance, factory);
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
        String commandId = FacultyCommands.DELETE.id();
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

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see StudentsGroup
     */
    @Override
    public Collection<StudentsGroup> findAllStudentsGroups() {
        return executeTheCommand(StudentsGroupCommands.FIND_ALL, null, factory);
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
        return executeTheCommand(StudentsGroupCommands.FIND_BY_ID, id, factory);
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
        return executeTheCommand(StudentsGroupCommands.CREATE_OR_UPDATE, instance, factory);
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
        String commandId = StudentsGroupCommands.DELETE.id();
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

    private static <T> T executeTheCommand(AuthorityPersonCommands command, Object option, CommandsFactory<?> factory) {
        return executeSimpleCommand(command.id(), option, factory);
    }

    private static <T> T executeTheCommand(FacultyCommands command, Object option, CommandsFactory<?> factory) {
        return executeSimpleCommand(command.id(), option, factory);
    }

    private static <T> T executeTheCommand(StudentsGroupCommands command, Object option, CommandsFactory<?> factory) {
        return executeSimpleCommand(command.id(), option, factory);
    }
}
