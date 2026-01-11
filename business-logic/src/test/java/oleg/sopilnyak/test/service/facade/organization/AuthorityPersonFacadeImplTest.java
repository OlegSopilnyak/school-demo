package oleg.sopilnyak.test.service.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LoginAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LogoutAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.MacroDeleteAuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AuthorityPersonFacadeImplTest {
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGIN = "organization.authority.person.login";
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGOUT = "organization.authority.person.logout";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW = "organization.authority.person.create.macro";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL = "organization.authority.person.delete.macro";

    CommandActionExecutor actionExecutor = mock(CommandActionExecutor.class);
    @Mock
    ApplicationContext applicationContext;
    @Mock
    PersistenceFacade persistenceFacade;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;

    // person command
    CreateOrUpdateAuthorityPersonCommand createPersonCommand;
    DeleteAuthorityPersonCommand deletePersonCommand;
    DeleteAuthorityPersonMacroCommand deletePersonMacroCommand;
    LoginAuthorityPersonCommand loginPersonCommand;
    // profile command
    CreateOrUpdatePrincipalProfileCommand createProfileCommand;
    DeletePrincipalProfileCommand deleteProfileCommand;

    CommandsFactory<AuthorityPersonCommand<?>> factory;
    AuthorityPersonFacadeImpl facade;
    @Mock
    SchedulingTaskExecutor schedulingTaskExecutor;

    @Mock
    AuthorityPerson mockPerson;
    @Mock
    AuthorityPersonPayload mockPersonPayload;
    @Mock
    PrincipalProfile mockProfile;
    @Mock
    PrincipalProfilePayload mockProfilePayload;
    @Mock
    Faculty mockFaculty;

    @BeforeEach
    void setUp() {
        factory = buildFactory();
        facade = spy(new AuthorityPersonFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-action");
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @AfterEach
    void tearDown() {
        reset(schedulingTaskExecutor);
    }

    @Test
    void shouldLogoutAuthorityPerson() {
        String commandId = ORGANIZATION_AUTHORITY_PERSON_LOGOUT;
        AuthorityPersonCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("authorityPersonLogout", AuthorityPersonCommand.class);
        String token = "logged_in_person_token";

        facade.logout(token);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(token));
        verify(factory.command(commandId)).doCommand(any(Context.class));
    }

    @Test
    void shouldLoginAuthorityPerson() {
        doReturn(loginPersonCommand).when(applicationContext).getBean("authorityPersonLogin", AuthorityPersonCommand.class);
        Long id = 341L;
        String username = "test-login";
        String password = "test-password";
        when(mockProfilePayload.getId()).thenReturn(id);
        when(mockProfilePayload.isPassword(password)).thenReturn(true);
        when(persistenceFacade.findPrincipalProfileByLogin(username)).thenReturn(Optional.of(mockProfile));
        when(persistenceFacade.findAuthorityPersonByProfileId(id)).thenReturn(Optional.of(mockPerson));
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);

        Optional<AuthorityPerson> loggedIn = facade.login(username, password);

        assertThat(loggedIn).isPresent();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGIN);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).createContext(Input.of(username, password));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).doCommand(any(Context.class));
        verify(persistenceFacade).findPrincipalProfileByLogin(username);
        verify(persistenceFacade).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldNotLoginAuthorityPerson_WrongPassword() {
        doReturn(loginPersonCommand).when(applicationContext).getBean("authorityPersonLogin", AuthorityPersonCommand.class);
        String username = "test-login";
        when(persistenceFacade.findPrincipalProfileByLogin(username)).thenReturn(Optional.of(mockProfile));
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);

        SchoolAccessDeniedException thrown =
                assertThrows(SchoolAccessDeniedException.class, () -> facade.login(username, "password"));

        assertThat(thrown).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(thrown.getMessage()).isEqualTo("Login authority person command failed for username:" + username);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGIN);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).createContext(Input.of(username, "password"));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).doCommand(any(Context.class));
        verify(persistenceFacade).findPrincipalProfileByLogin(username);
        verify(persistenceFacade, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldFindAllAuthorityPersons_Empty() {
        String commandId = ORGANIZATION_AUTHORITY_PERSON_FIND_ALL;
        AuthorityPersonCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("authorityPersonFindAll", AuthorityPersonCommand.class);

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllAuthorityPersons();
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldFindAllAuthorityPersons_ExistsInstance() {
        String commandId = ORGANIZATION_AUTHORITY_PERSON_FIND_ALL;
        AuthorityPersonCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("authorityPersonFindAll", AuthorityPersonCommand.class);
        when(persistenceFacade.findAllAuthorityPersons()).thenReturn(Set.of(mockPerson));

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllAuthorityPersons();
        verify(payloadMapper).toPayload(mockPerson);
    }

    @Test
    void shouldNotFindAuthorityPersonById_NotFound() {
        String commandId = ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID;
        AuthorityPersonCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("authorityPersonFind", AuthorityPersonCommand.class);
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldFindAuthorityPersonById() {
        String commandId = ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID;
        AuthorityPersonCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("authorityPersonFind", AuthorityPersonCommand.class);
        Long id = 301L;
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent();
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldCreateNewAuthorityPerson() {
        doReturn(createPersonCommand).when(applicationContext).getBean("authorityPersonUpdate", AuthorityPersonCommand.class);
        doReturn(createProfileCommand).when(applicationContext).getBean("profilePrincipalUpdate", PrincipalProfileCommand.class);
        when(mockPersonPayload.getFirstName()).thenReturn("John");
        when(mockPersonPayload.getLastName()).thenReturn("Doe");
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);
        when(persistenceFacade.save(mockPersonPayload)).thenReturn(Optional.of(mockPerson));
        when(persistenceFacade.save(any(PrincipalProfilePayload.class))).thenReturn(Optional.of(mockProfile));

        Optional<AuthorityPerson> result = facade.create(mockPerson);

        assertThat(result.orElseThrow()).isEqualTo(mockPersonPayload);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).createContext(Input.of(mockPersonPayload));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockPersonPayload);
        verify(payloadMapper,times(2)).toPayload(mockPerson);
        verify(payloadMapper).toPayload(mockProfile);
        verify(payloadMapper, never()).toPayload(any(AuthorityPersonPayload.class));
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Create() {
        doReturn(createPersonCommand).when(applicationContext).getBean("authorityPersonUpdate", AuthorityPersonCommand.class);
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);

        Optional<AuthorityPerson> result = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(result).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(Input.of(mockPersonPayload));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockPersonPayload);
        verify(payloadMapper).toPayload(mockPerson);
        verify(payloadMapper, never()).toPayload(any(AuthorityPersonPayload.class));
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        doReturn(createPersonCommand).when(applicationContext).getBean("authorityPersonUpdate", AuthorityPersonCommand.class);
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(persistenceFacade.save(mockPersonPayload)).thenReturn(Optional.of(mockPersonPayload));

        Optional<AuthorityPerson> result = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(result).contains(mockPersonPayload);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(Input.of(mockPersonPayload));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockPersonPayload);
        verify(payloadMapper).toPayload(mockPerson);
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.initialize();
        doAnswer((Answer<Void>) invocationOnMock -> {
            threadPoolTaskExecutor.execute(invocationOnMock.getArgument(0, Runnable.class));
            return null;
        }).when(schedulingTaskExecutor).execute(any(Runnable.class));
        Long id = 302L;
        Long profileId = 402L;
        doReturn(deletePersonMacroCommand).when(applicationContext).getBean("authorityPersonMacroDelete", MacroDeleteAuthorityPerson.class);
        doReturn(deletePersonCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(deleteProfileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        when(mockPerson.getProfileId()).thenReturn(profileId);
        when(persistenceFacade.findPrincipalProfileById(profileId)).thenReturn(Optional.of(mockProfile));
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(persistenceFacade.toEntity(mockProfile)).thenReturn(mockProfile);
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);

        facade.deleteAuthorityPersonById(id);
        threadPoolTaskExecutor.shutdown();

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistenceFacade).findPrincipalProfileById(profileId);
        verify(payloadMapper).toPayload(mockPerson);
        verify(payloadMapper).toPayload(mockProfile);
        verify(persistenceFacade).deleteAuthorityPerson(id);
        verify(persistenceFacade).deleteProfileById(profileId);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        Long id = 303L;
        doReturn(deletePersonMacroCommand).when(applicationContext).getBean("authorityPersonMacroDelete", MacroDeleteAuthorityPerson.class);

        AuthorityPersonNotFoundException thrown =
                assertThrows(AuthorityPersonNotFoundException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.initialize();
        doAnswer((Answer<Void>) invocationOnMock -> {
            threadPoolTaskExecutor.execute(invocationOnMock.getArgument(0, Runnable.class));
            return null;
        }).when(schedulingTaskExecutor).execute(any(Runnable.class));
        Long id = 304L;
        Long profileId = 404L;
        doReturn(deletePersonMacroCommand).when(applicationContext).getBean("authorityPersonMacroDelete", MacroDeleteAuthorityPerson.class);
        doReturn(deletePersonCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(deleteProfileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        when(mockPerson.getProfileId()).thenReturn(profileId);
        when(persistenceFacade.findPrincipalProfileById(profileId)).thenReturn(Optional.of(mockProfile));
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(persistenceFacade.toEntity(mockProfile)).thenReturn(mockProfile);
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);
        when(mockPersonPayload.getFaculties()).thenReturn(List.of(mockFaculty));

        AuthorityPersonManagesFacultyException thrown =
                assertThrows(AuthorityPersonManagesFacultyException.class, () -> facade.deleteAuthorityPersonById(id));
        threadPoolTaskExecutor.shutdown();

        assertEquals("AuthorityPerson with ID:304 is managing faculties.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    private CommandsFactory<AuthorityPersonCommand<?>> buildFactory() {
        createPersonCommand = spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade, payloadMapper));
        createProfileCommand = spy(new CreateOrUpdatePrincipalProfileCommand(persistenceFacade, payloadMapper));
        deletePersonCommand = spy(new DeleteAuthorityPersonCommand(persistenceFacade, payloadMapper));
        deleteProfileCommand = spy(new DeletePrincipalProfileCommand(persistenceFacade, payloadMapper));
        deletePersonMacroCommand = spy(new DeleteAuthorityPersonMacroCommand(
                deletePersonCommand, deleteProfileCommand, schedulingTaskExecutor, persistenceFacade, actionExecutor
        ));
        loginPersonCommand = spy(new LoginAuthorityPersonCommand(persistenceFacade, payloadMapper));
        ReflectionTestUtils.setField(createProfileCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(deleteProfileCommand, "applicationContext", applicationContext);

        Map<AuthorityPersonCommand<?>, String> commands =  Map.of(
                loginPersonCommand, "authorityPersonLogin",
                spy(new LogoutAuthorityPersonCommand()), "authorityPersonLogout",
                spy(new FindAllAuthorityPersonsCommand(persistenceFacade, payloadMapper)), "authorityPersonFindAll",
                spy(new FindAuthorityPersonCommand(persistenceFacade, payloadMapper)), "authorityPersonFind",
                createPersonCommand, "authorityPersonUpdate",
                spy(new CreateAuthorityPersonMacroCommand(createPersonCommand, createProfileCommand, payloadMapper, actionExecutor)), "authorityPersonMacroCreate",
                deletePersonCommand, "authorityPersonDelete",
                deletePersonMacroCommand, "authorityPersonMacroDelete"
        );
        String acName = "applicationContext";
        commands.entrySet().forEach(entry -> {
            RootCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
        });
        return spy(new AuthorityPersonCommandsFactory(commands.keySet())
        );
    }
}