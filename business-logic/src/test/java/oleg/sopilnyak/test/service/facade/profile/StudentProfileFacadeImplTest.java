package oleg.sopilnyak.test.service.facade.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
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
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class StudentProfileFacadeImplTest {
    private static final String PROFILE_FIND_BY_ID = "profile.student.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.student.deleteById";

    @Mock
    ProfilePersistenceFacade persistence;
    @Mock
    ApplicationContext applicationContext;
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    FindStudentProfileCommand findCommand;
    CreateOrUpdateStudentProfileCommand updateCommand;
    DeleteStudentProfileCommand deleteCommand;
    CommandsFactory<StudentProfileCommand<?>> factory;
    StudentProfileFacadeImpl facade;
    @Mock
    CommandActionExecutor actionExecutor;

    @Mock
    StudentProfile profile;
    @Mock
    StudentProfilePayload payload;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory());
        facade = spy(new StudentProfileFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-doingMainLoop");
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldFindProfileById() {
        doReturn(findCommand).when(applicationContext).getBean("profileStudentFind", StudentProfileCommand.class);
        Long id = 700L;
        when(payloadMapper.toPayload(profile)).thenReturn(payload);
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
        verify(payloadMapper).toPayload(profile);
    }

    @Test
    void shouldNotFindProfileById() {
        doReturn(findCommand).when(applicationContext).getBean("profileStudentFind", StudentProfileCommand.class);
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
        doReturn(findCommand).when(applicationContext).getBean("profileStudentFind", StudentProfileCommand.class);
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
        doReturn(updateCommand).when(applicationContext).getBean("profileStudentUpdate", StudentProfileCommand.class);
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
        doReturn(updateCommand).when(applicationContext).getBean("profileStudentUpdate", StudentProfileCommand.class);
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
        doReturn(deleteCommand).when(applicationContext).getBean("profileStudentDelete", StudentProfileCommand.class);
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
        doReturn(deleteCommand).when(applicationContext).getBean("profileStudentDelete", StudentProfileCommand.class);
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
        doReturn(deleteCommand).when(applicationContext).getBean("profileStudentDelete", StudentProfileCommand.class);
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
        doReturn(deleteCommand).when(applicationContext).getBean("profileStudentDelete", StudentProfileCommand.class);
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
        doReturn(deleteCommand).when(applicationContext).getBean("profileStudentDelete", StudentProfileCommand.class);
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
        reset(actionExecutor);
        Long id = -716L;
        when(profile.getId()).thenReturn(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        reset(actionExecutor);
        when(profile.getId()).thenReturn(null);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    private CommandsFactory<StudentProfileCommand<?>> buildFactory() {
        findCommand = spy(new FindStudentProfileCommand(persistence, payloadMapper));
        updateCommand = spy(new CreateOrUpdateStudentProfileCommand(persistence, payloadMapper));
        deleteCommand = spy(new DeleteStudentProfileCommand(persistence, payloadMapper));
        ReflectionTestUtils.setField(findCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(updateCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(deleteCommand, "applicationContext", applicationContext);

        return new StudentProfileCommandsFactory(Set.of(findCommand, updateCommand, deleteCommand));
    }
}