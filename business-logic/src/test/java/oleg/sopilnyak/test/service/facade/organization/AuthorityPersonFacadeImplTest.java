package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.factory.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.facade.impl.AuthorityPersonFacadeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE = "organization.authority.person.delete";
    AuthorityPersonPersistenceFacade persistenceFacade = mock(AuthorityPersonPersistenceFacade.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();
    @Mock
    AuthorityPerson mockPerson;
    @Mock
    Faculty mockFaculty;

    @Spy
    @InjectMocks
    AuthorityPersonFacadeImpl facade;

    @Test
    void shouldFindAllAuthorityPersons_Empty() {

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldFindAllAuthorityPersons_ExistsInstance() {
        when(persistenceFacade.findAllAuthorityPersons()).thenReturn(Set.of(mockPerson));

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).hasSize(1);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldNotGetAuthorityPersonById_NotFound() {
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.getAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
    }

    @Test
    void shouldGetAuthorityPersonById() {
        Long id = 301L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.getAuthorityPersonById(id);

        assertThat(person).isPresent();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Create() {

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).execute(mockPerson);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        when(persistenceFacade.save(mockPerson)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isPresent();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).execute(mockPerson);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        Long id = 302L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        Long id = 303L;

        NotExistAuthorityPersonException thrown =
                assertThrows(NotExistAuthorityPersonException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        Long id = 304L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(mockPerson.getFaculties()).thenReturn(List.of(mockFaculty));

        AuthorityPersonManageFacultyException thrown =
                assertThrows(AuthorityPersonManageFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:304 is managing faculties.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    private CommandsFactory<?> buildFactory() {
        return new AuthorityPersonCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade)),
                        spy(new DeleteAuthorityPersonCommand(persistenceFacade)),
                        spy(new FindAllAuthorityPersonsCommand(persistenceFacade)),
                        spy(new FindAuthorityPersonCommand(persistenceFacade))
                )
        );
    }
}