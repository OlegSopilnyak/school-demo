package oleg.sopilnyak.test.service.facade.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.AuthorityPersonCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.FacultyCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;

import java.util.Collection;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.CommandExecutor.*;

/**
 * Service: To process commands for school's organization structure
 */
@Slf4j
@AllArgsConstructor
public class OrganizationFacadeImpl implements OrganizationCommandFacade {
    private final CommandsFactory factory;

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     */
    public Collection<AuthorityPerson> findAllAuthorityPersons() {
        return executeSimpleCommand(AuthorityPersonCommandFacade.FIND_ALL, null, factory);
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
        return executeSimpleCommand(AuthorityPersonCommandFacade.FIND_BY_ID, id, factory);
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
        return executeSimpleCommand(AuthorityPersonCommandFacade.CREATE_OR_UPDATE, instance, factory);
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
        String commandId = AuthorityPersonCommandFacade.DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn("Something went wrong", executionException);
            if (executionException instanceof AuthorityPersonIsNotExistsException) {
                throw (AuthorityPersonIsNotExistsException) executionException;
            } else if (executionException instanceof AuthorityPersonManageFacultyException) {
                throw (AuthorityPersonManageFacultyException) executionException;
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
        return executeSimpleCommand(FacultyCommandFacade.FIND_ALL, null, factory);
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
        return executeSimpleCommand(FacultyCommandFacade.FIND_BY_ID, id, factory);
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
        return executeSimpleCommand(FacultyCommandFacade.CREATE_OR_UPDATE, instance, factory);
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
        String commandId = FacultyCommandFacade.DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn("Something went wrong", executionException);
            if (executionException instanceof FacultyNotExistsException) {
                throw (FacultyNotExistsException) executionException;
            } else if (executionException instanceof FacultyIsNotEmptyException) {
                throw (FacultyIsNotEmptyException) executionException;
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
        return executeSimpleCommand(StudentsGroupCommandFacade.FIND_ALL, null, factory);
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
        return executeSimpleCommand(StudentsGroupCommandFacade.FIND_BY_ID, id, factory);
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
        return executeSimpleCommand(StudentsGroupCommandFacade.CREATE_OR_UPDATE, instance, factory);
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
        String commandId = StudentsGroupCommandFacade.DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn("Something went wrong", executionException);
            if (executionException instanceof StudentsGroupNotExistsException) {
                throw (StudentsGroupNotExistsException) executionException;
            } else if (executionException instanceof StudentGroupWithStudentsException) {
                throw (StudentGroupWithStudentsException) executionException;
            } else {
                throwFor(commandId, cmdResult.getException());
            }
        }
    }
}
