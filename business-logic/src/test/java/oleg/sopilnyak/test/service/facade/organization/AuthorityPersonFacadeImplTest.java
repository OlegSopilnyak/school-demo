package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.AuthorityPersonPayload;
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
    CommandsFactory<AuthorityPersonCommand> factory = buildFactory();
    @Mock
    AuthorityPerson mockPerson;
    @Mock
    AuthorityPersonPayload mockPayload;
    @Mock
    Faculty mockFaculty;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;

    @Spy
    @InjectMocks
    AuthorityPersonFacadeImpl facade;

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
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
    }

    @Test
    void shouldFindAuthorityPersonById() {
        Long id = 301L;
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPayload);
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent();
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(mockPerson);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Create() {
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPayload);

        Optional<AuthorityPerson> result = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(result).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(mockPayload);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockPayload);
        verify(payloadMapper).toPayload(mockPerson);
        verify(payloadMapper, never()).toPayload(any(AuthorityPersonPayload.class));
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        when(payloadMapper.toPayload(mockPerson)).thenReturn(mockPayload);
        when(payloadMapper.toPayload(mockPayload)).thenReturn(mockPayload);
        when(persistenceFacade.save(mockPayload)).thenReturn(Optional.of(mockPayload));

        Optional<AuthorityPerson> result = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(result).contains(mockPayload);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(mockPayload);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockPayload);
        verify(payloadMapper).toPayload(mockPerson);
        verify(payloadMapper).toPayload(mockPayload);
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        Long id = 302L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
//        when(persistenceFacade.toEntity(mockPerson)).thenReturn(mockPerson);

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(mockPerson);
        verify(persistenceFacade).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        Long id = 303L;

        NotExistAuthorityPersonException thrown =
                assertThrows(NotExistAuthorityPersonException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
        Long id = 304L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
//        when(persistenceFacade.toEntity(mockPerson)).thenReturn(mockPerson);
        when(mockPerson.getFaculties()).thenReturn(List.of(mockFaculty));

        AuthorityPersonManageFacultyException thrown =
                assertThrows(AuthorityPersonManageFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:304 is managing faculties.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    private CommandsFactory<AuthorityPersonCommand> buildFactory() {
        return new AuthorityPersonCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade, payloadMapper)),
                        spy(new DeleteAuthorityPersonCommand(persistenceFacade, payloadMapper)),
                        spy(new FindAllAuthorityPersonsCommand(persistenceFacade)),
                        spy(new FindAuthorityPersonCommand(persistenceFacade))
                )
        );
    }
}