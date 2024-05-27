package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.CanBeNested;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.NestedCommand;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import org.slf4j.Logger;

/**
 * Type for school-course command
 */
public interface CourseCommand extends SchoolCommand, CanBeNested {
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

// For commands playing Nested Command Role

    /**
     * To get access to command instance as nested one
     *
     * @return the reference to the command instance
     * @see NestedCommand#asNestedCommand()
     */
    @Override
    default CourseCommand asNestedCommand() {
        return this;
    }

//    /**
//     * To prepare context for nested command using the visitor
//     *
//     * @param visitor visitor of prepared contexts
//     * @param input   Macro-Command call's input
//     * @param <T>     type of command result
//     * @return prepared for nested command context
//     * @see PrepareContextVisitor#prepareContext(CourseCommand, Object)
//     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
//     */
//    @Override
//    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
//        return visitor.prepareContext(this, input);
//    }
//
//    /**
//     * To transfer command execution result to next command context
//     *
//     * @param visitor visitor for transfer result
//     * @param result  result of command execution
//     * @param target  command context for next execution
//     * @param <S>     type of current command execution result
//     * @param <T>     type of next command execution result
//     * @see TransferResultVisitor#transferPreviousExecuteDoResult(CourseCommand, Object, Context)
//     * @see Context#setRedoParameter(Object)
//     */
//    @Override
//    default <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
//                                         final S result, final Context<T> target) {
//        visitor.transferPreviousExecuteDoResult(this, result, target);
//    }
//
//    /**
//     * To execute command Do as a nested command
//     *
//     * @param visitor       visitor to do nested command execution
//     * @param context       context for nested command execution
//     * @param stateListener listener of context-state-change
//     * @param <T>           type of command execution result
//     * @see NestedCommandExecutionVisitor#doNestedCommand(SchoolCommand, Context, Context.StateChangedListener)
//     * @see Context#addStateListener(Context.StateChangedListener)
//     * @see CourseCommand#doCommand(Context)
//     * @see Context#removeStateListener(Context.StateChangedListener)
//     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
//     */
//    @Override
//    default <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
//                                       final Context<T> context, final Context.StateChangedListener<T> stateListener) {
//        visitor.doNestedCommand(this, context, stateListener);
//    }
//
//    /**
//     * To execute command Undo as a nested command
//     *
//     * @param visitor visitor to do nested command execution
//     * @param context context for nested command execution
//     * @param <T>     type of command execution result
//     * @see NestedCommandExecutionVisitor#undoNestedCommand(SchoolCommand, Context)
//     * @see CourseCommand#undoCommand(Context)
//     */
//    @Override
//    default <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
//                                               final Context<T> context) {
//        return visitor.undoNestedCommand(this, context);
//    }
}
