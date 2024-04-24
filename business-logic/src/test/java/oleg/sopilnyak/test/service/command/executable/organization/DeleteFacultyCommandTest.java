package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
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
class DeleteFacultyCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    DeleteFacultyCommand command;
    @Mock
    Faculty instance;

    @Test
    void shouldExecuteCommand() throws NotExistFacultyException, FacultyIsNotEmptyException {
        Long id = 410L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade).deleteFaculty(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(false)).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() throws NotExistFacultyException, FacultyIsNotEmptyException {
        Long id = 411L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistenceFacade).deleteFaculty(id);
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade).deleteFaculty(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldNotExecuteCommand_NoFaculty() {
        Long id = 412L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(NotExistFacultyException.class);
    }

    @Test
    void shouldNotExecuteCommand_NotEmptyFaculty() {
        Long id = 413L;
        when(instance.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(FacultyIsNotEmptyException.class);
    }
}