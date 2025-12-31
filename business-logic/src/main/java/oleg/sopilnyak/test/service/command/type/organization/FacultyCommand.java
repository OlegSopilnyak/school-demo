package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

/**
 * Type for school-organization faculties management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public interface FacultyCommand<T> extends OrganizationCommand<T> {
    // template of error message
    String FACULTY_WITH_ID_PREFIX = "Faculty with ID:";

    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String FIND_ALL = "organization.faculty.findAll";
        public static final String FIND_BY_ID = "organization.faculty.findById";
        public static final String CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
        public static final String DELETE = "organization.faculty.delete";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "facultyCommandsFactory";

    // spring-bean names of the command family
    final class Component {
        private Component() {
        }

        public static final String FIND_ALL = "facultyFindAll";
        public static final String FIND_BY_ID = "facultyFind";
        public static final String CREATE_OR_UPDATE = "facultyUpdate";
        public static final String DELETE = "facultyDelete";
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
        return (Class<F>) FacultyCommand.class;
    }

    /**
     * To adopt faculty entity to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @return instance from business-logic data model
     * @see Faculty#getCourses()
     * @see oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper#toPayload(Faculty)
     * @see RootCommand#getPayloadMapper()
     */
    default FacultyPayload adoptEntity(final Faculty entity) {
        getLog().debug("In authority faculty with id={} manages {} courses", entity.getId(), entity.getCourses().size());
        return entity instanceof FacultyPayload entityPayload ? entityPayload : getPayloadMapper().toPayload(entity);
    }


// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(FacultyCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
