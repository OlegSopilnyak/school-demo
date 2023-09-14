package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.organization.*;
import oleg.sopilnyak.test.service.facade.organization.entity.AuthorityPersonCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.FacultyCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;
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
class OrganizationFacadeImplTest {
    OrganizationPersistenceFacade persistenceFacade = mock(OrganizationPersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();
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
        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(AuthorityPersonCommandFacade.FIND_ALL);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldGetAuthorityPersonById() {
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.getAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(AuthorityPersonCommandFacade.FIND_BY_ID);
        verify(persistenceFacade).findAuthorityPersonById(id);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson() {
        Long id = 301L;

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isEmpty();
        verify(factory).command(AuthorityPersonCommandFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 302L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(AuthorityPersonCommandFacade.DELETE);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 303L;

        AuthorityPersonIsNotExistsException thrown = assertThrows(AuthorityPersonIsNotExistsException.class, () -> {
            facade.deleteAuthorityPersonById(id);
        });

        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());

        verify(factory).command(AuthorityPersonCommandFacade.DELETE);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 304L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));
        when(mockPerson.getFaculties()).thenReturn(List.of(mockFaculty));

        AuthorityPersonManageFacultyException thrown = assertThrows(AuthorityPersonManageFacultyException.class, () -> {
            facade.deleteAuthorityPersonById(id);
        });

        assertEquals("AuthorityPerson with ID:304 is managing faculties.", thrown.getMessage());

        verify(factory).command(AuthorityPersonCommandFacade.DELETE);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldFindAllFaculties() {
        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(FacultyCommandFacade.FIND_ALL);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldGetFacultyById() {
        Long id = 400L;

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(FacultyCommandFacade.FIND_BY_ID);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty() {
        Long id = 401L;

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isEmpty();
        verify(factory).command(FacultyCommandFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldDeleteFacultyById() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 402L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        facade.deleteFacultyById(id);

        verify(factory).command(FacultyCommandFacade.DELETE);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotExists() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 403L;

        FacultyNotExistsException thrown = assertThrows(FacultyNotExistsException.class, () -> {
            facade.deleteFacultyById(id);
        });

        assertEquals("Faculty with ID:403 is not exists.", thrown.getMessage());
        verify(factory).command(FacultyCommandFacade.DELETE);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotEmpty() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 404L;
        when(mockFaculty.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> {
            facade.deleteFacultyById(id);
        });

        assertEquals("Faculty with ID:404 has courses.", thrown.getMessage());
        verify(factory).command(FacultyCommandFacade.DELETE);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldFindAllStudentsGroup() {
        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(StudentsGroupCommandFacade.FIND_ALL);
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldGetStudentsGroupById() {
        Long id = 500L;

        Optional<StudentsGroup> faculty = facade.getStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(StudentsGroupCommandFacade.FIND_BY_ID);
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup() {
        Long id = 501L;

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(mockGroup);

        assertThat(faculty).isEmpty();
        verify(factory).command(StudentsGroupCommandFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(mockGroup);
    }

    @Test
    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 502L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        facade.deleteStudentsGroupById(id);

        verify(factory).command(StudentsGroupCommandFacade.DELETE);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 503L;
        StudentsGroupNotExistsException thrown = assertThrows(StudentsGroupNotExistsException.class, () -> {
            facade.deleteStudentsGroupById(id);
        });

        assertEquals("StudentsGroup with ID:503 is not exists.", thrown.getMessage());
        verify(factory).command(StudentsGroupCommandFacade.DELETE);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 504L;
        when(mockGroup.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));
        StudentGroupWithStudentsException thrown = assertThrows(StudentGroupWithStudentsException.class, () -> {
            facade.deleteStudentsGroupById(id);
        });

        assertEquals("StudentsGroup with ID:504 has students.", thrown.getMessage());
        verify(factory).command(StudentsGroupCommandFacade.DELETE);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory(
                Set.of(
                        new CreateOrUpdateAuthorityPersonCommand(persistenceFacade),
                        new CreateOrUpdateFacultyCommand(persistenceFacade),
                        new CreateOrUpdateStudentsGroupCommand(persistenceFacade),
                        new DeleteAuthorityPersonCommand(persistenceFacade),
                        new DeleteFacultyCommand(persistenceFacade),
                        new DeleteStudentsGroupCommand(persistenceFacade),
                        new FindAllAuthorityPersonsCommand(persistenceFacade),
                        new FindAllFacultiesCommand(persistenceFacade),
                        new FindAllStudentsGroupsCommand(persistenceFacade),
                        new FindAuthorityPersonCommand(persistenceFacade),
                        new FindFacultyCommand(persistenceFacade),
                        new FindStudentsGroupCommand(persistenceFacade)
                )
        );
    }
}