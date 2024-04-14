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
    /**
     * ID of findById profile command
     */
    String FIND_BY_ID_COMMAND_ID = "profile.person.findById";
    /**
     * ID of deleteById profile command
     */
    String DELETE_BY_ID_COMMAND_ID = "profile.person.deleteById";
    /**
     * ID of createOrUpdate profile command
     */
    String CREATE_OR_UPDATE_COMMAND_ID = "profile.person.createOrUpdate";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "profileCommandsFactory";

    /**
     * To get reference to profile's persistence facade
     *
     * @return reference to the facade
     */
    ProfilePersistenceFacade getPersistenceFacade();

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();


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
    default void cacheProfileForRollback(Context<?> context, Long inputId) throws ProfileNotExistsException {
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
    default void rollbackCachedProfile(Context<?> context) {
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
    default void redo(Context<?> context) {
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
    default void doRedo(Context<?> context) {
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
    default void undo(Context<?> context) {
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
    default void doUndo(Context<?> context) {
        context.setState(Context.State.UNDONE);
    }
}
