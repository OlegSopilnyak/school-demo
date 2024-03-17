package oleg.sopilnyak.test.service.command.id.set;


/**
 * Service-Command-Ids: The set of students-group command-ids
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public enum StudentsGroupCommands {
    FIND_ALL("organization.students.group.findAll"),
    FIND_BY_ID("organization.students.group.findById"),
    CREATE_OR_UPDATE("organization.students.group.createOrUpdate"),
    DELETE("organization.students.group.delete")
    ;
    private final String commandId;
    StudentsGroupCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
