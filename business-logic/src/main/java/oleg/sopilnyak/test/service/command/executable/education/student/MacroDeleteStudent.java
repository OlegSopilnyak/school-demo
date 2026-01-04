package oleg.sopilnyak.test.service.command.executable.education.student;

import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;

/**
 * Type for students macro deletion command (for create the command context)
 *
 * @param <T> the type of command execution (do) result
 * @see StudentCommand
 */
public interface MacroDeleteStudent<T> extends StudentCommand<T> {
    /**
     * The class of commands family, the command is belonged to
     *
     * @return command family class value
     * @see BasicCommand#self()
     */
    @Override
    @SuppressWarnings("unchecked")
    default <F extends RootCommand> Class<F> commandFamily() {
        return (Class<F>) MacroDeleteStudent.class;
    }

    /**
     * To create context for delete person profile nested command
     *
     * @param command  delete student profile command instance
     * @param personId related person-id value
     * @param <N>      type of delete student profile nested command result
     * @return built context of the command for input parameter
     */
    <N> Context<N> createStudentProfileContext(StudentProfileCommand<N> command, Long personId);
}
