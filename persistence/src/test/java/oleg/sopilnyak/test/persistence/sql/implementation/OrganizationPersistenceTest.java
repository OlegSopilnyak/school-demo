package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.FacultyRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.StudentsGroupRepository;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.joint.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class OrganizationPersistenceTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    OrganizationPersistenceFacade persistence;

    @SpyBean
    @Autowired
    AuthorityPersonRepository authorityPersonRepository;
    @SpyBean
    @Autowired
    FacultyRepository facultyRepository;
    @SpyBean
    @Autowired
    StudentsGroupRepository studentsGroupRepository;

    @AfterEach
    void tearDown() {
        reset(persistence, authorityPersonRepository, facultyRepository, studentsGroupRepository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void persistenceShouldBePresent() {
        assertThat(persistence).isNotNull();
        assertThat(authorityPersonRepository).isNotNull();
        assertThat(facultyRepository).isNotNull();
        assertThat(studentsGroupRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllAuthorityPersons() {
        AuthorityPerson person = createAuthorityPerson(0);

        Set<AuthorityPerson> set = persistence.findAllAuthorityPersons();

        assertThat(set).isNotNull().isNotEmpty().hasSize(1).contains(person);
        verify(authorityPersonRepository).findAll();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAuthorityPersonByProfileId() {
        long profileId = 101;
        AuthorityPerson person = makeCleanAuthorityPerson(1);
        if (person instanceof FakeAuthorityPerson fake) fake.setProfileId(profileId);
        Optional<AuthorityPerson> entity = persistence.save(person);
        assertThat(entity).isPresent();
        assertThat(entity.get().getProfileId()).isEqualTo(profileId);

        Optional<AuthorityPerson> found = persistence.findAuthorityPersonByProfileId(profileId);

        assertThat(found).isPresent();
        assertAuthorityPersonEquals(person, found.get(), false);
        verify(authorityPersonRepository).findByProfileId(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAuthorityPersonById() {
        AuthorityPerson person = createAuthorityPerson(2);
        long id = person.getId();

        Optional<AuthorityPerson> found = persistence.findAuthorityPersonById(id);

        assertThat(found).isPresent();
        assertAuthorityPersonEquals(person, found.get());
        verify(authorityPersonRepository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateAuthorityPerson() {
        AuthorityPerson person = makeCleanAuthorityPerson(3);

        Optional<AuthorityPerson> created = persistence.save(person);

        assertThat(created).isPresent();
        assertAuthorityPersonEquals(person, created.get(), false);
        verify(authorityPersonRepository).save(any(AuthorityPersonEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateAuthorityPerson() {
        AuthorityPersonEntity person = createAuthorityPerson(4);
        assertThat(person.getFirstName()).isNotEqualTo("firstName");
        person.setFirstName("firstName");

        Optional<AuthorityPerson> updated = persistence.save(person);

        assertThat(updated).isPresent();
        assertThat(updated.orElseThrow().getFirstName()).isEqualTo("firstName");
        assertAuthorityPersonEquals(person, updated.get());
        verify(authorityPersonRepository).save(person);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteAuthorityPerson() {
        AuthorityPerson person = createAuthorityPerson(5);
        long id = person.getId();

        assertThat(persistence.deleteAuthorityPerson(id)).isTrue();

        assertThat(persistence.findAuthorityPersonById(id)).isEmpty();
        verify(authorityPersonRepository).deleteById(id);
        verify(authorityPersonRepository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllFaculties() {
        Faculty faculty = createFaculty(0);

        Set<Faculty> set = persistence.findAllFaculties();

        assertThat(set).isNotNull().isNotEmpty().hasSize(1).contains(faculty);
        verify(facultyRepository).findAll();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindFacultyById() {
        Faculty faculty = createFaculty(1);
        long id = faculty.getId();

        Optional<Faculty> found = persistence.findFacultyById(id);

        assertThat(found).isPresent();
        assertFacultyEquals(faculty, found.get());
        verify(facultyRepository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateFaculty() {
        Faculty faculty = makeCleanFaculty(2);

        Optional<Faculty> saved = persistence.save(faculty);

        assertThat(saved).isPresent();
        assertFacultyEquals(faculty, saved.get(), false);
        verify(facultyRepository).saveAndFlush(any(FacultyEntity.class));


    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateFaculty() {
        FacultyEntity faculty = createFaculty(3);
        assertThat(faculty.getName()).isNotEqualTo("name");
        faculty.setName("name");

        Optional<Faculty> saved = persistence.save(faculty);

        assertThat(saved).isPresent();
        assertThat(saved.orElseThrow().getName()).isEqualTo("name");
        assertFacultyEquals(faculty, saved.get());
        verify(facultyRepository).saveAndFlush(faculty);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteFaculty() {
        Faculty faculty = createFaculty(4);
        long id = faculty.getId();

        persistence.deleteFaculty(id);

        assertThat(persistence.findFacultyById(id)).isEmpty();
        verify(facultyRepository).deleteById(id);
        verify(facultyRepository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllStudentsGroups() {
        StudentsGroup group = createStudentsGroup(0);

        Set<StudentsGroup> set = persistence.findAllStudentsGroups();

        assertThat(set).isNotNull().isNotEmpty().hasSize(1).contains(group);
        verify(studentsGroupRepository).findAll();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentsGroupById() {
        StudentsGroup group = createStudentsGroup(1);
        long id = group.getId();

        Optional<StudentsGroup> found = persistence.findStudentsGroupById(id);

        assertThat(found).isPresent();
        assertStudentsGroupEquals(group, found.get());
        verify(studentsGroupRepository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateStudentsGroup() {
        StudentsGroup group = makeCleanStudentsGroup(2);

        Optional<StudentsGroup> saved = persistence.save(group);

        assertThat(saved).isPresent();
        assertStudentsGroupEquals(group, saved.get(), false);
        verify(studentsGroupRepository).saveAndFlush(any(StudentsGroupEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateStudentsGroup() {
        StudentsGroupEntity group = createStudentsGroup(3);
        assertThat(group.getName()).isNotEqualTo("name");
        group.setName("name");

        Optional<StudentsGroup> saved = persistence.save(group);

        assertThat(saved).isPresent();
        assertThat(saved.orElseThrow().getName()).isEqualTo("name");
        assertStudentsGroupEquals(group, saved.get(), false);
        verify(studentsGroupRepository).saveAndFlush(group);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudentsGroup() {
        StudentsGroup group = makeCleanStudentsGroup(4);
        if (group instanceof FakeStudentsGroup fakeGroup) {
            fakeGroup.setStudents(List.of());
        } else {
            fail("Cannot delete students group");
        }
        Optional<StudentsGroup> saved = persistence.save(group);
        long id = saved.orElseThrow().getId();

        persistence.deleteStudentsGroup(id);

        assertThat(persistence.findStudentsGroupById(id)).isEmpty();
        verify(studentsGroupRepository).deleteById(id);
        verify(studentsGroupRepository).flush();
    }

    //private methods
    private AuthorityPersonEntity createAuthorityPerson(int order) {
        try {
            AuthorityPersonEntity entity = persistence.save(makeCleanAuthorityPerson(order)).map(AuthorityPersonEntity.class::cast).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(authorityPersonRepository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, authorityPersonRepository);
        }
    }

    private FacultyEntity createFaculty(int order) {
        try {
            FacultyEntity entity = persistence.save(makeCleanFaculty(order)).map(FacultyEntity.class::cast).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(facultyRepository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, facultyRepository);
        }
    }

    private StudentsGroupEntity createStudentsGroup(int order) {
        try {
            StudentsGroupEntity entity = persistence.save(makeCleanStudentsGroup(order)).map(StudentsGroupEntity.class::cast).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(studentsGroupRepository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, studentsGroupRepository);
        }
    }
}