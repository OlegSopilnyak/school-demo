package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;

/**
 * Type for school-organization authority persons macro deletion command
 *
 * @param <T> the type of command execution (do) result
 * @see AuthorityPersonCommand
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface MacroDeleteAuthorityPerson<T> extends AuthorityPersonCommand<T> {
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
