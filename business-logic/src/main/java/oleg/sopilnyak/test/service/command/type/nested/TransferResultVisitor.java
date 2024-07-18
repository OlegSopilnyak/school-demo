package oleg.sopilnyak.test.service.command.type.nested;

import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import org.slf4j.Logger;

import java.util.Deque;
import java.util.Optional;

/**
 * Visitor: Transfer command result to next command context
 */
public interface TransferResultVisitor {
    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see RootCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final RootCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see StudentCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final StudentCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see CourseCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final CourseCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see CompositeCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final CompositeCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see PrincipalProfileCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final PrincipalProfileCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see StudentProfileCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final StudentProfileCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see AuthorityPersonCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final AuthorityPersonCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see FacultyCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final FacultyCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To transfer result from current command to next command context
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @param <S>     type of current result
     * @param <T>     type of next command result
     * @see StudentsGroupCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see Optional#get()
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     */
    default <S, T> void transferPreviousExecuteDoResult(final StudentsGroupCommand command,
                                                        final S result,
                                                        final Context<T> target) {
        defaultResultTransfer(command, result, target);
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    private <S, T, C extends RootCommand> void defaultResultTransfer(final C command, final S result, final Context<T> target) {
        getLog().debug("Default command execution result transfer for '{}' result:{} to context:{}", command, result, target);
    }
}
