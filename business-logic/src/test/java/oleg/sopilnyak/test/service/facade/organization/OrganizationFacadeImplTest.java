package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.service.command.executable.organization.*;
import oleg.sopilnyak.test.service.command.factory.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.id.set.AuthorityPersonCommands;
import oleg.sopilnyak.test.service.command.id.set.FacultyCommands;
import oleg.sopilnyak.test.service.command.id.set.StudentsGroupCommands;
import oleg.sopilnyak.test.service.facade.impl.OrganizationFacadeImpl;
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
class OrganizationFacadeImplTest<T> {
    OrganizationPersistenceFacade persistenceFacade = mock(OrganizationPersistenceFacade.class);
    @Spy
    CommandsFactory<T> factory = buildFactory();
    @Mock
    AuthorityPerson mockPerson;
    @Mock
    Faculty mockFaculty;
    @Mock
    StudentsGroup mockGroup;

    @Spy
    @InjectMocks
    OrganizationFacadeImpl facade;

    @Test
    void shouldFindAllAuthorityPersons() {
        String commandId = AuthorityPersonCommands.FIND_ALL;

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldFindAllAuthorityPersons2() {
        String commandId = AuthorityPersonCommands.FIND_ALL;
        when(persistenceFacade.findAllAuthorityPersons()).thenReturn(Set.of(mockPerson));

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldNotGetAuthorityPersonById() {
        String commandId = AuthorityPersonCommands.FIND_BY_ID;
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.getAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
    }

    @Test
    void shouldGetAuthorityPersonById() {
        String commandId = AuthorityPersonCommands.FIND_BY_ID;
        Long id = 301L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.getAuthorityPersonById(id);

        assertThat(person).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson() {
        String commandId = AuthorityPersonCommands.CREATE_OR_UPDATE;

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockPerson);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson2() {
        String commandId = AuthorityPersonCommands.CREATE_OR_UPDATE;
        when(persistenceFacade.save(mockPerson)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockPerson);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        String commandId = AuthorityPersonCommands.DELETE;
        Long id = 302L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        String commandId = AuthorityPersonCommands.DELETE;
        Long id = 303L;

        AuthorityPersonIsNotExistsException thrown =
                assertThrows(AuthorityPersonIsNotExistsException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        String commandId = AuthorityPersonCommands.DELETE;
        Long id = 304L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(mockPerson.getFaculties()).thenReturn(List.of(mockFaculty));

        AuthorityPersonManageFacultyException thrown =
                assertThrows(AuthorityPersonManageFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:304 is managing faculties.", thrown.getMessage());

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldFindAllFaculties() {
        String commandId = FacultyCommands.FIND_ALL;

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties2() {
        String commandId = FacultyCommands.FIND_ALL;
        when(persistenceFacade.findAllFaculties()).thenReturn(Set.of(mockFaculty));

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldNotGetFacultyById() {
        String commandId = FacultyCommands.FIND_BY_ID;
        Long id = 400L;

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldGetFacultyById() {
        String commandId = FacultyCommands.FIND_BY_ID;
        Long id = 410L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty() {
        String commandId = FacultyCommands.CREATE_OR_UPDATE;
        when(persistenceFacade.save(mockFaculty)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockFaculty);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {
        String commandId = FacultyCommands.CREATE_OR_UPDATE;

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockFaculty);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldDeleteFacultyById() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        String commandId = FacultyCommands.DELETE;
        Long id = 402L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        facade.deleteFacultyById(id);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotExists() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        String commandId = FacultyCommands.DELETE;
        Long id = 403L;

        FacultyNotExistsException thrown = assertThrows(FacultyNotExistsException.class, () -> facade.deleteFacultyById(id));

        assertEquals("Faculty with ID:403 is not exists.", thrown.getMessage());
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotEmpty() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        String commandId = FacultyCommands.DELETE;
        Long id = 404L;
        when(mockFaculty.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));

        assertEquals("Faculty with ID:404 has courses.", thrown.getMessage());
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldFindAllStudentsGroup() {
        String commandId = StudentsGroupCommands.FIND_ALL;
        when(persistenceFacade.findAllStudentsGroups()).thenReturn(Set.of(mockGroup));

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldNotFindAllStudentsGroup() {
        String commandId = StudentsGroupCommands.FIND_ALL;

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldGetStudentsGroupById() {
        String commandId = StudentsGroupCommands.FIND_BY_ID;
        Long id = 500L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        Optional<StudentsGroup> faculty = facade.getStudentsGroupById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldNotGetStudentsGroupById() {
        String commandId = StudentsGroupCommands.FIND_BY_ID;
        Long id = 510L;

        Optional<StudentsGroup> faculty = facade.getStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup() {
        String commandId = StudentsGroupCommands.CREATE_OR_UPDATE;
        when(persistenceFacade.save(mockGroup)).thenReturn(Optional.of(mockGroup));

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(mockGroup);

        assertThat(faculty).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockGroup);
        verify(persistenceFacade).save(mockGroup);
    }

    @Test
    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        String commandId = StudentsGroupCommands.DELETE;
        Long id = 502L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        facade.deleteStudentsGroupById(id);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        String commandId = StudentsGroupCommands.DELETE;
        Long id = 503L;
        StudentsGroupNotExistsException thrown =
                assertThrows(StudentsGroupNotExistsException.class, () -> facade.deleteStudentsGroupById(id));

        assertEquals("StudentsGroup with ID:503 is not exists.", thrown.getMessage());
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        String commandId = StudentsGroupCommands.DELETE;
        Long id = 504L;
        when(mockGroup.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));
        StudentGroupWithStudentsException thrown =
                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertEquals("StudentsGroup with ID:504 has students.", thrown.getMessage());
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    private CommandsFactory<T> buildFactory() {
        return new OrganizationCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade)),
                        spy(new CreateOrUpdateFacultyCommand(persistenceFacade)),
                        spy(new CreateOrUpdateStudentsGroupCommand(persistenceFacade)),
                        spy(new DeleteAuthorityPersonCommand(persistenceFacade)),
                        spy(new DeleteFacultyCommand(persistenceFacade)),
                        spy(new DeleteStudentsGroupCommand(persistenceFacade)),
                        spy(new FindAllAuthorityPersonsCommand(persistenceFacade)),
                        spy(new FindAllFacultiesCommand(persistenceFacade)),
                        spy(new FindAllStudentsGroupsCommand(persistenceFacade)),
                        spy(new FindAuthorityPersonCommand(persistenceFacade)),
                        spy(new FindFacultyCommand(persistenceFacade)),
                        spy(new FindStudentsGroupCommand(persistenceFacade))
                )
        );
    }
}