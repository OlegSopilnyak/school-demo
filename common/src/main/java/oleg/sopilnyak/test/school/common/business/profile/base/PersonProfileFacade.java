package oleg.sopilnyak.test.school.common.business.profile.base;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;

import java.util.Optional;

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
     * @param profile instance to create or update
     * @return created instance or Optional#empty()
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    Optional<PersonProfile> createOrUpdate(PersonProfile profile);

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws NotExistProfileException throws if profile with id does not exist
     */
    void deleteById(Long id) throws NotExistProfileException;

    /**
     * To delete the profile
     *
     * @param profile instance to delete
     * @throws NotExistProfileException throws if profile with id does not exist
     * @see PersonProfile
     */
    default void delete(PersonProfile profile) throws NotExistProfileException {
        if (PersistenceFacadeUtilities.isInvalid(profile)) {
            throw new NotExistProfileException("Wrong " + profile + " to delete");
        }
        deleteById(profile.getId());
    }
}
