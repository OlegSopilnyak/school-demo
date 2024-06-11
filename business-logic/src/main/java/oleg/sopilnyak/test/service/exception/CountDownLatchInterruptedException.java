package oleg.sopilnyak.test.service.exception;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Exception throws when command-executor cannot execute the command
 */
public class CountDownLatchInterruptedException extends RuntimeException {
    /**
     * Constructs a new instance of th exception with the specified detail command-id and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param latchSize the size of broken CountDownLatch
     * @param cause     the cause (which is saved for later retrieval by the
     *                  {@link #getCause()} method).  (A {@code null} value is
     *                  permitted, and indicates that the cause is nonexistent or
     *                  unknown.)
     * @see RootCommand#getId()
     */
    public CountDownLatchInterruptedException(long latchSize, Throwable cause) {
        super("CountDownLatch interrupted for " + latchSize + " countDowns", cause);
    }
}
