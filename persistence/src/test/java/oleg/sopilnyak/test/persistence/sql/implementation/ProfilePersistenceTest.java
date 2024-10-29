package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class ProfilePersistenceTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @SpyBean
    @Autowired
    PersonProfileRepository<PersonProfileEntity> repository;

    @AfterEach
    void tearDown() {
        reset(persistence);
        reset(repository);
    }

    @Test
    void persistenceShouldBePresent() {
        assertThat(persistence).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentProfileById() {
        StudentProfile profile = makeStudentProfile(null);
        Optional<StudentProfile> studentProfile = persistence.save(profile);
        assertThat(studentProfile).isNotEmpty();
        Long id = studentProfile.orElseThrow().getId();

        Optional<StudentProfile> student = persistence.findStudentProfileById(id);

        assertThat(student).isNotEmpty();
        assertProfilesEquals(student.orElse(null), profile, false);

        verify(persistence).saveProfile(profile);
        verify(repository).saveAndFlush(any(PersonProfileEntity.class));

        verify(persistence).findProfileById(id);
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontFindStudentProfileById() {
        Long id = 500L;
        StudentProfile profile = makeStudentProfile(id);
        assertThat(persistence.save(profile)).isEmpty();

        assertThat(persistence.findStudentProfileById(id)).isEmpty();

        verify(persistence).saveProfile(profile);
        verify(persistence).findProfileById(id);
        verify(repository, times(2)).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindPrincipalProfileById() {
        PrincipalProfile profile = makePrincipalProfile(null);
        Optional<PrincipalProfile> principalProfile = persistence.save(profile);
        assertThat(principalProfile).isNotEmpty();
        Long id = principalProfile.orElseThrow().getId();

        Optional<PrincipalProfile> principal = persistence.findPrincipalProfileById(id);

        assertThat(principal).isNotEmpty();
        assertProfilesEquals(principal.orElse(null), profile, false);

        verify(persistence).saveProfile(profile);
        verify(repository).saveAndFlush(any(PersonProfileEntity.class));

        verify(persistence).findProfileById(id);
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontFindPrincipalProfileById() {
        Long id = 501L;
        PrincipalProfile profile = makePrincipalProfile(id);
        assertThat(persistence.save(profile)).isEmpty();

        assertThat(persistence.findPrincipalProfileById(id)).isEmpty();

        verify(persistence).saveProfile(profile);
        verify(persistence).findProfileById(id);
        verify(repository, times(2)).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindProfileById() {
        StudentProfile profile = makeStudentProfile(null);
        Optional<? extends PersonProfile> personProfile = persistence.saveProfile(profile);
        assertThat(personProfile).isNotEmpty();
        Long id = personProfile.orElseThrow().getId();

        Optional<PersonProfile> person = persistence.findProfileById(id);

        assertThat(person).isNotEmpty();
        assertProfilesEquals((StudentProfile) person.orElse(null), profile, false);

        verify(persistence).saveProfile(profile);
        verify(repository).saveAndFlush(any(StudentProfileEntity.class));

        verify(persistence).findProfileById(id);
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontFindProfileById() {
        Long id = 502L;
        PersonProfile profile = makeStudentProfile(id);
        assertThat(persistence.saveProfile(profile)).isEmpty();

        Optional<PersonProfile> person = persistence.findProfileById(id);

        assertThat(person).isEmpty();
        verify(persistence).saveProfile(profile);
        verify(persistence).findProfileById(id);
        verify(repository, times(2)).findById(id);
        verify(repository, never()).saveAndFlush(any(StudentProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveStudentProfile() {
        StudentProfile profile = makeStudentProfile(null);

        Optional<StudentProfile> student = persistence.save(profile);

        assertThat(student).isNotEmpty();
        assertProfilesEquals(student.orElse(null), profile, false);
        verify(persistence).saveProfile(profile);
        verify(repository).saveAndFlush(any(StudentProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotSaveStudentProfile() {
        Long id = 502L;
        StudentProfile profile = makeStudentProfile(id);

        Optional<StudentProfile> student = persistence.save(profile);

        assertThat(student).isEmpty();
        verify(persistence).saveProfile(profile);
        verify(repository).findById(id);
        verify(repository, never()).saveAndFlush(any(PersonProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSavePrincipalProfile() {
        PrincipalProfile profile = makePrincipalProfile(null);

        Optional<PrincipalProfile> principal = persistence.save(profile);

        assertThat(principal).isNotEmpty();
        assertProfilesEquals(principal.orElse(null), profile, false);
        verify(persistence).saveProfile(profile);
        verify(repository).saveAndFlush(any(PrincipalProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotSavePrincipalProfile() {
        Long id = 503L;
        PrincipalProfile profile = makePrincipalProfile(id);

        Optional<PrincipalProfile> student = persistence.save(profile);

        assertThat(student).isEmpty();
        verify(persistence).saveProfile(profile);
        verify(repository).findById(id);
        verify(repository, never()).saveAndFlush(any(PersonProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveBaseProfile() {
        StudentProfile profile = makeStudentProfile(null);

        Optional<? extends PersonProfile> person = persistence.saveProfile(profile);

        assertThat(person).isNotEmpty();
        assertProfilesEquals((StudentProfile) person.orElse(null), profile, false);
        verify(repository).saveAndFlush(any(PersonProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontSaveBaseProfile() {
        Long id = 503L;
        PersonProfile profile = makeStudentProfile(id);

        Optional<? extends PersonProfile> person = persistence.saveProfile(profile);

        assertThat(person).isEmpty();
        verify(persistence).saveProfile(profile);
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteProfileById() throws ProfileNotFoundException {
        StudentProfile profile = makeStudentProfile(null);
        Optional<StudentProfile> student = persistence.save(profile);
        assertThat(student).isNotEmpty();
        Long id = student.get().getId();

        persistence.deleteProfileById(id);

        verify(repository).findById(id);
        assertThat(repository.findById(id)).isEmpty();
        verify(repository).deleteById(anyLong());
        verify(repository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontDeleteProfileById() {
        Long id = 504L;

        ProfileNotFoundException exception =
                assertThrows(ProfileNotFoundException.class, () -> persistence.deleteProfileById(id));

        verify(repository).findById(id);
        assertThat(exception.getMessage()).isEqualTo("PersonProfile with ID:504 is not exists.");
    }
}