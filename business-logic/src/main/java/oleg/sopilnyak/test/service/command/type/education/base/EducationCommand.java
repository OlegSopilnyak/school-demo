package oleg.sopilnyak.test.service.command.type.education.base;


import oleg.sopilnyak.test.service.command.type.core.RootCommand;

/**
 * Type for education entities management command
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see oleg.sopilnyak.test.service.command.type.education.CourseCommand
 * @see oleg.sopilnyak.test.service.command.type.education.StudentCommand
 */
public interface EducationCommand<T> extends RootCommand<T> {
}
