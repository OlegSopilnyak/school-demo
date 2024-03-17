package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of student-command-ids
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public enum StudentCommands {
    FIND_BY_ID("student.findById"),
    FIND_ENROLLED("student.findEnrolledTo"),
    FIND_NOT_ENROLLED("student.findNotEnrolled"),
    CREATE_OR_UPDATE("student.createOrUpdate"),
    DELETE("student.delete")
    ;
    private final String commandId;
    StudentCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
