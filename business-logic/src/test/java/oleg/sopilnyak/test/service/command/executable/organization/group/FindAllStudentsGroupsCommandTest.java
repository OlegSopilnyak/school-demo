package oleg.sopilnyak.test.service.command.executable.organization.group;

import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllStudentsGroupsCommandTest {
    @Mock
    StudentsGroupPersistenceFacade persistence;
    @Spy
    @InjectMocks
    FindAllStudentsGroupsCommand command;
    @Mock
    StudentsGroup entity;

    @Test
    void shouldDoCommand_EntityExists() {
        when(persistence.findAllStudentsGroups()).thenReturn(Set.of(entity));
        Context<Set<StudentsGroup>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of(entity));
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        Context<Set<StudentsGroup>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of());
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        doThrow(RuntimeException.class).when(persistence).findAllStudentsGroups();
        Context<Set<StudentsGroup>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        Context<Set<StudentsGroup>> context = command.createContext(null);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(entity));
        }
//        context.setState(DONE);
//        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}