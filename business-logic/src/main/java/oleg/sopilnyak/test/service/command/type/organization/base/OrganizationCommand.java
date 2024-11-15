package oleg.sopilnyak.test.service.command.type.organization.base;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Type for organization entities management command
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand
 * @see oleg.sopilnyak.test.service.command.type.organization.FacultyCommand
 * @see oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand
 */
public interface OrganizationCommand<T> extends RootCommand<T> {
}
