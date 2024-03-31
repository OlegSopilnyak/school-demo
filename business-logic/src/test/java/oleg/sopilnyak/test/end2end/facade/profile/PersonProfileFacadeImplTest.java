package oleg.sopilnyak.test.end2end.facade.profile;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.factory.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.facade.impl.PersonProfileFacadeImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class PersonProfileFacadeImplTest extends MysqlTestModelFactory {
    private static final String PROFILE_PERSON_FIND_BY_ID = "profile.person.findById";
    private static final String PROFILE_PERSON_CREATE_OR_UPDATE = "profile.person.createOrUpdate";
    private static final String PROFILE_PERSON_DELETE_BY_ID = "profile.person.deleteById";

    @SpyBean
    @Autowired
    PersonProfileRepository<PersonProfileEntity> personProfileRepository;
    @Autowired
    PersistenceFacade database;
    PersistenceFacade persistenceFacade;
    CommandsFactory<?> factory;
    PersonProfileFacadeImpl<?> facade;

    @BeforeEach
    void setUp() {
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new PersonProfileFacadeImpl<>(factory));
    }

    @AfterEach
    void tearDown() {
        reset(personProfileRepository);
    }

    @Test
    void shouldAllPartsBeReady() {
        assertThat(facade).isNotNull();
        assertThat(persistenceFacade).isNotNull();
        assertThat(personProfileRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        PersonProfile inputProfile = makeStudentProfile(null);
        Long profileId = persist(inputProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));

        Optional<PersonProfile> profile = facade.findById(profileId);

        assertPersonProfilesEquals(profile.orElse(null), inputProfile, false);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindById() {
        Long profileId = 400L;

        Optional<PersonProfile> profile = facade.findById(profileId);

        assertThat(profile).isEmpty();
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentProfileById() {
        StudentProfile studentProfile = makeStudentProfile(null);
        Long profileId = persist(studentProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(StudentProfileEntity.class));

        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);

        assertPersonProfilesEquals(profile.orElse(null), studentProfile, false);
        verify(facade).findById(profileId);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindStudentProfileById_NoData() {
        Long profileId = 401L;

        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindStudentProfileById_WrongProfileType() {
        PrincipalProfile wrongStudentProfile = makePrincipalProfile(null);
        Long profileId = persist(wrongStudentProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(PrincipalProfileEntity.class));

        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindPrincipalProfileById() {
        PrincipalProfile principalProfile = makePrincipalProfile(null);
        Long profileId = persist(principalProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(PrincipalProfileEntity.class));

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);

        assertProfilesEquals(profile.orElse(null), principalProfile, false);
        verify(facade).findById(profileId);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindPrincipalProfileById_NoData() {
        Long profileId = 402L;

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindPrincipalProfileById_WrongProfileType() {
        StudentProfile wrongPrincipalProfile = makeStudentProfile(null);
        Long profileId = persist(wrongPrincipalProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateStudentProfile() {
        StudentProfile studentProfile = makeStudentProfile(null);

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);

        assertProfilesEquals(profile.orElse(null), studentProfile, false);
        verify(facade).createOrUpdatePersonProfile(studentProfile);
        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).execute(studentProfile);
        verify(persistenceFacade).saveProfile(studentProfile);
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateStudentProfile_Null() {

        Optional<StudentProfile> profile = facade.createOrUpdateProfile((StudentProfile) null);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdatePersonProfile(null);
        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).execute(null);
        verify(persistenceFacade).saveProfile(null);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateStudentProfile_WrongProfileType() {
        PrincipalProfile wrongStudentProfile = makePrincipalProfile(null);
        Long profileId = persist(wrongStudentProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
        StudentProfile studentProfile = makeStudentProfile(profileId);

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdatePersonProfile(studentProfile);
        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).execute(studentProfile);
        verify(persistenceFacade).saveProfile(studentProfile);
        verify(personProfileRepository, atLeastOnce()).saveAndFlush(any(PersonProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdatePrincipalProfile() {
        PrincipalProfile principalProfile = makePrincipalProfile(null);

        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile(principalProfile);

        assertProfilesEquals(profile.orElse(null), principalProfile, false);
        verify(facade).createOrUpdatePersonProfile(principalProfile);
        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).execute(principalProfile);
        verify(persistenceFacade).saveProfile(principalProfile);
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdatePrincipalProfile_Null() {

        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile((PrincipalProfile) null);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdatePersonProfile(null);
        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).execute(null);
        verify(persistenceFacade).saveProfile(null);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeletePersonProfileById() throws ProfileNotExistsException {
        PrincipalProfile wrongStudentProfile = makePrincipalProfile(null);
        Long profileId = persist(wrongStudentProfile).getId();
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
        assertThat(database.findProfileById(profileId)).isNotEmpty();

        facade.deleteProfileById(profileId);

        assertThat(database.findProfileById(profileId)).isEmpty();
        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository, atLeastOnce()).findById(profileId);
        verify(persistenceFacade).deleteProfileById(profileId);
        verify(personProfileRepository).deleteById(profileId);
        verify(personProfileRepository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeletePersonProfile_ProfileNotExists() throws ProfileNotExistsException {
        Long profileId = 415L;

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfileById(profileId));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:415 is not exists.");
        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
        verify(persistenceFacade, never()).deleteProfileById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeletePersonProfileByInstance() throws ProfileNotExistsException {
        PrincipalProfile profile = makePrincipalProfile(null);
        Long profileId = persist(profile).getId();
        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
        assertThat(database.findProfileById(profileId)).isNotEmpty();

        facade.deleteProfile(makePrincipalProfile(profileId));

        assertThat(database.findProfileById(profileId)).isEmpty();
        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository, atLeastOnce()).findById(profileId);
        verify(persistenceFacade).deleteProfileById(profileId);
        verify(personProfileRepository).deleteById(profileId);
        verify(personProfileRepository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeletePersonProfileByInstance_ProfileNotExists() throws ProfileNotExistsException {
        Long profileId = 416L;
        PersonProfile profile = makeStudentProfile(profileId);

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfile(profile));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:416 is not exists.");
        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(personProfileRepository).findById(profileId);
        verify(persistenceFacade, never()).deleteProfileById(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeletePersonProfileByInstance_NullId() {
        PersonProfile profile = makePrincipalProfile(null);

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfile(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(factory, never()).command(PROFILE_PERSON_DELETE_BY_ID);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeletePersonProfileInstance_NegativeId() {
        PersonProfile profile = makePrincipalProfile(-416L);

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfile(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(factory, never()).command(PROFILE_PERSON_DELETE_BY_ID);
    }

    // private methods
    private CommandsFactory<?> buildFactory(PersistenceFacade persistenceFacade) {
        return new ProfileCommandsFactory(
                Set.of(
                        spy(new FindProfileCommand(persistenceFacade)),
                        spy(new CreateOrUpdateProfileCommand(persistenceFacade)),
                        spy(new DeleteProfileCommand(persistenceFacade))
                )
        );
    }

    private PersonProfile persist(PersonProfile profile) {
        Optional<PersonProfile> saved = database.saveProfile(profile);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}