package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

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
