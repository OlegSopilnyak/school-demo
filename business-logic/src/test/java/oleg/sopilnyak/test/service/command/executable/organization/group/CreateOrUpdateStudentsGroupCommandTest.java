package oleg.sopilnyak.test.service.command.executable.organization.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateStudentsGroupCommandTest {
    @Mock
    StudentsGroup entity;
    @Mock
    StudentsGroupPayload payload;
    @Mock
    StudentsGroupPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentsGroupCommand command;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        doReturn(command).when(applicationContext).getBean("studentsGroupUpdate", StudentsGroupCommand.class);
    }

    @Test
    void shouldBeValidCommand() {
        reset(applicationContext);
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CreateEntity() {
        Long id = -500L;
        when(entity.getId()).thenReturn(id);
        when(payload.getId()).thenReturn(id);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(id);
        Optional<StudentsGroup> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(payload);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        Long id = 500L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(payload);
        Optional<StudentsGroup> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(payload);
        verify(command).executeDo(context);
        verify(entity, times(3)).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, times(2)).toPayload(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_EntityNotFound() {
        Long id = 501L;
        when(entity.getId()).thenReturn(id);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentsGroupNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, never()).toPayload(any(StudentsGroup.class));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 502L;
        when(entity.getId()).thenReturn(id);
        doThrow(RuntimeException.class).when(persistence).findStudentsGroupById(id);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, never()).toPayload(any(StudentsGroup.class));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        Long id = 503L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        doThrow(RuntimeException.class).when(persistence).save(any(StudentsGroup.class));
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity, times(2)).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper).toPayload(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<StudentsGroup>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        reset(applicationContext);
        Context<Optional<StudentsGroup>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        Long id = 504L;
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        reset(applicationContext);
        Context<Optional<StudentsGroup>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage())
                .isEqualTo("Wrong input parameter value (cannot be null or empty).");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("param"));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).startsWith("Parameter not a 'Long' value:[param]");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteEntityExceptionThrown() throws ProfileNotFoundException {
        Long id = 505L;
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteStudentsGroup(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).save(entity);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }
}