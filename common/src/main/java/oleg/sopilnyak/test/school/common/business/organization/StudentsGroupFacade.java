package oleg.sopilnyak.test.school.common.business.organization;

import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.StudentsGroupNotExistsException;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.Collection;
import java.util.Optional;

/**
 * Service-Facade: Service for manage organization in the school (school students groups)
 *
 * @see OrganizationFacade
 * @see StudentsGroup
 */
public interface StudentsGroupFacade extends OrganizationFacade {
    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see StudentsGroup
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
     * @throws StudentsGroupNotExistsException   throws when students group is not exists
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

}
