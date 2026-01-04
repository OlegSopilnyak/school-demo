package oleg.sopilnyak.test.service.command.type.core.nested;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;

/**
 * Deque: used for collect contexts during macro-command do nested flow
 * Synchronized Contexts Deque
 *
 * @param <T> type of deque item
 */
public class NestedContextDeque<T> {
    @Getter
    private final Deque<T> deque = new LinkedList<>();
    private final Lock locker = new ReentrantLock();

    /**
     * To put synchronously item to the queue<BR/>
     * in the thread-safe manner
     *
     * @param item item to put
     * @see Deque#addLast(Object)
     * @see Lock#lock()
     * @see Lock#unlock()
     */
    public void putToTail(final T item) {
        locker.lock();
        try {
            deque.addLast(item);
        } finally {
            locker.unlock();
        }
    }
}
