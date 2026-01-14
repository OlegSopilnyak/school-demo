package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@SuppressWarnings("unchcked")
public class TestCommandMessageWatchdog<T> implements CommandMessageWatchdog<T> {
    CommandMessage<?> original;
    final AtomicReference<CommandMessage<T>> result = new AtomicReference<>(null);
    final AtomicReference<CommandMessageWatchdog.State> state = new AtomicReference<>(CommandMessageWatchdog.State.IN_PROGRESS);
    final Object monitor = new Object();

    @Override
    public State getState() {
        return state.get();
    }

    public void setState(CommandMessageWatchdog.State state) {
        this.state.getAndSet(state);
    }

    @Override
    public CommandMessage<T> getResult() {
        return result.get();
    }

    @Override
    public void setResult(CommandMessage<T> result) {
        if (result != null) {
            this.result.getAndSet(result);
            this.state.getAndSet(CommandMessageWatchdog.State.COMPLETED);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void waitForMessageComplete() {
        int maximumTry = 3;
        int tryCount = 1;
        while (getState() == CommandMessageWatchdog.State.IN_PROGRESS) {
            synchronized (monitor) {
                try {
                    monitor.wait(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (tryCount++ >= maximumTry) {
                    state.getAndSet(CommandMessageWatchdog.State.EXPIRED);
                    result.getAndSet((CommandMessage<T>) original);
                    break;
                }
            }
        }
    }

    @Override
    public void messageProcessingIsDone() {
        synchronized (monitor) {
            if (getState() == CommandMessageWatchdog.State.COMPLETED) {
                monitor.notifyAll();
            }
        }
    }
}
