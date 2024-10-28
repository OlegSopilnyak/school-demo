package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutAuthorityPersonCommandTest {
    @Spy
    @InjectMocks
    LogoutAuthorityPersonCommand command;

    @Test
    void shouldDoCommand() {
        String token = "logged_in_person_token";
        Context<Boolean> context = command.createContext(token);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isTrue();
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        String token = "logged_in_person_token";
        Context<Boolean> context = command.createContext(token);
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}