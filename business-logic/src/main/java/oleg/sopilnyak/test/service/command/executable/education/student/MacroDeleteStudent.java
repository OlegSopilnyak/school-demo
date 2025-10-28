package oleg.sopilnyak.test.service.command.executable.education.student;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;

/**
 * Type for students macro deletion command (for create the command context)
 *
 * @param <T> the type of command execution (do) result
 * @see StudentCommand
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public interface MacroDeleteStudent<T> extends StudentCommand<T> {
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
