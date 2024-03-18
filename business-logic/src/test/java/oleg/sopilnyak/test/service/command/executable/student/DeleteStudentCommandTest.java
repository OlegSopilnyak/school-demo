package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
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
class DeleteStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    DeleteStudentCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 110L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistenceFacade.deleteStudent(id)).thenReturn(true);

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).deleteStudent(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(false)).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 111L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).deleteStudent(id);

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).deleteStudent(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldNotExecuteCommand_NoStudent() {
        Long id = 112L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentNotExistsException.class);
    }

    @Test
    void shouldNotExecuteCommand_HasCourses() {
        Long id = 113L;
        when(instance.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentWithCoursesException.class);
    }
}