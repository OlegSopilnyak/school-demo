package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateStudentCommandTest {
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
    CreateOrUpdateStudentCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CreateStudent() {
        Long id = -1L;
        when(entity.getId()).thenReturn(id);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<Student>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).isPresent().contains(entity);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldDoCommand_UpdateStudent() {
        Long id = 110L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findStudentById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<Student>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(payload);
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().orElseThrow()).isPresent().contains(entity);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(payloadMapper).toPayload(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext("instance");

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(entity);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(entity);
    }

    @Test
    void shouldNotDoCommand_CreateExceptionThrown() {
        Long id = -111L;
        when(entity.getId()).thenReturn(id);
        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        doThrow(cannotExecute).when(persistence).save(entity);
        Context<Optional<Student>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Long id = 111L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findStudentById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        when(persistence.save(entity)).thenThrow(cannotExecute).thenReturn(Optional.of(entity));
        Context<Optional<Student>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(payload);
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(payloadMapper).toPayload(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldUndoCommand_DeleteStudent() {
        Long id = 111L;
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("instance");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).startsWith("Parameter not a  'Long' value:[instance]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(entity);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(entity);
    }

    @Test
    void shouldNotUndoCommand_DeleteThrown() {
        Long id = 111L;
        doThrow(RuntimeException.class).when(persistence).deleteStudent(id);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotUndoCommand_RestoreThrown() {
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }
}