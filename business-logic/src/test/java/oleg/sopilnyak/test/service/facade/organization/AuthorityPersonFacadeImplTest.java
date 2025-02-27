package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.authority.*;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorityPersonFacadeImplTest {
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGIN = "organization.authority.person.login";
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGOUT = "organization.authority.person.logout";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW = "organization.authority.person.create.macro";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL = "organization.authority.person.delete.macro";

    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);

    CreateOrUpdateAuthorityPersonCommand createPersonCommand;
    CreateOrUpdatePrincipalProfileCommand createProfileCommand;
    DeleteAuthorityPersonCommand deletePersonCommand;
    DeletePrincipalProfileCommand deleteProfileCommand;
    DeleteAuthorityPersonMacroCommand deletePersonMacroCommand;

    CommandsFactory<AuthorityPersonCommand<?>> factory;
    AuthorityPersonFacadeImpl facade;

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
        createPersonCommand = spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade, payloadMapper));
        createProfileCommand = spy(new CreateOrUpdatePrincipalProfileCommand(persistenceFacade, payloadMapper));
        deletePersonCommand = spy(new DeleteAuthorityPersonCommand(persistenceFacade, payloadMapper));
        deleteProfileCommand = spy(new DeletePrincipalProfileCommand(persistenceFacade, payloadMapper));
        deletePersonMacroCommand = spy(new DeleteAuthorityPersonMacroCommand(deletePersonCommand, deleteProfileCommand, persistenceFacade, 10));
        deletePersonMacroCommand.runThreadPoolExecutor();
        factory = buildFactory();
        facade = spy(new AuthorityPersonFacadeImpl(factory, payloadMapper));
    }

    @Test
    void shouldLogoutAuthorityPerson() {
        String token = "logged_in_person_token";

        facade.logout(token);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT)).createContext(Input.of(token));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT)).doCommand(any(Context.class));
    }

    @Test
    void shouldLoginAuthorityPerson() {
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

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllAuthorityPersons();
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldFindAllAuthorityPersons_ExistsInstance() {
        when(persistenceFacade.findAllAuthorityPersons()).thenReturn(Set.of(mockPerson));

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).hasSize(1);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllAuthorityPersons();
        verify(payloadMapper).toPayload(mockPerson);
    }

    @Test
    void shouldNotFindAuthorityPersonById_NotFound() {
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldFindAuthorityPersonById() {
        Long id = 301L;
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent();
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldCreateNewAuthorityPerson() {
        when(mockPersonPayload.getFirstName()).thenReturn("John");
        when(mockPersonPayload.getLastName()).thenReturn("Doe");
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockPersonPayload)).thenReturn(mockPersonPayload);
        when(persistenceFacade.save(mockPersonPayload)).thenReturn(Optional.of(mockPersonPayload));
        when(persistenceFacade.save(any(PrincipalProfilePayload.class))).thenReturn(Optional.of(mockProfilePayload));

        Optional<AuthorityPerson> result = facade.create(mockPerson);

        assertThat(result.orElseThrow()).isEqualTo(mockPersonPayload);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).createContext(Input.of(mockPersonPayload));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockPersonPayload);
        verify(payloadMapper).toPayload(mockPerson);
        verify(payloadMapper, never()).toPayload(any(AuthorityPersonPayload.class));
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Create() {
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
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockPersonPayload)).thenReturn(mockPersonPayload);
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
        Long id = 302L;
        Long profileId = 402L;
        when(mockPerson.getProfileId()).thenReturn(profileId);
        when(persistenceFacade.findPrincipalProfileById(profileId)).thenReturn(Optional.of(mockProfile));
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(persistenceFacade.toEntity(mockProfile)).thenReturn(mockProfile);
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);

        facade.deleteAuthorityPersonById(id);

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
        Long id = 304L;
        Long profileId = 404L;
        when(mockPerson.getProfileId()).thenReturn(profileId);
        when(persistenceFacade.findPrincipalProfileById(profileId)).thenReturn(Optional.of(mockProfile));
        when(persistenceFacade.save(mockProfilePayload)).thenReturn(Optional.of(mockProfilePayload));
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(persistenceFacade.toEntity(mockProfile)).thenReturn(mockProfile);
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPersonPayload);
        when(payloadMapper.toPayload(mockProfile)).thenReturn(mockProfilePayload);
        when(mockPersonPayload.getFaculties()).thenReturn(List.of(mockFaculty));

        AuthorityPersonManagesFacultyException thrown =
                assertThrows(AuthorityPersonManagesFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:304 is managing faculties.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    private CommandsFactory<AuthorityPersonCommand<?>> buildFactory() {
        return spy(new AuthorityPersonCommandsFactory(
                        Set.of(
                                spy(new LoginAuthorityPersonCommand(persistenceFacade, payloadMapper)),
                                spy(new LogoutAuthorityPersonCommand()),
                                spy(new FindAllAuthorityPersonsCommand(persistenceFacade)),
                                spy(new FindAuthorityPersonCommand(persistenceFacade)),
                                createPersonCommand,
                                spy(new CreateAuthorityPersonMacroCommand(createPersonCommand, createProfileCommand, payloadMapper)),
                                deletePersonCommand,
                                deletePersonMacroCommand
                        )
                )
        );
    }
}