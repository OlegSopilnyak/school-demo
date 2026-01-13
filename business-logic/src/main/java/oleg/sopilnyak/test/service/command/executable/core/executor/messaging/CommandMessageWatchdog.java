package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.service.message.CommandMessage;

/**
 * Watcher: command-message in progress watcher
 *
 * @param <T> type of processed command's result
 * @see CommandMessage
 */
public interface CommandMessageWatchdog<T> {
    /**
     * Waiting for command-message processing to complete or expiration
     *
     * @see CommandMessageWatchdog.State#EXPIRED
     * @see CommandMessageWatchdog.State#IN_PROGRESS
     */
    void waitForMessageComplete();

    /**
     * Finalize (stop) command-message's watching
     *
     * @see CommandMessageWatchdog.State#EXPIRED
     * @see CommandMessageWatchdog.State#COMPLETED
     * @see CommandMessageWatchdog#waitForMessageComplete()
     */
    void messageProcessingIsDone();

    /**
     * To get processed result
     *
     * @return processed command result value
     */
    CommandMessage<T> getResult();

    /**
     * Set up processed result value for watchdog
     *
     * @param result result value
     * @see CommandMessageWatchdog.State#COMPLETED
     */
    void setResult(CommandMessage<T> result);

    /**
     * To get current state of the watcher
     *
     * @return current state value
     */
    State getState();

    // State of command-message processing
    enum State {IN_PROGRESS, COMPLETED, EXPIRED}
}
