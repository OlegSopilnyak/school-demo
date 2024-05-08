package oleg.sopilnyak.test.service.command.executable.organization.group;

import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindStudentsGroupCommandTest {
    @Mock
    StudentsGroupPersistenceFacade persistence;
    @Spy
    @InjectMocks
    FindStudentsGroupCommand command;
    @Mock
    StudentsGroup entity;

    @Test
    void shouldDoCommand_EntityExists() {
        long id = 520L;
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        Context<Optional<StudentsGroup>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(entity));
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        long id = 521L;
        Context<Optional<StudentsGroup>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        long id = 522L;
        Context<Optional<StudentsGroup>> context = command.createContext(id);
        doThrow(RuntimeException.class).when(persistence).findStudentsGroupById(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        long id = 523L;
        Context<Optional<StudentsGroup>> context = command.createContext(id);
        context.setState(DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}