package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.profile.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonProfileFacadeImplTest<T> {
    ProfilePersistenceFacade persistenceFacade = mock(ProfilePersistenceFacade.class);
    @Spy
    CommandsFactory<T> factory = buildFactory();

    @Spy
    @InjectMocks
    PersonProfileFacadeImpl facade;

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

    private CommandsFactory<T> buildFactory() {
        return new ProfileCommandsFactory(
                Set.of(
                        spy(new FindProfileCommand(persistenceFacade)),
                        spy(new CreateProfileCommand(persistenceFacade))
                )
        );
    }

    private static String commandIdOf(ProfileCommands command) {
        return command.toString();
    }
}