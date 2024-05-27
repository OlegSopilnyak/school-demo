package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

/**
 * Type for school-principal-profile commands
 */
public interface PrincipalProfileCommand extends ProfileCommand {
    /**
     * ID of findById principal profile command
     */
    String FIND_BY_ID = "profile.principal.findById";
    /**
     * ID of deleteById principal profile command
     */
    String DELETE_BY_ID = "profile.principal.deleteById";
    /**
     * ID of createOrUpdate principal profile command
     */
    String CREATE_OR_UPDATE = "profile.principal.createOrUpdate";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "principalProfileCommandsFactory";

// For commands playing Nested Command Role
//
//    /**
//     * To get access to command instance as nested one
//     *
//     * @return the reference to the command instance
//     * @see NestedCommand#asNestedCommand()
//     */
//    @Override
//    default SchoolCommand asNestedCommand(){
//        return this;
//    }
//
//    /**
//     * To prepare context for nested command using the visitor
//     *
//     * @param visitor visitor of prepared contexts
//     * @param input   Macro-Command call's input
//     * @param <T>     type of command result
//     * @return prepared for nested command context
//     * @see PrepareContextVisitor#prepareContext(PrincipalProfileCommand, Object)
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
//     * @see TransferResultVisitor#transferPreviousExecuteDoResult(PrincipalProfileCommand, Object, Context)
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
//     * @see PrincipalProfileCommand#doCommand(Context)
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
//     * @see PrincipalProfileCommand#undoCommand(Context)
//     */
//    @Override
//    default <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
//                                               final Context<T> context) {
//        return visitor.undoNestedCommand(this, context);
//    }
}
