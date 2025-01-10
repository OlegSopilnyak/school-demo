package oleg.sopilnyak.test.service.command.type.nested;

import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
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
     * To prepare context for particular type (NestedCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see NestedCommand
     * @see RootCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final NestedCommand<T> command, final Input<?> macroInputParameter) {
        return prepareContext((RootCommand<T>) command, macroInputParameter);
    }

    /**
     * To prepare context for particular type (RootCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see RootCommand
     * @see RootCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final RootCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (SequentialMacroCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see SequentialMacroCommand
     * @see SequentialMacroCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final SequentialMacroCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (ParallelMacroCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see ParallelMacroCommand
     * @see ParallelMacroCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final ParallelMacroCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (CourseCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see CourseCommand
     * @see CourseCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final CourseCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (StudentCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see StudentCommand
     * @see StudentCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final StudentCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (PrincipalProfileCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see PrincipalProfileCommand
     * @see PrincipalProfileCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final PrincipalProfileCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (StudentProfileCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see StudentProfileCommand
     * @see StudentProfileCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final StudentProfileCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (AuthorityPersonCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see AuthorityPersonCommand
     * @see AuthorityPersonCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final AuthorityPersonCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (FacultyCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see FacultyCommand
     * @see FacultyCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final FacultyCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }

    /**
     * To prepare context for particular type (StudentsGroupCommand) of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <T>                 type of command's do result
     * @return built context of the nested command for the macro input parameter
     * @see StudentsGroupCommand
     * @see StudentsGroupCommand#createContext(Input)
     * @see Context
     */
    default <T> Context<T> prepareContext(final StudentsGroupCommand<T> command, final Input<?> macroInputParameter) {
        return command.createContext(macroInputParameter);
    }
}
