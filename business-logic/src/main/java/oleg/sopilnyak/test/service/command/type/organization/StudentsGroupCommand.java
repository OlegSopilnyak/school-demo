package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Type for school-organization students groups management command
 *
 * @param <T> the type of command execution (do) result
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public interface StudentsGroupCommand<T> extends OrganizationCommand<T> {
    // template of error message
    String GROUP_WITH_ID_PREFIX = "Students Group with ID:";

    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String FIND_ALL = "organization.students.group.findAll";
        public static final String FIND_BY_ID = "organization.students.group.findById";
        public static final String CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
        public static final String DELETE = "organization.students.group.delete";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "groupCommandsFactory";

    // spring-bean names of the command family
    final class Component {
        private Component() {
        }

        public static final String FIND_ALL = "studentsGroupFindAll";
        public static final String FIND_BY_ID = "studentsGroupFind";
        public static final String CREATE_OR_UPDATE = "studentsGroupUpdate";
        public static final String DELETE = "studentsGroupDelete";
    }

    /**
     * The class of commands family, the command is belonged to
     *
     * @return command family class value
     * @see BasicCommand#self()
     */
    @Override
    @SuppressWarnings("unchecked")
    default <F extends RootCommand> Class<F> commandFamily() {
        return (Class<F>) StudentsGroupCommand.class;
    }

    /**
     * To adopt faculty entity to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @return instance from business-logic data model
     * @see StudentsGroup#getStudents()
     * @see oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper#toPayload(StudentsGroup)
     * @see RootCommand#getPayloadMapper()
     */
    default StudentsGroupPayload adoptEntity(final StudentsGroup entity) {
        getLog().debug("In students group with id={} exist {} students", entity.getId(), entity.getStudents().size());
        return entity instanceof StudentsGroupPayload entityPayload ? entityPayload : getPayloadMapper().toPayload(entity);
    }

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
            // To detach Faculty optional result entity from persistence layer
            return  optionalEntity.isEmpty() ?
                    (T) Optional.empty()
                    :
                    (T) optionalEntity.map(StudentsGroup.class::cast).map(this::detach);
        } else if (result instanceof Set entitiesSet) {
            return (T) detach(entitiesSet);
        } else {
            getLog().debug("Won't detach result. Leave it as is:'{}'", result);
            return result;
        }
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
}
