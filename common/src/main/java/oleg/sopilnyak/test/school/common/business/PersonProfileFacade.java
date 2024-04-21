package oleg.sopilnyak.test.school.common.business;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Service-BaseFacade: Service for manage person profiles in the school
 */
public interface PersonProfileFacade {
    /**
     * To get the person's profile by ID
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see PersonProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    Optional<PersonProfile> findById(Long id);

    /**
     * To create person-profile
     *
     * @param profile instance to create
     * @return created instance or Optional#empty()
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    Optional<PersonProfile> createOrUpdatePersonProfile(PersonProfile profile);

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws ProfileNotExistsException throws if profile with id does not exist
     */
    void deleteProfileById(Long id) throws ProfileNotExistsException;

    /**
     * To delete the profile
     *
     * @param profile instance to delete
     * @throws ProfileNotExistsException throws if profile with id does not exist
     * @see PersonProfile
     */
    default void deleteProfile(PersonProfile profile) throws ProfileNotExistsException {
        if (isInvalid(profile)) {
            throw new ProfileNotExistsException("Wrong " + profile + " to delete");
        }
        deleteProfileById(profile.getId());
    }

    static boolean isInvalid(final PersonProfile instance) {
        return isNull(instance) || isInvalidId(instance.getId());
    }

    static boolean isInvalidId(final Long instanceId) {
        return isNull(instanceId) || instanceId <= 0L;
    }
}
