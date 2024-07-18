package oleg.sopilnyak.test.service.command.type.nested;

import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;

/**
 * Visitor: Prepare nested command context
 */
public interface PrepareContextVisitor {
    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see NestedCommand
     * @see RootCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final NestedCommand command, final Object mainInput) {
        return prepareContext((RootCommand) command, mainInput);
    }
    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see RootCommand
     * @see RootCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final RootCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see SequentialMacroCommand
     * @see SequentialMacroCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final SequentialMacroCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see ParallelMacroCommand
     * @see ParallelMacroCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final ParallelMacroCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see CourseCommand
     * @see CourseCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final CourseCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see StudentCommand
     * @see StudentCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final StudentCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see PrincipalProfileCommand
     * @see PrincipalProfileCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final PrincipalProfileCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see StudentProfileCommand
     * @see StudentProfileCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final StudentProfileCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see AuthorityPersonCommand
     * @see AuthorityPersonCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final AuthorityPersonCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see FacultyCommand
     * @see FacultyCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final FacultyCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of command's do result
     * @return built context of the command for input parameter
     * @see StudentsGroupCommand
     * @see StudentsGroupCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(final StudentsGroupCommand command, final Object mainInput) {
        return command.createContext(mainInput);
    }
}
