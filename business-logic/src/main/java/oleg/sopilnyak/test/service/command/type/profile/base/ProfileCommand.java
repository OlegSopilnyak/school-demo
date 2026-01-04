package oleg.sopilnyak.test.service.command.type.profile.base;

import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

/**
 * Type for school-profile command

 * @param <T> the type of command execution (do) result
 * @see PersonProfile
 * @see oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand
 * @see oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand
 */
public interface ProfileCommand<T> extends RootCommand<T> {
    String PROFILE_WITH_ID_PREFIX = "Profile with ID:";

    /**
     * To adopt person profile to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @param <E>    type of person profile to adopt
     * @return instance from business-logic data model
     */
    default <E extends PersonProfile> E adoptEntity(final E entity) {
        throw new UnsupportedOperationException("Please implement adopt entity in concrete command-class");
    }
}
