package oleg.sopilnyak.test.service.command.id.set;

import static oleg.sopilnyak.test.service.command.type.ProfileCommand.*;

/**
 * Service-Command-Ids: The enumeration of person-profile command-with-ids
 * @see oleg.sopilnyak.test.school.common.model.PersonProfile
 */
public enum ProfileCommands {
    FIND_BY_ID(FIND_BY_ID_COMMAND_ID),
    DELETE_BY_ID(DELETE_BY_ID_COMMAND_ID),
    CREATE_OR_UPDATE(CREATE_OR_UPDATE_COMMAND_ID)
    ;
    private final String commandId;
    ProfileCommands(String commandId) {
        this.commandId = commandId;
    }

    public String id() {
        return commandId;
    }
}
