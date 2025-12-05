package oleg.sopilnyak.test.service.facade.impl.message;

import oleg.sopilnyak.test.service.message.BaseCommandMessage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Watcher: message in progress watcher
 */
@Slf4j
public class MessageProgressWatchdog<T> {
    @Getter
    private BaseCommandMessage<T> result = null;
    @Getter
    private volatile State state = State.IN_PROGRESS;
    private final Object getResultMonitor = new Object();

    public void setResult(BaseCommandMessage<T> result) {
        if (result != null) {
            this.result = result;
            this.state = State.COMPLETED;
        }
    }

    public void waitForMessageComplete() {
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
                getResultMonitor.notifyAll();
            }
        }
    }
    // State of message processing
    public enum State {IN_PROGRESS, COMPLETED}
}
