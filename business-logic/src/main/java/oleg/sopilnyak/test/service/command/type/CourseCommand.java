package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import org.slf4j.Logger;

/**
 * Type for school-course command
 */
public interface CourseCommand extends SchoolCommand {
    String COURSE_WITH_ID_PREFIX = "Course with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "courseCommandsFactory";

    String FIND_BY_ID_COMMAND_ID = "course.findById";
    String FIND_REGISTERED_COMMAND_ID = "course.findRegisteredFor";
    String FIND_NOT_REGISTERED_COMMAND_ID = "course.findWithoutStudents";
    String CREATE_OR_UPDATE_COMMAND_ID = "course.createOrUpdate";
    String DELETE_COMMAND_ID = "course.delete";
    String REGISTER_COMMAND_ID = "course.register";
    String UN_REGISTER_COMMAND_ID = "course.unRegister";


    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To execute command
     *
     * @param context context of redo execution
     * @see Context
     */
    @Override
    default <T> void doCommand(Context<T> context) {
        if (isWrongRedoStateOf(context)) {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        }
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default <T> void undoCommand(Context<T> context) {
        if (isWrongUndoStateOf(context)) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            executeUndo(context);
        }
    }

    /**
     * To prepare command context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     */
    @Override
    default <T> Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Object input) {
        return visitor.prepareContext(this, input);
    }
}
