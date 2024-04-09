package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;

class CommandContextTest {

    CommandContext<Boolean> context = CommandContext.<Boolean>builder().build();

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

    @Test
    void shouldAddStateChangedListenerAndReactToStateChanging() {
        AtomicBoolean changed1 = new AtomicBoolean(false);
        AtomicBoolean changed2 = new AtomicBoolean(false);
        Context.StateChangedListener listener1 = (context, previous, newOne) -> changed1.getAndSet(true);
        Context.StateChangedListener listener2 = (context, previous, newOne) -> changed2.getAndSet(true);

        context.addStateListener(listener1);
        context.addStateListener(listener2);

        context.setState(INIT);

        assertThat(changed1.get()).isTrue();
        assertThat(changed2.get()).isTrue();
    }
}