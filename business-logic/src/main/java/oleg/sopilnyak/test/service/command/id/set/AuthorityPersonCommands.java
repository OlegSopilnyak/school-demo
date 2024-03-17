package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of authority-persons-command-ids
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public enum AuthorityPersonCommands {
    FIND_ALL("organization.authority.person.findAll"),
    FIND_BY_ID( "organization.authority.person.findById"),
    CREATE_OR_UPDATE( "organization.authority.person.createOrUpdate"),
    DELETE( "organization.authority.person.delete")
    ;
    private final String commandId;
    AuthorityPersonCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
