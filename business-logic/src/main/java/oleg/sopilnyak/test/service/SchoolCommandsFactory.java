package oleg.sopilnyak.test.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory: To manage school's commands
 */
@Slf4j
@AllArgsConstructor
public class SchoolCommandsFactory implements CommandsFactory {
    private final Map<String, SchoolCommand<?>> commandsMap = new HashMap<>();

    public SchoolCommandsFactory(Collection<SchoolCommand<?>> commands) {
        commands.forEach(command -> commandsMap.putIfAbsent(command.getId(), command));
    }

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance
     * @param <T> type of command execution result
     */
     @SuppressWarnings("unchecked")
    @Override
    public <T> SchoolCommand<T> command(String commandId) {
        return (SchoolCommand<T>) commandsMap.get(commandId);
    }
}
