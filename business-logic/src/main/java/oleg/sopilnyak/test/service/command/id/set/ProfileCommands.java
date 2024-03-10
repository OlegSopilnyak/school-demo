package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The enumeration of person-profile command-ids
 */
public enum ProfileCommands {
    FIND_BY_ID("profile.person.findById"),
    FIND_STUDENT_BY_ID("profile.student.findById"),
    FIND_PRINCIPAL_BY_ID("profile.principal.findById"),
    CREATE_OR_UPDATE_STUDENT("profile.student.createOrUpdate"),
    CREATE_OR_UPDATE_PRINCIPAL("profile.principal.createOrUpdate")
    ;
    private final String commandId;
    ProfileCommands(String commandId) {
        this.commandId = commandId;
    }

    @Override
    public String toString() {
        return commandId;
    }
}
