package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.service.message.CommandMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Watcher: command-message in progress watcher
 */
@Slf4j
public class MessageProgressWatchdog<T> {
    private final Duration duration;
    // original instance of the message to watch after
    private final CommandMessage<T> original;
    private final AtomicReference<CommandMessage<T>> result = new AtomicReference<>(null);
    @Getter
    private volatile State state = State.IN_PROGRESS;
    private final Object getResultMonitor = new Object();

    public MessageProgressWatchdog(CommandMessage<T> original) {
        this(original, Duration.ofMillis(400L));
    }

    public MessageProgressWatchdog(CommandMessage<T> original, Duration duration) {
        this.original = original;
        this.duration = duration;
    }

    /**
     * To get processed result
     *
     * @return processed result value
     */
    public CommandMessage<T> getResult() {
        return result.get();
    }

    /**
     * Set up result value for watchdog
     *
     * @param result result value
     * @see MessageProgressWatchdog.State#COMPLETED
     */
    public void setResult(CommandMessage<T> result) {
        if (result != null) {
            this.result.getAndSet(result);
            this.state = State.COMPLETED;
        }
    }

    /**
     * Wait for message processing to complete
     *
     * @see MessageProgressWatchdog.State#EXPIRED
     * @see MessageProgressWatchdog.State#IN_PROGRESS
     * @see LocalDateTime
     * @see Duration#between(Temporal, Temporal)
     * @see Duration#compareTo(Duration)
     * @see Object#wait(long)
     */
    public void waitForMessageComplete() {
        synchronized (getResultMonitor) {
            result.getAndSet(null);
            final LocalDateTime startsAt = LocalDateTime.now();
            // waiting while state is in progress
            while (state == State.IN_PROGRESS) {
                try {
                    getResultMonitor.wait(100);
                    // check result message expiration
                    if (Duration.between(startsAt, LocalDateTime.now()).compareTo(duration) > 0) {
                        // updating watchdog's state
                        state = State.EXPIRED;
                        result.getAndSet(original);
                        // updating result message context
                        final String errorMessage = "Expired message with id:" + getResult().getCorrelationId();
                        log.warn(errorMessage);
                        getResult().getContext().failed(new TimeoutException(errorMessage));
                        break;
                    }
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
     *
     * @see MessageProgressWatchdog.State#EXPIRED
     * @see Object#notifyAll()
     */
    public void messageProcessingIsDone() {
        synchronized (getResultMonitor) {
            if (state == State.COMPLETED) {
                getResultMonitor.notifyAll();
            }
        }
    }
    // State of message processing
    public enum State {IN_PROGRESS, COMPLETED, EXPIRED}
}
