package oleg.sopilnyak.test.service.command.course;

import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.executable.course.UnRegisterStudentFromCourseCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnRegisterStudentFromCourseCommandTest {
    @Mock
    PersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Mock
    Student student;
    @Spy
    @InjectMocks
    UnRegisterStudentFromCourseCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 130L;
        Long[] ids = new Long[]{id, id};
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.unLink(student, course)).thenReturn(true);

        CommandResult<Boolean> result = command.execute(ids);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).unLink(student, course);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 131L;
        Long[] ids = new Long[]{id, id};
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistenceFacade).unLink(student, course);
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(ids);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).unLink(student, course);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldNotExecuteCommand_NoStudent() {
        Long id = 132L;
        Long[] ids = new Long[]{id, id};

        CommandResult<Boolean> result = command.execute(ids);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentNotExistsException.class);
    }

    @Test
    void shouldNotExecuteCommand_NoCourse() {
        Long id = 133L;
        Long[] ids = new Long[]{id, id};
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));

        CommandResult<Boolean> result = command.execute(ids);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(CourseNotExistsException.class);
    }

}