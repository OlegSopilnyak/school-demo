package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.base.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.facade.impl.StudentsGroupFacadeImpl;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentsGroupFacadeImplTest {
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "organization.students.group.findAll";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "organization.students.group.findById";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "organization.students.group.delete";
    StudentsGroupPersistenceFacade persistenceFacade = mock(StudentsGroupPersistenceFacade.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();
    @Mock
    StudentsGroup mockGroup;

    @Spy
    @InjectMocks
    StudentsGroupFacadeImpl facade;

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
    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
        Long id = 502L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        facade.deleteStudentsGroupById(id);

        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
        Long id = 503L;
        NotExistStudentsGroupException thrown =
                assertThrows(NotExistStudentsGroupException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("StudentsGroup with ID:503 is not exists." );
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
        Long id = 504L;
        when(mockGroup.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));
        StudentGroupWithStudentsException thrown =
                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("StudentsGroup with ID:504 has students.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    private CommandsFactory<?> buildFactory() {
        return mock(OrganizationCommandsFactory.class);
//        return new OrganizationCommandsFactory(
//                Set.of(
//                        spy(new CreateOrUpdateStudentsGroupCommand(persistenceFacade)),
//                        spy(new DeleteStudentsGroupCommand(persistenceFacade)),
//                        spy(new FindAllStudentsGroupsCommand(persistenceFacade)),
//                        spy(new FindStudentsGroupCommand(persistenceFacade))
//                )
//        );
    }
}