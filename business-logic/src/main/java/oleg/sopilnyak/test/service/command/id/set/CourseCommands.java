package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of course-command-ids
 *
 * @see oleg.sopilnyak.test.school.common.model.Course
 */
public enum CourseCommands {
    FIND_BY_ID("course.findById"),
    FIND_REGISTERED("course.findRegisteredFor"),
    FIND_NOT_REGISTERED("course.findWithoutStudents"),
    CREATE_OR_UPDATE("course.createOrUpdate"),
    DELETE("course.delete"),
    REGISTER("course.register"),
    UN_REGISTER("course.unRegister")
    ;
    private final String commandId;
    CourseCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
