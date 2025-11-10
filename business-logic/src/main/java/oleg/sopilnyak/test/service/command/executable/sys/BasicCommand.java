package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Type: Basic implementation of root-command for transactional do/undo operations
 *
 * @param <T> the type of command execution result
 */
@Component
public abstract class BasicCommand<T> implements RootCommand<T> {
    // beans factory to prepare the current command for transactional operations
    protected transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<RootCommand<T>> self = new AtomicReference<>(null);

    @Autowired
    public final void setApplicationContext(ApplicationContext applicationContext) {
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
                self.getAndSet(applicationContext.getBean(this.springName(), this.commandFamily()));
            }
        }
        return self.get();
    }

    @Override
    public String springName() {
        return "courseDelete";
    }

    /**
     * The class of commands family, the command is belonged to
     *
     * @return command family class value
     */
    @Override
    public Class<CourseCommand> commandFamily() {
        return CourseCommand.class;
    }
}
