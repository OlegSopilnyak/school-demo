package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.StudentsGroupNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentsGroupCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    DeleteStudentsGroupCommand command;
    @Mock
    StudentsGroup instance;

    @Test
    void shouldExecuteCommand() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 510L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(false)).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() throws StudentGroupWithStudentsException, StudentsGroupNotExistsException {
        Long id = 511L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistenceFacade).deleteStudentsGroup(id);
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldNotExecuteCommand_NoGroup() {
        Long id = 513L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentsGroupNotExistsException.class);
    }

    @Test
    void shouldNotExecuteCommand_NotEmptyGroup() {
        Long id = 514L;
        when(instance.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentGroupWithStudentsException.class);
    }
}