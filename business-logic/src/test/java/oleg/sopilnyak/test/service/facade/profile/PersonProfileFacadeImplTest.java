package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PersonProfileFacadeImplTest {
    private static final String PROFILE_PERSON_FIND_BY_ID = "profile.person.findById";
    private static final String PROFILE_PERSON_CREATE_OR_UPDATE = "profile.person.createOrUpdate";
    private static final String PROFILE_PERSON_DELETE_BY_ID = "profile.person.deleteById";
    ProfilePersistenceFacade persistenceFacade = mock(ProfilePersistenceFacade.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();

    @Spy
    @InjectMocks
    PersonProfileFacadeImpl<?> facade;
//
//    @Test
//    void shouldFindById() {
//        Long profileId = 410L;
//        PersonProfile abstractProfile = mock(PersonProfile.class);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(abstractProfile));
//
//        Optional<PersonProfile> profile = facade.findById(profileId);
//
//        assertThat(profile).hasValue(abstractProfile);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//    }
//
//    @Test
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
//    }
//
//    @Test
//    void shouldFindStudentProfileById() {
//        Long profileId = 411L;
//        StudentProfile studentProfile = mock(StudentProfile.class);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(studentProfile));
//
//        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);
//
//        assertThat(profile).hasValue(studentProfile);
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//    }
//
//    @Test
//    void shouldNotFindStudentProfileById_EmptyData() {
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
//    }
//
//    @Test
//    void shouldNotFindStudentProfileById_WrongProfileType() {
//        Long profileId = 411L;
//        PrincipalProfile wrongStudentProfile = mock(PrincipalProfile.class);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(wrongStudentProfile));
//
//        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//    }
//
//    @Test
//    void shouldFindPrincipalProfileById() {
//        Long profileId = 412L;
//        PrincipalProfile principalProfile = mock(PrincipalProfile.class);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(principalProfile));
//
//        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);
//
//        assertThat(profile).hasValue(principalProfile);
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//    }
//
//    @Test
//    void shouldNotFindPrincipalProfileById() {
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
//    }
//
//    @Test
//    void shouldNotFindPrincipalProfileById_WrongProfileType() {
//        Long profileId = 402L;
//        StudentProfile wrongStudentProfile = mock(StudentProfile.class);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(wrongStudentProfile));
//
//        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(facade).findById(profileId);
//        verify(factory).command(PROFILE_PERSON_FIND_BY_ID);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//    }
//
//    @Test
//    void shouldCreateOrUpdateStudentProfile() {
//        StudentProfile studentProfile = mock(StudentProfile.class);
//        when(persistenceFacade.save(studentProfile)).thenReturn(Optional.of(studentProfile));
//
//        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);
//
//        assertThat(profile).hasValue(studentProfile);
//        verify(facade).createOrUpdate(studentProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(studentProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).save(studentProfile);
//    }
//
//    @Test
//    void shouldNotCreateOrUpdateStudentProfile() {
//        StudentProfile studentProfile = mock(StudentProfile.class);
//
//        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);
//
//        assertThat(profile).isEmpty();
//        verify(facade).createOrUpdate(studentProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(studentProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).save(studentProfile);
//    }
//
//    @Test
//    void shouldCreateOrUpdatePrincipalProfile() {
//        PrincipalProfile principalProfile = mock(PrincipalProfile.class);
//        when(persistenceFacade.save(principalProfile)).thenReturn(Optional.of(principalProfile));
//
//        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile(principalProfile);
//
//        assertThat(profile).hasValue(principalProfile);
//        verify(facade).createOrUpdate(principalProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(principalProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).save(principalProfile);
//    }
//
//    @Test
//    void shouldNotCreateOrUpdatePrincipalProfile() {
//        PrincipalProfile principalProfile = mock(PrincipalProfile.class);
//
//        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile(principalProfile);
//
//        assertThat(profile).isEmpty();
//        verify(facade).createOrUpdate(principalProfile);
//        verify(factory).command(PROFILE_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).createContext(principalProfile);
//        verify(factory.command(PROFILE_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
//        verify(persistenceFacade).save(principalProfile);
//    }
//
//    @Test
//    void shouldDeletePersonProfile() throws NotExistProfileException {
//        Long profileId = 414L;
//        PersonProfile profile = mock(PersonProfile.class);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(profile));
//
//        facade.deleteById(profileId);
//
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(persistenceFacade).deleteProfileById(profileId);
//    }
//
//    @Test
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
//        verify(persistenceFacade, never()).deleteProfileById(profileId);
//    }
//
//    @Test
//    void shouldDeletePersonProfileInstance() throws NotExistProfileException {
//        Long profileId = 414L;
//        PersonProfile profile = mock(PersonProfile.class);
//        when(profile.getId()).thenReturn(profileId);
//        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(profile));
//
//        facade.delete(profile);
//
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(persistenceFacade).deleteProfileById(profileId);
//    }
//
//    @Test
//    void shouldNotDeletePersonProfileInstance_ProfileNotExists() throws NotExistProfileException {
//        Long profileId = 416L;
//        PersonProfile profile = mock(PersonProfile.class);
//        when(profile.getId()).thenReturn(profileId);
//
//        NotExistProfileException exception = assertThrows(
//                NotExistProfileException.class,
//                () -> facade.delete(profile)
//        );
//
//        assertThat(exception.getMessage()).isEqualTo("PersonProfile with ID:416 is not exists.");
//        verify(factory).command(PROFILE_PERSON_DELETE_BY_ID);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).createContext(profileId);
//        verify(factory.command(PROFILE_PERSON_DELETE_BY_ID)).doCommand(any(Context.class));
//        verify(persistenceFacade).findProfileById(profileId);
//        verify(persistenceFacade, never()).deleteProfileById(profileId);
//    }
//
//    @Test
//    void shouldNotDeletePersonProfileInstance_NullId() {
//        Long profileId = null;
//        PersonProfile profile = mock(PersonProfile.class);
//        when(profile.getId()).thenReturn(profileId);
//
//        NotExistProfileException exception = assertThrows(
//                NotExistProfileException.class,
//                () -> facade.delete(profile)
//        );
//
//        assertThat(exception.getMessage()).startsWith("Wrong ");
//        verify(factory, never()).command(PROFILE_PERSON_DELETE_BY_ID);
//    }
//
//    @Test
//    void shouldNotDeletePersonProfileInstance_NegativeId() {
//        Long profileId = -416L;
//        PersonProfile profile = mock(PersonProfile.class);
//        when(profile.getId()).thenReturn(profileId);
//
//        NotExistProfileException exception = assertThrows(
//                NotExistProfileException.class,
//                () -> facade.delete(profile)
//        );
//
//        assertThat(exception.getMessage()).startsWith("Wrong ");
//        verify(factory, never()).command(PROFILE_PERSON_DELETE_BY_ID);
//    }

    private CommandsFactory<?> buildFactory() {
        return mock(ProfileCommandsFactory.class);
//        return new ProfileCommandsFactory(
//                Set.of(
//                        spy(new FindProfileCommand(persistenceFacade)),
//                        spy(new CreateOrUpdateProfileCommand(persistenceFacade)),
//                        spy(new DeleteProfileCommand(persistenceFacade))
//                )
//        );
    }
}