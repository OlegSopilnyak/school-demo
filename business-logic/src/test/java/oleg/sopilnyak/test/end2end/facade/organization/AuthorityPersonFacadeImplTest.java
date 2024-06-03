package oleg.sopilnyak.test.end2end.facade.organization;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class AuthorityPersonFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE = "organization.authority.person.delete";

    @Autowired
    PersistenceFacade database;

    AuthorityPersonPersistenceFacade persistence;
    CommandsFactory<AuthorityPersonCommand> factory;
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

        assertThat(persons).contains(person);
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
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAuthorityPersonById() {
        AuthorityPerson authorityPerson = persistAuthorityPerson();
        Long id = authorityPerson.getId();

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent().contains(authorityPerson);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateAuthorityPerson_Create() {
        AuthorityPerson authorityPerson = makeCleanAuthorityPerson(2);
        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), false);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(authorityPerson);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(authorityPerson);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        AuthorityPerson authorityPerson = persistAuthorityPerson();

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), true);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(authorityPerson);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(authorityPerson);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateFaculty() {
        Long id = 301L;
        AuthorityPerson authorityPersonSource = makeCleanAuthorityPerson(2);
        if (authorityPersonSource instanceof FakeAuthorityPerson source) {
            source.setId(id);
        }


        UnableExecuteCommandException thrown =
                assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateAuthorityPerson(authorityPersonSource));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(cause.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(authorityPersonSource);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        AuthorityPerson authorityPerson = persistAuthorityPerson();
        Long id = authorityPerson.getId();

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence).toEntity(authorityPerson);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() {
        Long id = 303L;

        NotExistAuthorityPersonException thrown =
                assertThrows(NotExistAuthorityPersonException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).isEqualTo("AuthorityPerson with ID:303 is not exists.");

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        AuthorityPerson authorityPersonSource = makeCleanAuthorityPerson(2);
        if (authorityPersonSource instanceof FakeAuthorityPerson person) {
            person.setFaculties(List.of(makeCleanFacultyNoDean(2)));
        }
        Optional<AuthorityPerson> authorityPerson = database.save(authorityPersonSource);
        assertThat(authorityPerson).isPresent();
        Long id = authorityPerson.get().getId();

        AuthorityPersonManageFacultyException thrown =
                assertThrows(AuthorityPersonManageFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is managing faculties.");
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence).toEntity(any(AuthorityPerson.class));
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    // private methods
    private CommandsFactory<AuthorityPersonCommand> buildFactory(AuthorityPersonPersistenceFacade persistenceFacade) {
        return new AuthorityPersonCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade)),
                        spy(new DeleteAuthorityPersonCommand(persistenceFacade)),
                        spy(new FindAllAuthorityPersonsCommand(persistenceFacade)),
                        spy(new FindAuthorityPersonCommand(persistenceFacade))
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
        return database.toEntity(entity);
    }
}