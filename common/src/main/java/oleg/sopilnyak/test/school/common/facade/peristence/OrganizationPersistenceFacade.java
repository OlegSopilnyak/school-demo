package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.Optional;
import java.util.Set;

/**
 * Persistence facade for organization structure entities
 */
public interface OrganizationPersistenceFacade {
    /**
     * To get all authority persons of the school
     *
     * @return the set of authority persons
     * @see AuthorityPerson
     */
    Set<AuthorityPerson> findAllAuthorityPersons();

    /**
     * To find authority person by id
     *
     * @param id system-id of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> findAuthorityPersonById(Long id);

    /**
     * Create or update authority person
     *
     * @param authorityPerson authority person instance to store
     * @return authority person instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> save(AuthorityPerson authorityPerson);

    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManageFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonIsNotExistsException   throws when you want to delete authority person who is not created before
     * @see AuthorityPerson
     */
    void deleteAuthorityPerson(Long id) throws
            AuthorityPersonManageFacultyException,
            AuthorityPersonIsNotExistsException;

    /**
     * To get all faculties of the school
     *
     * @return the set of faculties
     * @see Faculty
     */
    Set<Faculty> findAllFaculties();
    /**
     * To find faculty by id
     *
     * @param id system-id of the faculty
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> findFacultyById(Long id);
    /**
     * Create or update faculty instance
     *
     * @param instance faculty instance to store
     * @return faculty instance or empty(), if instance couldn't store
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> save(Faculty instance);
    /**
     * To delete faculty by id
     *
     * @param id system-id of the faculty
     * @throws FacultyNotExistsException throws when you want to delete faculty which is not created before
     * @throws FacultyIsNotEmptyException throws when you want to delete faculty which has courses
     * @see Faculty
     */
    void deleteFaculty(Long id) throws
            FacultyNotExistsException,
            FacultyIsNotEmptyException;

    /**
     * To get all students groups of the school
     *
     * @return the set of students groups
     * @see StudentsGroup
     */
    Set<StudentsGroup> findAllStudentsGroups();
    /**
     * To find students group by id
     *
     * @param id system-id of the students group
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    Optional<StudentsGroup> findStudentsGroupById(Long id);
    /**
     * Create or update students group instance
     *
     * @param instance students group instance to store
     * @return students group instance or empty(), if instance couldn't store
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    Optional<StudentsGroup> save(StudentsGroup instance);
    /**
     * To delete students group by id
     *
     * @param id system-id of the students group
     * @throws StudentsGroupNotExistsException throws when you want to delete students group which is not created before
     * @throws StudentGroupWithStudentsException throws when you want to delete students group with students
     * @see StudentsGroup
     */
    void deleteStudentsGroup(Long id) throws
            StudentsGroupNotExistsException,
            StudentGroupWithStudentsException;
}
