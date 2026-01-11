package oleg.sopilnyak.test.service.command.type.core.nested;

import oleg.sopilnyak.test.service.command.executable.core.MacroCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;

import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import lombok.AllArgsConstructor;

/**
 * StateChangedListener: Context state changed listener. According to state, put context to appropriate deque
 *
 * @see Context.State
 * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
 * @see NestedContextDeque#putToTail(Object)
 * @see MacroCommand#executeNested(Deque, Context.StateChangedListener)
 */
@AllArgsConstructor
public class NestedStateChangedListener implements Context.StateChangedListener {
    private final NestedContextDeque<Context<?>> succeedContexts;
    private final NestedContextDeque<Context<?>> failedContexts;
    private final CountDownLatch nestedLatch;
    private final Logger log;

    /**
     * State changed event processing method
     *
     * @param context  the context where state was changed
     * @param previous previous context state value
     * @param current  new context state value
     */
    @Override
    public void stateChanged(Context<?> context, Context.State previous, Context.State current) {
        final String commandId = context.getCommand().getId();
        switch (current) {
            case INIT, READY, WORK, UNDONE ->
                    log.debug("Changed context state of '{}' from:{} to :{}", commandId, previous, current);
            case CANCEL -> {
                log.debug("Command '{}' is Canceled from State:{}", commandId, previous);
                nestedLatch.countDown();
            }
            case DONE -> {
                log.debug("Command '{}' is Done from State:{}", commandId, previous);
                succeedContexts.putToTail(context);
                nestedLatch.countDown();
            }
            case FAIL -> {
                log.debug("Command '{}' is Failed from State:{}", commandId, previous);
                failedContexts.putToTail(context);
                nestedLatch.countDown();
            }
            default -> throw new IllegalStateException("Unexpected value: " + current);
        }
    }
}
