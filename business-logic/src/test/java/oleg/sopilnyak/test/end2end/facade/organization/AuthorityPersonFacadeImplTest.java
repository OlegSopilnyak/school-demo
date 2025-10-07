package oleg.sopilnyak.test.end2end.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LoginAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LogoutAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class AuthorityPersonFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGIN = "organization.authority.person.login";
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGOUT = "organization.authority.person.logout";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW = "organization.authority.person.create.macro";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL = "organization.authority.person.delete.macro";

    @SpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @SpyBean
    @Autowired
    SchedulingTaskExecutor schedulingTaskExecutor;
    @Autowired
    PersistenceFacade database;

    PersistenceFacade persistence;
    CommandsFactory<AuthorityPersonCommand<?>> factory;
    AuthorityPersonFacadeImpl facade;
    BusinessMessagePayloadMapper payloadMapper;

    @BeforeEach
    void setUp() {
        payloadMapper = spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
        persistence = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistence));
        facade = spy(new AuthorityPersonFacadeImpl(factory, payloadMapper));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(database).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLogoutAuthorityPerson() {
        String token = "logged_in_person_token";

        facade.logout(token);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT)).createContext(Input.of(token));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT)).doCommand(any(Context.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLoginAuthorityPerson() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload person = createAuthorityPerson();
        assertThat(database.findAuthorityPersonById(person.getId())).contains(person.getOriginal());
        assertThat(database.findAuthorityPersonByProfileId(person.getProfileId())).contains(person.getOriginal());
        setPersonPermissions(person, username, password);

        Optional<AuthorityPerson> loggedIn = facade.login(username, password);

        assertThat(loggedIn).isPresent().contains(person);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGIN);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).createContext(Input.of(username, password));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(person.getProfileId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotLoginAuthorityPerson_WrongPassword() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload person = createAuthorityPerson();
        assertThat(database.findAuthorityPersonById(person.getId())).contains(person.getOriginal());
        assertThat(database.findAuthorityPersonByProfileId(person.getProfileId())).contains(person.getOriginal());
        setPersonPermissions(person, username, password);

        SchoolAccessDeniedException thrown =
                assertThrows(SchoolAccessDeniedException.class, () -> facade.login(username, "password"));

        assertThat(thrown).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(thrown.getMessage()).isEqualTo("Login authority person command failed for username:" + username);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGIN);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).createContext(Input.of(username, "password"));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(person.getProfileId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllAuthorityPersons_Empty() {

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllAuthorityPersons_ExistsInstance() {
        AuthorityPerson person = persistAuthorityPerson();
        assertThat(database.findAuthorityPersonById(person.getId())).contains(person);

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).contains(payloadMapper.toPayload(person));
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindAuthorityPersonById_NotFound() {
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAuthorityPersonById() {
        AuthorityPerson authorityPerson = payloadMapper.toPayload(persistAuthorityPerson());
        Long id = authorityPerson.getId();

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent().contains(authorityPerson);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateAuthorityPerson_Create() {
        AuthorityPerson authorityPerson = payloadMapper.toPayload(makeCleanAuthorityPerson(2));

        Optional<AuthorityPerson> person = facade.create(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), false);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).createContext(Input.of(authorityPerson));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).doCommand(any(Context.class));
        verify(persistence).save(authorityPerson);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        AuthorityPerson authorityPerson = payloadMapper.toPayload(persistAuthorityPerson());

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), true);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(Input.of(authorityPerson));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(authorityPerson);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateFaculty() {
        Long id = 301L;
        AuthorityPersonPayload authorityPersonSource = payloadMapper.toPayload(makeCleanAuthorityPerson(2));
        authorityPersonSource.setId(id);
        reset(payloadMapper);


        UnableExecuteCommandException thrown = assertThrows(
                UnableExecuteCommandException.class,
                () -> facade.createOrUpdateAuthorityPerson(authorityPersonSource)
        );

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(Input.of(authorityPersonSource));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        Long id = createAuthorityPerson().getId();

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistence, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() {
        Long id = 303L;

        AuthorityPersonNotFoundException thrown =
                assertThrows(AuthorityPersonNotFoundException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).isEqualTo("AuthorityPerson with ID:303 is not exists.");

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        AuthorityPerson authorityPersonSource = makeCleanAuthorityPerson(2);
        if (authorityPersonSource instanceof FakeAuthorityPerson person) {
            person.setFaculties(List.of(makeCleanFacultyNoDean(2)));
        }
        Optional<AuthorityPerson> authorityPerson = facade.create(authorityPersonSource);
        assertThat(authorityPerson).isPresent();
        Long id = authorityPerson.orElseThrow().getId();

        AuthorityPersonManagesFacultyException thrown =
                assertThrows(AuthorityPersonManagesFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is managing faculties.");
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistence, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    // private methods
    private CommandsFactory<AuthorityPersonCommand<?>> buildFactory(PersistenceFacade persistenceFacade) {
        CreateOrUpdateAuthorityPersonCommand createOrUpdateAuthorityPersonCommand =
                spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade, payloadMapper));
        CreateOrUpdatePrincipalProfileCommand createOrUpdatePrincipalProfileCommand =
                spy(new CreateOrUpdatePrincipalProfileCommand(persistenceFacade, payloadMapper));
        CreateAuthorityPersonMacroCommand createAuthorityPersonMacroCommand =
                spy(new CreateAuthorityPersonMacroCommand(
                        createOrUpdateAuthorityPersonCommand, createOrUpdatePrincipalProfileCommand, payloadMapper, actionExecutor
                ));
        DeleteAuthorityPersonCommand deleteAuthorityPersonCommand =
                spy(new DeleteAuthorityPersonCommand(persistenceFacade, payloadMapper));
        DeletePrincipalProfileCommand deletePrincipalProfileCommand =
                spy(new DeletePrincipalProfileCommand(persistenceFacade, payloadMapper));
        DeleteAuthorityPersonMacroCommand deleteAuthorityPersonMacroCommand =
                spy(new DeleteAuthorityPersonMacroCommand(
                        deleteAuthorityPersonCommand, deletePrincipalProfileCommand, persistenceFacade, actionExecutor, schedulingTaskExecutor
                ));
        return new AuthorityPersonCommandsFactory(
                Set.of(
                        spy(new LoginAuthorityPersonCommand(persistenceFacade, payloadMapper)),
                        spy(new LogoutAuthorityPersonCommand()),
                        createOrUpdateAuthorityPersonCommand,
                        createAuthorityPersonMacroCommand,
                        deleteAuthorityPersonCommand,
                        deleteAuthorityPersonMacroCommand,
                        spy(new FindAllAuthorityPersonsCommand(persistenceFacade, payloadMapper)),
                        spy(new FindAuthorityPersonCommand(persistenceFacade, payloadMapper))
                )
        );
    }

    private AuthorityPerson persistAuthorityPerson() {
        AuthorityPerson authorityPerson = makeCleanAuthorityPerson(1);
        AuthorityPerson entity = database.save(authorityPerson).orElse(null);
        assertThat(entity).isNotNull();
        Optional<AuthorityPerson> dbAuthorityPerson = database.findAuthorityPersonById(entity.getId());
        assertAuthorityPersonEquals(dbAuthorityPerson.orElseThrow(), authorityPerson, false);
        assertThat(dbAuthorityPerson).contains(entity);
        return entity;
    }

    private AuthorityPersonPayload createAuthorityPerson() {
        AuthorityPerson authorityPerson = makeCleanAuthorityPerson(11);
        AuthorityPerson entity = facade.create(authorityPerson).orElse(null);
        assertThat(entity).isNotNull();
        Optional<AuthorityPerson> dbAuthorityPerson = database.findAuthorityPersonById(entity.getId());
        assertAuthorityPersonEquals(dbAuthorityPerson.orElseThrow(), authorityPerson, false);
        if (entity instanceof AuthorityPersonPayload payload) {
            assertThat(dbAuthorityPerson).contains(payload.getOriginal());
            return payload;
        } else {
            assertThat(dbAuthorityPerson).contains(entity);
        }
        fail("Entity is not payload");
        return null;
    }

    private void setPersonPermissions(AuthorityPersonPayload person, String username, String password) {
        try {
            assertThat(persistence.updateAuthorityPersonAccess(person, username, password)).isTrue();
        } finally {
            reset(persistence);
        }
    }
}