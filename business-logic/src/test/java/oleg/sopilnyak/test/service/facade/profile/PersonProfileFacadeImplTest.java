package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.profile.CreateProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.factory.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.facade.impl.PersonProfileFacadeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static oleg.sopilnyak.test.service.command.id.set.ProfileCommands.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonProfileFacadeImplTest<T> {
    ProfilePersistenceFacade persistenceFacade = mock(ProfilePersistenceFacade.class);
    @Spy
    CommandsFactory<T> factory = buildFactory();

    @Spy
    @InjectMocks
    PersonProfileFacadeImpl<T> facade;

    @Test
    void shouldFindById() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 410L;
        PersonProfile abstractProfile = mock(PersonProfile.class);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(abstractProfile));

        Optional<PersonProfile> profile = facade.findById(profileId);

        assertThat(profile).hasValue(abstractProfile);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldNotFindById() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 400L;

        Optional<PersonProfile> profile = facade.findById(profileId);

        assertThat(profile).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldFindStudentProfileById() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 411L;
        StudentProfile studentProfile = mock(StudentProfile.class);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(studentProfile));

        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);

        assertThat(profile).hasValue(studentProfile);
        verify(facade).findById(profileId);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldNotFindStudentProfileById_EmptyData() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 401L;

        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldNotFindStudentProfileById_WrongProfileType() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 411L;
        PrincipalProfile wrongStudentProfile = mock(PrincipalProfile.class);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(wrongStudentProfile));

        Optional<StudentProfile> profile = facade.findStudentProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldFindPrincipalProfileById() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 412L;
        PrincipalProfile principalProfile = mock(PrincipalProfile.class);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(principalProfile));

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);

        assertThat(profile).hasValue(principalProfile);
        verify(facade).findById(profileId);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldNotFindPrincipalProfileById() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 402L;

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldNotFindPrincipalProfileById_WrongProfileType() {
        String commandId = commandIdOf(FIND_BY_ID);
        Long profileId = 402L;
        StudentProfile wrongStudentProfile = mock(StudentProfile.class);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(wrongStudentProfile));

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(profileId);

        assertThat(profile).isEmpty();
        verify(facade).findById(profileId);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
    }

    @Test
    void shouldCreateOrUpdateStudentProfile() {
        String commandId = commandIdOf(CREATE_OR_UPDATE);
        StudentProfile studentProfile = mock(StudentProfile.class);
        when(persistenceFacade.saveProfile(studentProfile)).thenReturn(Optional.of(studentProfile));

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);

        assertThat(profile).hasValue(studentProfile);
        verify(facade).createOrUpdatePersonProfile(studentProfile);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentProfile);
        verify(persistenceFacade).saveProfile(studentProfile);
    }

    @Test
    void shouldNotCreateOrUpdateStudentProfile() {
        String commandId = commandIdOf(CREATE_OR_UPDATE);
        StudentProfile studentProfile = mock(StudentProfile.class);

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdatePersonProfile(studentProfile);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentProfile);
        verify(persistenceFacade).saveProfile(studentProfile);
    }

    @Test
    void shouldNotCreateOrUpdateStudentProfile_WrongProfileType() {
        String commandId = commandIdOf(CREATE_OR_UPDATE);
        StudentProfile studentProfile = mock(StudentProfile.class);
        PrincipalProfile wrongStudentProfile = mock(PrincipalProfile.class);
        when(persistenceFacade.saveProfile(studentProfile)).thenReturn(Optional.of(wrongStudentProfile));

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(studentProfile);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdatePersonProfile(studentProfile);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentProfile);
        verify(persistenceFacade).saveProfile(studentProfile);
    }

    @Test
    void shouldCreateOrUpdatePrincipalProfile() {
        String commandId = commandIdOf(CREATE_OR_UPDATE);
        PrincipalProfile principalProfile = mock(PrincipalProfile.class);
        when(persistenceFacade.saveProfile(principalProfile)).thenReturn(Optional.of(principalProfile));

        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile(principalProfile);

        assertThat(profile).hasValue(principalProfile);
        verify(facade).createOrUpdatePersonProfile(principalProfile);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(principalProfile);
        verify(persistenceFacade).saveProfile(principalProfile);
    }

    @Test
    void shouldNotCreateOrUpdatePrincipalProfile() {
        String commandId = commandIdOf(CREATE_OR_UPDATE);
        PrincipalProfile principalProfile = mock(PrincipalProfile.class);

        Optional<PrincipalProfile> profile = facade.createOrUpdateProfile(principalProfile);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdatePersonProfile(principalProfile);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(principalProfile);
        verify(persistenceFacade).saveProfile(principalProfile);
    }

    @Test
    void shouldDeletePersonProfile() throws ProfileNotExistsException {
        String commandId = commandIdOf(DELETE_BY_ID);
        Long profileId = 414L;
        PersonProfile profile = mock(PersonProfile.class);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(profile));

        facade.deleteProfileById(profileId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(persistenceFacade).deleteProfileById(profileId);
    }

    @Test
    void shouldNotDeletePersonProfile_ProfileNotExists() {
        String commandId = commandIdOf(DELETE_BY_ID);
        Long profileId = 415L;

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfileById(profileId));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:415 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(persistenceFacade, never()).deleteProfileById(profileId);
    }

    @Test
    void shouldDeletePersonProfileInstance() throws ProfileNotExistsException {
        String commandId = commandIdOf(DELETE_BY_ID);
        Long profileId = 414L;
        PersonProfile profile = mock(PersonProfile.class);
        when(profile.getId()).thenReturn(profileId);
        when(persistenceFacade.findProfileById(profileId)).thenReturn(Optional.of(profile));

        facade.deleteProfile(profile);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(persistenceFacade).deleteProfileById(profileId);
    }

    @Test
    void shouldNotDeletePersonProfileInstance_ProfileNotExists() {
        String commandId = commandIdOf(DELETE_BY_ID);
        Long profileId = 416L;
        PersonProfile profile = mock(PersonProfile.class);
        when(profile.getId()).thenReturn(profileId);

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfile(profile));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:416 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(profileId);
        verify(persistenceFacade).findProfileById(profileId);
        verify(persistenceFacade, never()).deleteProfileById(profileId);
    }

    @Test
    void shouldNotDeletePersonProfileInstance_NullId() {
        String commandId = commandIdOf(DELETE_BY_ID);
        Long profileId = 416L;
        PersonProfile profile = mock(PersonProfile.class);
        when(profile.getId()).thenReturn(null);

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfile(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(factory, never()).command(commandId);
        verify(factory.command(commandId), never()).execute(profileId);
        verify(persistenceFacade, never()).findProfileById(profileId);
        verify(persistenceFacade, never()).deleteProfileById(profileId);
    }

    @Test
    void shouldNotDeletePersonProfileInstance_NegativeId() {
        String commandId = commandIdOf(DELETE_BY_ID);
        Long profileId = -416L;
        PersonProfile profile = mock(PersonProfile.class);
        when(profile.getId()).thenReturn(profileId);

        ProfileNotExistsException exception = assertThrows(ProfileNotExistsException.class, () -> facade.deleteProfile(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(factory, never()).command(commandId);
        verify(factory.command(commandId), never()).execute(profileId);
        verify(persistenceFacade, never()).findProfileById(profileId);
        verify(persistenceFacade, never()).deleteProfileById(profileId);
    }

    private CommandsFactory<T> buildFactory() {
        return new ProfileCommandsFactory(
                Set.of(
                        spy(new FindProfileCommand(persistenceFacade)),
                        spy(new CreateProfileCommand(persistenceFacade)),
                        spy(new DeleteProfileCommand(persistenceFacade))
                )
        );
    }

    private static String commandIdOf(ProfileCommands command) {
        return command.id();
    }
}