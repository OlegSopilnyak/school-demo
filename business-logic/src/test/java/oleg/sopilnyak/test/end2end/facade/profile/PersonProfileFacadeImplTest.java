package oleg.sopilnyak.test.end2end.facade.profile;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
//        facade = spy(new PersonProfileFacadeImpl<>(factory));
    }

    @AfterEach
    void tearDown() {
        reset(personProfileRepository);
    }
//
//    @Test
//    void shouldAllPartsBeReady() {
//        assertThat(facade).isNotNull();
//        assertThat(persistenceFacade).isNotNull();
//        assertThat(personProfileRepository).isNotNull();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldFindById() {
//        PersonProfile inputProfile = makeStudentProfile(null);
//        Long profileId = persist(inputProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//
//        Optional<PersonProfile> profile = facade.findById(profileId);
//
//        assertPersonProfilesEquals(profile.orElse(null), inputProfile, false);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindById() {
//        Long profileId = 400L;
//
//        Optional<PersonProfile> profile = facade.findById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldFindStudentProfileById() {
//        StudentProfile studentProfile = makeStudentProfile(null);
//        Long profileId = persist(studentProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(StudentProfileEntity.class));
//
//        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);
//
//        assertPersonProfilesEquals(profile.orElse(null), studentProfile, false);
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindStudentProfileById_NoData() {
//        Long profileId = 401L;
//
//        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindStudentProfileById_WrongProfileType() {
//        PrincipalProfile wrongStudentProfile = makePrincipalProfile(null);
//        Long profileId = persist(wrongStudentProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PrincipalProfileEntity.class));
//
//        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldFindPrincipalProfileById() {
//        PrincipalProfile principalProfile = makePrincipalProfile(null);
//        Long profileId = persist(principalProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PrincipalProfileEntity.class));
//
//        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);
//
//        assertProfilesEquals(profile.orElse(null), principalProfile, false);
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindPrincipalProfileById_NoData() {
//        Long profileId = 402L;
//
//        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindPrincipalProfileById_WrongProfileType() {
//        StudentProfile wrongPrincipalProfile = makeStudentProfile(null);
//        Long profileId = persist(wrongPrincipalProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//
//        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldCreateOrUpdateStudentProfile() {
//        StudentProfile studentProfile = makeStudentProfile(null);
//
//        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);
//
//        assertProfilesEquals(profile.orElse(null), studentProfile, false);
//        verify(facade).createOrUpdate(studentProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(studentProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).saveProfile(studentProfile);
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotCreateOrUpdateStudentProfile_Null() {
//
//        Exception exception = assertThrows(
//                UnableExecuteCommandException.class,
//                () -> facade.createOrUpdateProfile((StudentProfile) null)
//        );
//
//        assertThat(exception.getMessage()).isEqualTo("Cannot execute command 'profile.person.createOrUpdate'");
//        assertThat(exception.getCause()).isInstanceOf(NullPointerException.class);
//        verify(facade).createOrUpdate(null);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(null);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotCreateOrUpdateStudentProfile_WrongProfileType() {
//        PrincipalProfile wrongStudentProfile = makePrincipalProfile(null);
//        Long profileId = persist(wrongStudentProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//        StudentProfile studentProfile = makeStudentProfile(profileId);
//
//        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);
//
//        assertThat(profile).isEmpty();
//        verify(facade).createOrUpdate(studentProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(studentProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).saveProfile(studentProfile);
//        verify(personProfileRepository, atLeastOnce()).saveAndFlush(any(PersonProfileEntity.class));
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldCreateOrUpdatePrincipalProfile() {
//        PrincipalProfile principalProfile = makePrincipalProfile(null);
//
//        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile(principalProfile);
//
//        assertProfilesEquals(profile.orElse(null), principalProfile, false);
//        verify(facade).createOrUpdate(principalProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(principalProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).saveProfile(principalProfile);
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotCreateOrUpdatePrincipalProfile_Null() {
//
//        Exception exception = assertThrows(
//                UnableExecuteCommandException.class,
//                () -> facade.createOrUpdateProfile((PrincipalProfile) null)
//        );
//
//        assertThat(exception.getMessage()).isEqualTo("Cannot execute command 'profile.person.createOrUpdate'");
//        assertThat(exception.getCause()).isInstanceOf(NullPointerException.class);
//        verify(facade).createOrUpdate(null);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(null);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDeletePersonProfileById() throws NotExistProfileException {
//        PrincipalProfile wrongStudentProfile = makePrincipalProfile(null);
//        Long profileId = persist(wrongStudentProfile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//        assertThat(database.findProfileById(profileId)).isNotEmpty();
//
//        facade.deleteById(profileId);
//
//        assertThat(database.findProfileById(profileId)).isEmpty();
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository, atLeastOnce()).findById(profileId);
//        verify(persistenceFacade).deleteProfileById(profileId);
//        verify(personProfileRepository).deleteById(profileId);
//        verify(personProfileRepository).flush();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeletePersonProfile_ProfileNotExists() throws NotExistProfileException {
//        Long profileId = 415L;
//
//        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.deleteById(profileId));
//
//        assertThat(exception.getMessage()).isEqualTo("PersonProfile with ID:415 is not exists.");
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//        verify(persistenceFacade, never()).deleteProfileById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDeletePersonProfileByInstance() throws NotExistProfileException {
//        PrincipalProfile profile = makePrincipalProfile(null);
//        Long profileId = persist(profile).getId();
//        verify(personProfileRepository).saveAndFlush(any(PersonProfileEntity.class));
//        assertThat(database.findProfileById(profileId)).isNotEmpty();
//
//        facade.delete(makePrincipalProfile(profileId));
//
//        assertThat(database.findProfileById(profileId)).isEmpty();
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository, atLeastOnce()).findById(profileId);
//        verify(persistenceFacade).deleteProfileById(profileId);
//        verify(personProfileRepository).deleteById(profileId);
//        verify(personProfileRepository).flush();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeletePersonProfileByInstance_ProfileNotExists() throws NotExistProfileException {
//        Long profileId = 416L;
//        PersonProfile profile = makeStudentProfile(profileId);
//
//        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(profile));
//
//        assertThat(exception.getMessage()).isEqualTo("PersonProfile with ID:416 is not exists.");
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(personProfileRepository).findById(profileId);
//        verify(persistenceFacade, never()).deleteProfileById(profileId);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeletePersonProfileByInstance_NullId() {
//        PersonProfile profile = makePrincipalProfile(null);
//
//        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(profile));
//
//        assertThat(exception.getMessage()).startsWith("Wrong ");
//        verify(factory, never()).command(PROFILE_PERSON_DELETE_BY_ID);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeletePersonProfileInstance_NegativeId() {
//        PersonProfile profile = makePrincipalProfile(-416L);
//
//        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(profile));
//
//        assertThat(exception.getMessage()).startsWith("Wrong ");
//        verify(factory, never()).command(PROFILE_PERSON_DELETE_BY_ID);
//    }

    // private methods
    private CommandsFactory<?> buildFactory(PersistenceFacade persistenceFacade) {
        return mock(ProfileCommandsFactory.class);
//        return new ProfileCommandsFactory(
//                Set.of(
//                        spy(new FindProfileCommand(persistenceFacade)),
//                        spy(new CreateOrUpdateProfileCommand(persistenceFacade)),
//                        spy(new DeleteProfileCommand(persistenceFacade))
//                )
//        );
    }

    private PersonProfile persist(PersonProfile profile) {
        Optional<PersonProfile> saved = database.saveProfile(profile);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }
}