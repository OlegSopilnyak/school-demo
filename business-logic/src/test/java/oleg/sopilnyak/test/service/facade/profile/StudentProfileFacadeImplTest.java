package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.FindStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentProfileFacadeImplTest {
    private static final String PROFILE_FIND_BY_ID = "profile.student.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.student.deleteById";
    ProfilePersistenceFacade persistence = mock(ProfilePersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();

    @Spy
    @InjectMocks
    StudentProfileFacadeImpl facade;
    @Mock
    StudentProfile profile;
    @Mock
    StudentProfilePayload payload;

    @Test
    void shouldFindProfileById() {
        Long id = 700L;
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));

        Optional<StudentProfile> faculty = facade.findStudentProfileById(id);

        assertThat(faculty).contains(payload);
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(payloadMapper).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldNotFindProfileById() {
        Long id = 710L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        Optional<StudentProfile> faculty = facade.findStudentProfileById(id);

        assertThat(faculty).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = 711L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock(PrincipalProfile.class)));

        Optional<StudentProfile> profileFromDb = facade.findStudentProfileById(id);

        assertThat(profileFromDb).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile() {
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        doCallRealMethod().when(persistence).save(payload);
        when(persistence.saveProfile(payload)).thenReturn(Optional.of(payload));

        Optional<StudentProfile> result = facade.createOrUpdateProfile(profile);

        assertThat(result).contains(payload);
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(Input.of(payload));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(persistence).saveProfile(payload);
        verify(payloadMapper).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldNotCreateOrUpdateProfile() {
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        doCallRealMethod().when(persistence).save(payload);

        Optional<StudentProfile> result = facade.createOrUpdateProfile(profile);

        assertThat(result).isEmpty();
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(Input.of(payload));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(persistence).saveProfile(payload);
        verify(payloadMapper).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldDeleteProfileById() {
        Long id = 702L;
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(payloadMapper).toPayload(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() throws ProfileNotFoundException {
        Long id = 715L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.deleteById(id));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:715 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 703L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:703 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldDeleteProfileInstance() throws ProfileNotFoundException {
        Long id = 714L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        facade.delete(profile);

        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_ProfileNotExists() throws ProfileNotFoundException {
        Long id = 716L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:716 is not exists.");
        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        when(profile.getId()).thenReturn(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        when(profile.getId()).thenReturn(null);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    private CommandsFactory<StudentProfileCommand<?>> buildFactory() {
        return new StudentProfileCommandsFactory(
                Set.of(
                        spy(new FindStudentProfileCommand(persistence)),
                        spy(new CreateOrUpdateStudentProfileCommand(persistence, payloadMapper)),
                        spy(new DeleteStudentProfileCommand(persistence, payloadMapper))
                )

        );
    }
}