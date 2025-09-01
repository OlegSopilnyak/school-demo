package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentCommandTest {
    @Mock
    Student entity;
    @Mock
    StudentPayload payload;
    @Mock
    StudentsPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    DeleteStudentCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_StudentFound() {
        Long id = 110L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(entity));
        when(persistence.deleteStudent(id)).thenReturn(true);
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(payloadMapper).toPayload(entity);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 111L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).deleteStudent(id);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(payloadMapper).toPayload(entity);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_StudentNotFound() {
        Long id = 112L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(payloadMapper, never()).toPayload(any(Student.class));
        verify(persistence, never()).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_StudentHasCourses() {
        Long id = 113L;
        when(payload.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistence.findStudentById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentWithCoursesException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(payloadMapper).toPayload(entity);
        verify(persistence, never()).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        when(persistence.save(entity)).thenReturn(Optional.of(entity));

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("instance"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).startsWith("Parameter not a 'Student' value:[instance]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(entity);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(entity);
    }
}