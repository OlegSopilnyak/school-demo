package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Service-Facade: Service for manage organization in the school
 */
public interface OrganizationFacade {
    /**
     * To get all authorityPerson
     *
     * @see AuthorityPerson
     * @return list of persons
     */
    Collection<AuthorityPerson> findAllAuthorityPersons();

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> getAuthorityPersonById(Long id);

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance);

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonIsNotExistsException   throws when authorityPerson is not exists
     * @throws AuthorityPersonManageFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    void deleteAuthorityPersonById(Long id)
            throws AuthorityPersonIsNotExistsException,
            AuthorityPersonManageFacultyException;

    default void deleteAuthorityPerson(AuthorityPerson instance)
            throws AuthorityPersonIsNotExistsException,
            AuthorityPersonManageFacultyException {
        if (isInvalid(instance)) {
            throw new AuthorityPersonIsNotExistsException("Wrong " + instance + " to delete");
        }
        deleteAuthorityPersonById(instance.getId());
    }

    /**
     * To get all faculties
     *
     * @see Faculty
     * @return list of faculties
     */
    Collection<Faculty> findAllFaculties();

    /**
     * To get the faculty by ID
     *
     * @param id system-id of the faculty
     * @return Faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> getFacultyById(Long id);

    /**
     * To create or update faculty instance
     *
     * @param instance faculty should be created or updated
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> createOrUpdateFaculty(Faculty instance);

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws FacultyNotExistsException  throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     */
    void deleteFacultyById(Long id)
            throws FacultyNotExistsException,
            FacultyIsNotEmptyException;

    default void deleteFaculty(Faculty instance)
            throws FacultyNotExistsException,
            FacultyIsNotEmptyException {
        if (isInvalid(instance)) {
            throw new FacultyNotExistsException("Wrong " + instance + " to delete");
        }
        deleteFacultyById(instance.getId());
    }

    /**
     * To get all faculties
     *
     * @see StudentsGroup
     * @return list of faculties
     */
    Collection<StudentsGroup> findAllStudentsGroups();
    /**
     * To get the students group by ID
     *
     * @param id system-id of the students group
     * @return StudentsGroup instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    Optional<StudentsGroup> getStudentsGroupById(Long id);
    /**
     * To create or update students group instance
     *
     * @param instance students group should be created or updated
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    Optional<StudentsGroup> createOrUpdateStudentsGroup(StudentsGroup instance);
    /**
     * To delete students group instance from the school
     *
     * @param id system-id of the faculty to delete
     * @throws StudentsGroupNotExistsException  throws when students group is not exists
     * @throws StudentGroupWithStudentsException throws when students group has students
     */
    void deleteStudentsGroupById(Long id)
            throws StudentsGroupNotExistsException,
            StudentGroupWithStudentsException;
    default void deleteStudentsGroup(StudentsGroup instance)
            throws StudentsGroupNotExistsException,
            StudentGroupWithStudentsException {
        if (isInvalid(instance)) {
            throw new StudentsGroupNotExistsException("Wrong " + instance + " to delete");
        }
        deleteStudentsGroupById(instance.getId());
    }

    private static boolean isInvalid(AuthorityPerson instance) {
        return isNull(instance) || isNull(instance.getId());
    }

    private static boolean isInvalid(Faculty instance) {
        return isNull(instance) || isNull(instance.getId());
    }

    private static boolean isInvalid(StudentsGroup instance) {
        return isNull(instance) || isNull(instance.getId());
    }

}
