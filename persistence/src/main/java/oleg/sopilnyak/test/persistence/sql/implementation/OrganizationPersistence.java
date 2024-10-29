package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.FacultyRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.StudentsGroupRepository;
import oleg.sopilnyak.test.school.common.exception.organization.*;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.joint.OrganizationPersistenceFacade;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Persistence facade implementation for organization structure entities
 */
public interface OrganizationPersistence extends OrganizationPersistenceFacade {
    Logger getLog();

    EntityMapper getMapper();

    AuthorityPersonRepository getAuthorityPersonRepository();

    FacultyRepository getFacultyRepository();

    StudentsGroupRepository getStudentsGroupRepository();

    /**
     * To get all authority persons of the school
     *
     * @return the set of authority persons
     * @see AuthorityPerson
     */
    @Override
    default Set<AuthorityPerson> findAllAuthorityPersons() {
        getLog().debug("Looking for all AuthorityPersons");
        return getAuthorityPersonRepository().findAll().stream().map(AuthorityPerson.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To find authority person by id
     *
     * @param id system-id of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    default Optional<AuthorityPerson> findAuthorityPersonByProfileId(Long id) {
        getLog().debug("Looking for AuthorityPerson with profile-id:{}", id);
        return getAuthorityPersonRepository().findByProfileId(id).map(AuthorityPerson.class::cast);
    }

    /**
     * To find authority person by id
     *
     * @param id system-id of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    default Optional<AuthorityPerson> findAuthorityPersonById(Long id) {
        getLog().debug("Looking for AuthorityPerson with ID:{}", id);
        return getAuthorityPersonRepository().findById(id).map(AuthorityPerson.class::cast);
    }

    /**
     * Create or update authority person
     *
     * @param person authority person instance to store
     * @return authority person instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default Optional<AuthorityPerson> save(AuthorityPerson person) {
        getLog().debug("Saving AuthorityPerson '{}'", person);
        final boolean isCreate = isForCreate(person.getId());
        final AuthorityPersonEntity entity =
                person instanceof AuthorityPersonEntity ap ? ap : getMapper().toEntity(person);
        final AuthorityPersonEntity saved = getAuthorityPersonRepository().save(entity);
        if (isCreate && nonNull(saved.getFacultyEntitySet())) {
            saved.getFacultyEntitySet().forEach(faculty -> connect(saved, faculty));
        }
        return Optional.of(getAuthorityPersonRepository().saveAndFlush(saved));
    }

    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManagesFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonNotFoundException      throws when you want to delete authority person who is not created before
     * @see AuthorityPerson
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default boolean deleteAuthorityPerson(Long id) throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        getLog().debug("Deleting the AuthorityPerson with ID:{}", id);
        final Optional<AuthorityPersonEntity> person = getAuthorityPersonRepository().findById(id);
        if (person.isEmpty()) {
            throw new AuthorityPersonNotFoundException("No authorization person with ID:" + id);
        } else if (!person.get().getFaculties().isEmpty()) {
            throw new AuthorityPersonManagesFacultyException("Authorization person with ID:" + id + " is not empty");
        }
        getAuthorityPersonRepository().deleteById(id);
        getAuthorityPersonRepository().flush();
        return true;
    }

    /**
     * To get all faculties of the school
     *
     * @return the set of faculties
     * @see Faculty
     */
    @Override
    default Set<Faculty> findAllFaculties() {
        getLog().debug("Looking for all Faculties");
        return getFacultyRepository().findAll().stream().map(Faculty.class::cast).collect(Collectors.toSet());
    }

    /**
     * To find faculty by id
     *
     * @param id system-id of the faculty
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    default Optional<Faculty> findFacultyById(Long id) {
        getLog().debug("Looking for Faculty with ID:{}", id);
        return getFacultyRepository().findById(id).map(Faculty.class::cast);
    }

    /**
     * Create or update faculty instance
     *
     * @param faculty faculty instance to store
     * @return faculty instance or empty(), if instance couldn't store
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default Optional<Faculty> save(Faculty faculty) {
        getLog().debug("Create or Update {}", faculty);
        final FacultyEntity entity = faculty instanceof FacultyEntity f ? f : getMapper().toEntity(faculty);
        return Optional.of(getFacultyRepository().saveAndFlush(entity));
    }

    /**
     * To delete faculty by id
     *
     * @param id system-id of the faculty
     * @throws FacultyNotFoundException   throws when you want to delete faculty which is not created before
     * @throws FacultyIsNotEmptyException throws when you want to delete faculty which has courses
     * @see Faculty
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default void deleteFaculty(Long id) throws FacultyNotFoundException, FacultyIsNotEmptyException {
        getLog().debug("Deleting for Faculty with ID:{}", id);
        final Optional<FacultyEntity> faculty = getFacultyRepository().findById(id);
        if (faculty.isEmpty()) {
            throw new FacultyNotFoundException("No faculty with ID:" + id);
        } else if (!faculty.get().getCourses().isEmpty()) {
            throw new FacultyIsNotEmptyException("Faculty with ID:" + id + " is not empty");
        }
        getFacultyRepository().deleteById(id);
        getFacultyRepository().flush();
    }

    /**
     * To get all students groups of the school
     *
     * @return the set of students groups
     * @see StudentsGroup
     */
    @Override
    default Set<StudentsGroup> findAllStudentsGroups() {
        getLog().debug("Looking for all StudentsGroups");
        return getStudentsGroupRepository().findAll().stream().map(StudentsGroup.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To find students group by id
     *
     * @param id system-id of the students group
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    default Optional<StudentsGroup> findStudentsGroupById(Long id) {
        getLog().debug("Looking for StudentsGroup with ID:{}", id);
        return getStudentsGroupRepository().findById(id).map(StudentsGroup.class::cast);
    }

    /**
     * Create or update students group instance
     *
     * @param instance students group instance to store
     * @return students group instance or empty(), if instance couldn't store
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default Optional<StudentsGroup> save(StudentsGroup instance) {
        getLog().debug("Create or Update StudentsGroup '{}'", instance);
        final StudentsGroupEntity entity =
                instance instanceof StudentsGroupEntity sg ? sg : getMapper().toEntity(instance);
        getLog().debug("Create or Update StudentsGroup with ID:{}", entity.getId());
        final StudentsGroupEntity saved = getStudentsGroupRepository().saveAndFlush(entity);
        getLog().debug("Updated students group {}", saved);
        return Optional.of(saved);
    }

    /**
     * To delete students group by id
     *
     * @param id system-id of the students group
     * @throws StudentsGroupNotFoundException    throws when you want to delete students group which is not created before
     * @throws StudentGroupWithStudentsException throws when you want to delete students group with students
     * @see StudentsGroup
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default void deleteStudentsGroup(Long id) throws StudentsGroupNotFoundException, StudentGroupWithStudentsException {
        getLog().debug("Deleting students group ID:{}", id);
        final Optional<StudentsGroupEntity> group = getStudentsGroupRepository().findById(id);
        if (group.isEmpty()) {
            throw new StudentsGroupNotFoundException("No students group with ID:" + id);
        } else if (!group.get().getStudents().isEmpty()) {
            throw new StudentGroupWithStudentsException("Students group with ID:" + id + " is not empty");
        }
        getStudentsGroupRepository().deleteById(id);
        getStudentsGroupRepository().flush();
        getLog().debug("Deleted students group ID:{}", id);

    }

    private static boolean isForCreate(Long id) {
        return isNull(id) || id < 0;
    }

    private void connect(AuthorityPersonEntity person, FacultyEntity faculty) {
        faculty.setDean(person);
        getFacultyRepository().save(faculty);
    }

}
