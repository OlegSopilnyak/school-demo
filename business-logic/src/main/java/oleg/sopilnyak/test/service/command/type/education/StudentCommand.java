package oleg.sopilnyak.test.service.command.type.education;

import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.core.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

/**
 * Type for school-students command
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see Student
 */
public interface StudentCommand<T> extends RootCommand<T> {
    // template of error message
    String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    //
    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "studentCommandsFactory";

    // spring-bean component names of the commands family
    final class Component {
        private Component() {
        }

        public static final String FIND_BY_ID = "studentFind";
        public static final String FIND_ENROLLED = "studentFindEnrolled";
        public static final String FIND_NOT_ENROLLED = "studentFindNotEnrolled";
        public static final String CREATE_OR_UPDATE = "studentUpdate";
        public static final String CREATE_NEW = "studentMacroCreate";
        public static final String DELETE = "studentDelete";
        public static final String DELETE_ALL = "studentMacroDelete";
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
        return (Class<F>) StudentCommand.class;
    }

    /**
     * To adopt student entity to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @return instance from business-logic data model
     * @see Student#getCourses()
     * @see oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper#toPayload(Student)
     * @see RootCommand#getPayloadMapper()
     */
    default StudentPayload adoptEntity(final Student entity) {
        getLog().debug("In student entity with id={} registered {} course(s)", entity.getId(), entity.getCourses().size());
        return entity instanceof StudentPayload entityPayload ? entityPayload : getPayloadMapper().toPayload(entity);
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor        visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(StudentCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.core.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
