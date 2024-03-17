package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of faculty command-ids
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public enum FacultyCommands {
    FIND_ALL("organization.faculty.findAll"),
    FIND_BY_ID("organization.faculty.findById"),
    CREATE_OR_UPDATE("organization.faculty.createOrUpdate"),
    DELETE("organization.faculty.delete")
    ;
    private final String commandId;
    FacultyCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
