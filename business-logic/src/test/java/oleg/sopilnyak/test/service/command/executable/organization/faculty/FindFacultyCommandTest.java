package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindFacultyCommandTest {
    @Mock
    FacultyPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    FindFacultyCommand command;
    @Mock
    Faculty entity;
    @Mock
    FacultyPayload payload;

    @Test
    void shouldDoCommand_EntityExists() {
        long id = 420L;
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(entity));
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        long id = 421L;
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        long id = 422L;
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));
        doThrow(RuntimeException.class).when(persistence).findFacultyById(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        long id = 423L;
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));
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