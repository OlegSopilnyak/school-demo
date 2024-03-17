package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The enumeration of person-profile command-with-ids
 * @see oleg.sopilnyak.test.school.common.model.PersonProfile
 */
public enum ProfileCommands {
    FIND_BY_ID("profile.person.findById"),
    DELETE_BY_ID("profile.person.deleteById"),
    CREATE_OR_UPDATE("profile.person.createOrUpdate")
    ;
    private final String commandId;
    ProfileCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
