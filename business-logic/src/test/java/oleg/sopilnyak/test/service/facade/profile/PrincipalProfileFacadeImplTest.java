package oleg.sopilnyak.test.service.facade.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
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
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.FindPrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

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
class PrincipalProfileFacadeImplTest {
    private static final String FIND_BY_ID = "school::person::profile::principal:find.By.Id";
    private static final String CREATE_OR_UPDATE = "school::person::profile::principal:create.Or.Update";
    private static final String DELETE_BY_ID = "school::person::profile::principal:delete.By.Id";

    @Mock
    ProfilePersistenceFacade persistence;
    @Mock
    ApplicationContext applicationContext;
    FindPrincipalProfileCommand findCommand;
    CreateOrUpdatePrincipalProfileCommand updateCommand;
    DeletePrincipalProfileCommand deleteCommand;
    CommandsFactory<PrincipalProfileCommand<?>> factory;
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    PrincipalProfileFacadeImpl facade;
    @Mock
    CommandActionExecutor actionExecutor;

    @Mock
    PrincipalProfile profile;
    @Mock
    PrincipalProfilePayload payload;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory());
        facade = spy(new PrincipalProfileFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-action");
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldFindProfileById_Unified() {
        String commandId = FIND_BY_ID;
        doReturn(findCommand).when(applicationContext).getBean("profilePrincipalFind", PrincipalProfileCommand.class);
        Long id = 600L;
        doReturn(payload).when(payloadMapper).toPayload(profile);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doReturn(Optional.of(profile)).when(persistence).findProfileById(id);

        Optional<PrincipalProfile> result = facade.doActionAndResult(commandId, id);

        assertThat(result).contains(payload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(payloadMapper).toPayload(profile);
    }

    @Test
    void shouldFindProfileById() {
        String commandId = FIND_BY_ID;
        doReturn(findCommand).when(applicationContext).getBean("profilePrincipalFind", PrincipalProfileCommand.class);
        Long id = 600L;
        doReturn(payload).when(payloadMapper).toPayload(profile);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doReturn(Optional.of(profile)).when(persistence).findProfileById(id);

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).contains(payload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(payloadMapper).toPayload(profile);
    }

    @Test
    void shouldNotFindProfileById() {
        String commandId = FIND_BY_ID;
        doReturn(findCommand).when(applicationContext).getBean("profilePrincipalFind", PrincipalProfileCommand.class);
        Long id = 610L;

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        String commandId = FIND_BY_ID;
        doReturn(findCommand).when(applicationContext).getBean("profilePrincipalFind", PrincipalProfileCommand.class);
        Long id = 611L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock(StudentProfile.class)));

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile_Unified() {
        String commandId = CREATE_OR_UPDATE;
        doReturn(updateCommand).when(applicationContext).getBean("profilePrincipalUpdate", PrincipalProfileCommand.class);
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        when(persistence.save(payload)).thenReturn(Optional.of(payload));

        Optional<PrincipalProfile> result = facade.doActionAndResult(commandId, payload);

        assertThat(result).contains(payload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(payload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(payloadMapper, never()).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldCreateOrUpdateProfile() {
        String commandId = CREATE_OR_UPDATE;
        doReturn(updateCommand).when(applicationContext).getBean("profilePrincipalUpdate", PrincipalProfileCommand.class);
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        when(persistence.save(payload)).thenReturn(Optional.of(payload));

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", payload);

        assertThat(result).contains(payload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(payload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(payloadMapper, never()).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldNotCreateOrUpdateProfile() {
        String commandId = CREATE_OR_UPDATE;
        doReturn(updateCommand).when(applicationContext).getBean("profilePrincipalUpdate", PrincipalProfileCommand.class);
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", payload);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(payload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(payloadMapper, never()).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldDeleteProfileById_Unified() {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 602L;
        when(persistence.findPrincipalProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        Object result = facade.doActionAndResult(commandId, id);

        assertThat(result).isNull();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldDeleteProfileById() {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 602L;
        when(persistence.findPrincipalProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        ReflectionTestUtils.invokeMethod(facade, "internalDelete", id);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() throws ProfileNotFoundException {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 615L;

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", id)
        );

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:615 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 603L;

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", id)
        );

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:603 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldDeleteProfileInstance() throws ProfileNotFoundException {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 614L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldDeleteProfileInstance_Unified() throws ProfileNotFoundException {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 614L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        Object result = facade.doActionAndResult(commandId, profile);

        assertThat(result).isNull();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_ProfileNotExists() throws ProfileNotFoundException {
        String commandId = DELETE_BY_ID;
        doReturn(deleteCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long id = 716L;
        when(profile.getId()).thenReturn(id);

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile)
        );

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:716 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        reset(actionExecutor);
        Long id = -716L;
        when(profile.getId()).thenReturn(id);

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile)
        );

        assertThat(thrown.getMessage()).startsWith("Wrong ");
        verify(factory, never()).command(DELETE_BY_ID);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        reset(actionExecutor);

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile)
        );

        assertThat(thrown.getMessage()).startsWith("Wrong ");
        verify(factory, never()).command(DELETE_BY_ID);
    }

    // private methods
    private CommandsFactory<PrincipalProfileCommand<?>> buildFactory() {
        findCommand = spy(new FindPrincipalProfileCommand(persistence, payloadMapper));
        updateCommand = spy(new CreateOrUpdatePrincipalProfileCommand(persistence, payloadMapper));
        deleteCommand = spy(new DeletePrincipalProfileCommand(persistence, payloadMapper));
        ReflectionTestUtils.setField(findCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(updateCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(deleteCommand, "applicationContext", applicationContext);

        return new PrincipalProfileCommandsFactory(Set.of(findCommand, updateCommand, deleteCommand));
    }
}