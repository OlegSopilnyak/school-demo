package oleg.sopilnyak.test.service.command.executable.core;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Type: Basic implementation of root-command for transactional do/undo operations
 *
 * @param <T> the type of command execution result
 */
public abstract class BasicCommand<T> implements RootCommand<T> {
    // beans factory to prepare the current command for transactional operations
    protected transient BeanFactory applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<RootCommand<T>> self = new AtomicReference<>(null);

    @Autowired
    public final void setApplicationContext(BeanFactory applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    public RootCommand<T> self() {
        synchronized (RootCommand.class) {
            if (isNull(self.get())) {
                // getting command instance reference, which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo/executeUndo methods
                final String springName = springName();
                final Class<? extends RootCommand<T>> familyType = commandFamily();
                getLog().debug("Getting command from family:'{}' bean-name:[{}]",familyType.getSimpleName(), springName);
                self.getAndSet(applicationContext.getBean(springName, familyType));
            }
        }
        return self.get();
    }
}
