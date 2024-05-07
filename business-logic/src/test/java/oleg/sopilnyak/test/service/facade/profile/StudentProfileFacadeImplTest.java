package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.FindStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
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
    @Spy
    CommandsFactory<?> factory = buildFactory();

    @Spy
    @InjectMocks
    StudentProfileFacadeImpl facade;
    @Mock
    StudentProfile mock;

    @Test
    void shouldFindProfileById() {
        Long id = 700L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock));

        Optional<StudentProfile> faculty = facade.findStudentProfileById(id);

        assertThat(faculty).isPresent();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById() {
        Long id = 710L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        Optional<StudentProfile> faculty = facade.findStudentProfileById(id);

        assertThat(faculty).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = 711L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock(PrincipalProfile.class)));

        Optional<StudentProfile> profile = facade.findStudentProfileById(id);

        assertThat(profile).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile() {
        doCallRealMethod().when(persistence).save(mock);
        when(persistence.saveProfile(mock)).thenReturn(Optional.of(mock));

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(mock);

        assertThat(profile).isPresent();
        verify(facade).createOrUpdate(mock);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(mock);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(mock);
        verify(persistence).saveProfile(mock);
    }

    @Test
    void shouldNotCreateOrUpdateProfile() {
        doCallRealMethod().when(persistence).save(mock);

        Optional<StudentProfile> profile = facade.createOrUpdateProfile(mock);

        assertThat(profile).isEmpty();
        verify(facade).createOrUpdate(mock);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(mock);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(mock);
        verify(persistence).saveProfile(mock);
    }

    @Test
    void shouldDeleteProfileById() {
        Long id = 702L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock));
        when(persistence.toEntity(mock)).thenReturn(mock);

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(mock);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() throws NotExistProfileException {
        Long id = 715L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.deleteById(id));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:715 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 703L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        NotExistProfileException thrown = assertThrows(NotExistProfileException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:703 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldDeleteProfileInstance() throws NotExistProfileException {
        Long id = 714L;
        when(mock.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock));
        when(persistence.toEntity(mock)).thenReturn(mock);

        facade.delete(mock);

        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(mock);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_ProfileNotExists() throws NotExistProfileException {
        Long id = 716L;
        when(mock.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(mock));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:716 is not exists.");
        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        when(mock.getId()).thenReturn(id);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(mock));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        when(mock.getId()).thenReturn(null);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(mock));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    private CommandsFactory<StudentProfileCommand> buildFactory() {
        return new StudentProfileCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateStudentProfileCommand(persistence)),
                        spy(new FindStudentProfileCommand(persistence)),
                        spy(new DeleteStudentProfileCommand(persistence))
                )

        );
    }
}