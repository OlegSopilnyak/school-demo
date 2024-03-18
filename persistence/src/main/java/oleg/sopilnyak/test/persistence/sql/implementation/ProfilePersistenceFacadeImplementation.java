package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persistence facade implementation for person-profile entities
 */
public interface ProfilePersistenceFacadeImplementation extends ProfilePersistenceFacade {
    Logger getLog();

    SchoolEntityMapper getMapper();
    /**
     * To get person-profile instance by id
     *
     * @param id system-id of the course
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    default Optional<PersonProfile> findProfileById(Long id) {
        getLog().debug("Looking for PersonProfile with ID:{}", id);
        return Optional.empty();
    }

    /**
     * To save principal-profile instance
     *
     * @param profile instance to save
     * @return saved instance of empty() if cannot
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default Optional<PersonProfile> saveProfile(PersonProfile profile) {
        getLog().debug("Saving PersonProfile '{}'", profile);
        return Optional.empty();
    }

    /**
     * To delete the profile by profile-id
     *
     * @param id the system-id of the profile
     * @return true if success
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default boolean deleteProfileById(Long id) {
        getLog().debug("Deleting PersonProfile with ID:{}", id);
        return false;
    }
}
