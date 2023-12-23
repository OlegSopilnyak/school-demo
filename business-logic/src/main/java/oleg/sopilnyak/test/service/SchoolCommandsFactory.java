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
public class SchoolCommandsFactory implements CommandsFactory {
    private final String name;
    private final Map<String, SchoolCommand<?>> commandsMap = new HashMap<>();

    public SchoolCommandsFactory(String name, Collection<? extends SchoolCommand<?>> commands) {
        this.name = name;
        commands.forEach(command -> commandsMap.putIfAbsent(command.getId(), command));
    }

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @param <T>       type of command execution result
     * @return command instance
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> SchoolCommand<T> command(String commandId) {
        return (SchoolCommand<T>) commandsMap.get(commandId);
    }

}
