package oleg.sopilnyak.test.service.command.type.profile.base;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Type for school-profile command

 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand
 * @see oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand
 */
public interface ProfileCommand<T> extends RootCommand<T> {
    String PROFILE_WITH_ID_PREFIX = "Profile with ID:";
}
