package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;

class CommandContextTest {

    CommandContext<Boolean> context = CommandContext.<Boolean>builder()
            .states(new ArrayList<>())
            .build();

    @Test
    void shouldAddStatesInCorrectOrder() {
        List<Context.State> states = List.of(
                // context is built (there is no parameter for redo)
                INIT,
                // context is ready to command redo(...)
                READY,
                // command execution is in progress
                WORK,
                // command redo(...) is finished successfully
                DONE,
                // command execution is finished unsuccessfully
                FAIL,
                // command undo(...) is finished successfully
                UNDONE
        );

        states.forEach(state -> context.setState(state));

        assertThat(context.getStates()).isEqualTo(states);
    }
}