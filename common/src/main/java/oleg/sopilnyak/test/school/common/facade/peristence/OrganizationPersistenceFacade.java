package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.Optional;
import java.util.Set;

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
     * @return course instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> saveAuthorityPerson(AuthorityPerson authorityPerson);
    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManageFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonIsNotExistsException throws when you want to delete authority person who is not created before
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
     * To get all students groups of the school
     *
     * @return the set of students groups
     * @see StudentsGroup
     */
    Set<StudentsGroup> findAllStudentsGroups();

}
