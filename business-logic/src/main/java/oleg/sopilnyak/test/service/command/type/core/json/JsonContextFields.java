package oleg.sopilnyak.test.service.command.type.core.json;

/**
 * Constants: the enumeration of field names for Json serialization/deserialization
 *
 * @see oleg.sopilnyak.test.service.command.executable.core.context.CommandContext
 */
public interface JsonContextFields {
    String COMMAND_FIELD_NAME = "command";
    String COMMAND_ID_FIELD_NAME = "id";
    String COMMAND_FAMILY_FIELD_NAME = "family";
    String STARTED_AT_FIELD_NAME = "started-at";
    String DURATION_FIELD_NAME = "duration";
    String STATE_FIELD_NAME = "state";
    String RESULT_FIELD_NAME = "result";
    String REDO_INPUT_FIELD_NAME = "redo-input";
    String UNDO_INPUT_FIELD_NAME = "undo-input";
    String ERROR_FIELD_NAME = "error";
    String HISTORY_FIELD_NAME = "history";
}
