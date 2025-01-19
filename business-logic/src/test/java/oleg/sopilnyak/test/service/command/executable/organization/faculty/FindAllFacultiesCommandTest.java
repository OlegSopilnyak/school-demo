package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
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

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllFacultiesCommandTest {
    @Mock
    FacultyPersistenceFacade persistence;
    @Spy
    @InjectMocks
    FindAllFacultiesCommand command;
    @Mock
    Faculty entity;

    @Test
    void shouldDoCommand_EntityExists() {
        when(persistence.findAllFaculties()).thenReturn(Set.of(entity));
        Context<Set<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of(entity));
        assertThat(context.getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        Context<Set<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of());
        assertThat(context.getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        doThrow(RuntimeException.class).when(persistence).findAllFaculties();
        Context<Set<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        Context<Set<Faculty>> context = command.createContext(null);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}