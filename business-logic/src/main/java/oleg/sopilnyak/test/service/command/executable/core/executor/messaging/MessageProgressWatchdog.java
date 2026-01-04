package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.service.message.CommandMessage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Watcher: message in progress watcher
 */
@Slf4j
public class MessageProgressWatchdog<T> {
    // original instance of the message to watch after
    private final CommandMessage<T> original;
    @Getter
    private CommandMessage<T> result;
    @Getter
    private volatile State state = State.IN_PROGRESS;
    private final Object getResultMonitor = new Object();

    public MessageProgressWatchdog(CommandMessage<T> original) {
        this.original = original;
        this.result  = original;
    }

    public void setResult(CommandMessage<T> result) {
        if (result != null) {
            this.result = result;
            this.state = State.COMPLETED;
        }
    }

    public void waitForMessageComplete() {
        result = null;
        synchronized (getResultMonitor) {
            while (state == State.IN_PROGRESS) {
                try {
                    getResultMonitor.wait(100);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for state to complete.", e);
                    /* Clean up whatever needs to be handled before interrupting  */
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Finalize message's watching
     */
    public void messageProcessingIsDone() {
        synchronized (getResultMonitor) {
            if (state == State.COMPLETED) {
                getResultMonitor.notify();
            }
        }
    }
    // State of message doingMainLoop
    public enum State {IN_PROGRESS, COMPLETED}
}
