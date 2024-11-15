package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;
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
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    @Spy
    CommandsFactory<StudentsGroupCommand> factory = buildFactory();
    @Spy
    @InjectMocks
    StudentsGroupFacadeImpl facade;

    @Mock
    StudentsGroup mockGroup;
    @Mock
    StudentsGroupPayload mockGroupPayload;

    @Test
    void shouldFindAllStudentsGroup() {
        when(persistenceFacade.findAllStudentsGroups()).thenReturn(Set.of(mockGroup));

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).hasSize(1);
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldNotFindAllStudentsGroup() {

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldFindStudentsGroupById() {
        Long id = 500L;
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldNotFindStudentsGroupById() {
        Long id = 510L;

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup() {
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(payloadMapper.toPayload(mockGroupPayload)).thenReturn(mockGroupPayload);
        when(persistenceFacade.save(mockGroupPayload)).thenReturn(Optional.of(mockGroupPayload));

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(mockGroup);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).createContext(mockGroupPayload);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockGroupPayload);
    }

    @Test
    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        Long id = 502L;
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        facade.deleteStudentsGroupById(id);

        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        Long id = 503L;
        StudentsGroupNotFoundException thrown =
                assertThrows(StudentsGroupNotFoundException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("Students Group with ID:503 is not exists.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        Long id = 504L;
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(mockGroupPayload.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        StudentGroupWithStudentsException thrown =
                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("Students Group with ID:504 has students.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    private CommandsFactory<StudentsGroupCommand> buildFactory() {
        return new StudentsGroupCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateStudentsGroupCommand(persistenceFacade, payloadMapper)),
                        spy(new DeleteStudentsGroupCommand(persistenceFacade, payloadMapper)),
                        spy(new FindAllStudentsGroupsCommand(persistenceFacade)),
                        spy(new FindStudentsGroupCommand(persistenceFacade))
                )

        );
    }
}