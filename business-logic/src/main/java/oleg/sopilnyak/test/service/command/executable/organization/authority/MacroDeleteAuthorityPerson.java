package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;

/**
 * Type for school-organization authority persons macro deletion command  (for create the command context)
 *
 * @param <T> the type of command execution (do) result
 * @see AuthorityPersonCommand
 * @see AuthorityPerson
 */
public interface MacroDeleteAuthorityPerson<T> extends AuthorityPersonCommand<T> {
    /**
     * The class of commands family, the command is belonged to
     *
     * @return command family class value
     * @see BasicCommand#self()
     */
    @Override
    @SuppressWarnings("unchecked")
    default <F extends RootCommand> Class<F> commandFamily() {
        return (Class<F>) MacroDeleteAuthorityPerson.class;
    }
    /**
     * To create context for delete person profile nested command
     *
     * @param command  delete principal person profile command instance
     * @param personId related person-id value
     * @param <N>      type of delete principal profile nested command result
     * @return built context of the command for input parameter
     */
    <N> Context<N> createPrincipalProfileContext(PrincipalProfileCommand<N> command, Long personId);
}
