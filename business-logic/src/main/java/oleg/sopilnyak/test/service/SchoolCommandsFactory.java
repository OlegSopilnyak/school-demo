package oleg.sopilnyak.test.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory: To manage school's commands
 */
@Slf4j
@Getter
public class SchoolCommandsFactory<T> implements CommandsFactory<T> {
    private final String name;
    private final Map<String, SchoolCommand<T>> commandsMap = new HashMap<>();

    public SchoolCommandsFactory(String name, Collection<? extends SchoolCommand<T>> commands) {
        this.name = name;
        commands.forEach(command -> commandsMap.putIfAbsent(command.getId(), command));
    }

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance
     */
    @Override
    public SchoolCommand<T> command(String commandId) {
        return commandsMap.get(commandId);
    }

}
