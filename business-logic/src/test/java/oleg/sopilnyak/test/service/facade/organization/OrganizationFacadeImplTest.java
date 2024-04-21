package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
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
class OrganizationFacadeImplTest {
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE = "organization.authority.person.delete";
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "organization.faculty.findAll";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "organization.faculty.findById";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    private static final String ORGANIZATION_FACULTY_DELETE = "organization.faculty.delete";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "organization.students.group.findAll";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "organization.students.group.findById";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "organization.students.group.delete";
    OrganizationPersistenceFacade persistenceFacade = mock(OrganizationPersistenceFacade.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();
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
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldFindAllAuthorityPersons2() {
        when(persistenceFacade.findAllAuthorityPersons()).thenReturn(Set.of(mockPerson));

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).hasSize(1);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void shouldNotGetAuthorityPersonById() {
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
    void shouldCreateOrUpdateAuthorityPerson() {

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).execute(mockPerson);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson2() {
        when(persistenceFacade.save(mockPerson)).thenReturn(Optional.of(mockPerson));

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(mockPerson);

        assertThat(person).isPresent();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).execute(mockPerson);
        verify(persistenceFacade).save(mockPerson);
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 302L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(mockPerson));

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 303L;

        AuthorityPersonIsNotExistsException thrown =
                assertThrows(AuthorityPersonIsNotExistsException.class, () -> facade.deleteAuthorityPersonById(id));

        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
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

    @Test
    void shouldFindAllFaculties() {

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties2() {
        when(persistenceFacade.findAllFaculties()).thenReturn(Set.of(mockFaculty));

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).hasSize(1);
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldNotGetFacultyById() {
        Long id = 400L;

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldGetFacultyById() {
        Long id = 410L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty() {
        when(persistenceFacade.save(mockFaculty)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).execute(mockFaculty);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).execute(mockFaculty);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldDeleteFacultyById() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 402L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        facade.deleteFacultyById(id);

        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotExists() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 403L;

        FacultyNotExistsException thrown = assertThrows(FacultyNotExistsException.class, () -> facade.deleteFacultyById(id));

        assertEquals("Faculty with ID:403 is not exists.", thrown.getMessage());
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotEmpty() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 404L;
        when(mockFaculty.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));

        assertEquals("Faculty with ID:404 has courses.", thrown.getMessage());
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldFindAllStudentsGroup() {
        when(persistenceFacade.findAllStudentsGroups()).thenReturn(Set.of(mockGroup));

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).hasSize(1);
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldNotFindAllStudentsGroup() {

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldGetStudentsGroupById() {
        Long id = 500L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        Optional<StudentsGroup> faculty = facade.getStudentsGroupById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldNotGetStudentsGroupById() {
        Long id = 510L;

        Optional<StudentsGroup> faculty = facade.getStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup() {
        when(persistenceFacade.save(mockGroup)).thenReturn(Optional.of(mockGroup));

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(mockGroup);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).execute(mockGroup);
        verify(persistenceFacade).save(mockGroup);
    }

    @Test
    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 502L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        facade.deleteStudentsGroupById(id);

        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 503L;
        StudentsGroupNotExistsException thrown =
                assertThrows(StudentsGroupNotExistsException.class, () -> facade.deleteStudentsGroupById(id));

        assertEquals("StudentsGroup with ID:503 is not exists.", thrown.getMessage());
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 504L;
        when(mockGroup.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));
        StudentGroupWithStudentsException thrown =
                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertEquals("StudentsGroup with ID:504 has students.", thrown.getMessage());
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    private CommandsFactory<?> buildFactory() {
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