package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import org.slf4j.Logger;

/**
 * Type for school-profile command
 */
public interface ProfileCommand<T> extends SchoolCommand<T> {
    String FIND_BY_ID_COMMAND_ID="profile.person.findById";
    String DELETE_BY_ID_COMMAND_ID="profile.person.deleteById";
    String CREATE_OR_UPDATE_COMMAND_ID="profile.person.createOrUpdate";
    /**
     * To get reference to profile's persistence facade
     *
     * @return reference to the facade
     */
    ProfilePersistenceFacade getPersistenceFacade();

    Logger getLog();

    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "profileCommandsFactory";

    /**
     * To cache into context old value of the profile for possible roll;back
     *
     * @param context command execution context
     * @param inputId system-id of the profile
     * @throws ProfileNotExistsException if profile does not exist
     * @see ProfilePersistenceFacade
     * @see ProfilePersistenceFacade#findProfileById(Long)
     * @see Context
     * @see Context#setUndoParameter(Object)
     */
    default void cacheProfileForRollback(Context<T> context, Long inputId) throws ProfileNotExistsException {
        final PersonProfile existsProfile = getPersistenceFacade().findProfileById(inputId)
                .orElseThrow(() -> new ProfileNotExistsException("PersonProfile with ID:" + inputId + " is not exists."));
        // saving the copy of exists entity for undo operation
        context.setUndoParameter(getPersistenceFacade().toEntity(existsProfile));
    }

    /**
     * To restore person profile from cache(context)
     *
     * @param context command execution context
     */
    default void rollbackCachedProfile(Context<T> context) {
        final Object oldProfile = context.getUndoParameter();
        if (oldProfile instanceof PersonProfile profile) {
            getLog().debug("Restoring changed value of profile {}", profile);
            getPersistenceFacade().saveProfile(profile);
        }
    }

    /**
     * To execute command
     *
     * @param context context of redo execution
     * @see Context
     */
    @Override
    default void redo(Context<T> context) {
        if (isWrongRedoStateOf(context)) {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            doRedo(context);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    default void doRedo(Context<T> context){
        context.setState(Context.State.DONE);
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default void undo(Context<T> context) {
        if (isWrongUndoStateOf(context)) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            doUndo(context);
        }
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    default void doUndo(Context<T> context){
        context.setState(Context.State.UNDONE);
    }
}
