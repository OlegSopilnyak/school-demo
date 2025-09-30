package oleg.sopilnyak.test.service.command.type.organization;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.legacy.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

/**
 * Type for school-organization students groups management command
 *
 * @param <T> the type of command execution (do) result
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public interface StudentsGroupCommand<T> extends OrganizationCommand<T> {
    String GROUP_WITH_ID_PREFIX = "Students Group with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "groupCommandsFactory";
    /**
     * Command-ID: for find all students groups
     */
    String FIND_ALL = "organization.students.group.findAll";
    /**
     * Command-ID: for find by ID students group
     */
    String FIND_BY_ID = "organization.students.group.findById";
    /**
     * Command-ID: for create or update students group entity
     */
    String CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    /**
     * Command-ID: for delete students group entity
     */
    String DELETE = "organization.students.group.delete";

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#afterExecuteDo(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    default T detachedResult(T result) {
        if (isNull(result)) {
            getLog().debug("Result is null");
            return null;
        } else if (result instanceof StudentsGroup entity) {
            return (T) detach(entity);
        } else if (result instanceof Optional<?> optionalEntity) {
            return (T) detach((Optional<StudentsGroup>) optionalEntity);
        } else if (result instanceof StudentsGroupSet entitiesSet) {
            return (T) detach(entitiesSet);
        } else {
            getLog().debug("Won't detach result. Leave it as is:'{}'", result);
            return result;
        }
    }

    /**
     * Set of StudentsGroup entities
     */
    interface StudentsGroupSet extends Set<StudentsGroup> {
    }

    /**
     * To detach StudentsGroup entity from persistence layer
     *
     * @param entity entity to detach
     * @return detached entity
     * @see #detachedResult(Object)
     */
    private StudentsGroup detach(StudentsGroup entity) {
        getLog().info("Entity to detach:'{}'", entity);
        return entity instanceof StudentsGroupPayload payload ? payload : getPayloadMapper().toPayload(entity);
    }

    /**
     * To detach StudentsGroup optional entity from persistence layer
     *
     * @param optionalEntity optional entity to detach
     * @return detached optional entity
     * @see #detachedResult(Object)
     */
    private Optional<StudentsGroup> detach(Optional<StudentsGroup> optionalEntity) {
        if (isNull(optionalEntity) || optionalEntity.isEmpty()) {
            getLog().info("Result is null or empty");
            return Optional.empty();
        } else {
            getLog().info("Optional entity to detach:'{}'", optionalEntity);
            return optionalEntity.map(this::detach);
        }
    }

    /**
     * To detach StudentsGroup entities set from persistence layer
     *
     * @param entitiesSet entities set to detach
     * @return detached entities set
     * @see #detachedResult(Object)
     */
    private Set<StudentsGroup> detach(Set<StudentsGroup> entitiesSet) {
        getLog().info("Entities set to detach:'{}'", entitiesSet);
        return entitiesSet.stream().map(this::detach).collect(Collectors.toSet());
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(StudentsGroupCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see StudentsGroupCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
//    @Override
//    default void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
//                                   final Context<?> context, final Context.StateChangedListener stateListener) {
//        visitor.doNestedCommand(this, (Context<T>) context, stateListener);
//    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see StudentsGroupCommand#undoCommand(Context)
     */
//    @Override
//    default Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
//        return visitor.undoNestedCommand(this, context);
//    }
}
