package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.model.PersonProfile;

import java.util.Optional;

/**
 * Persistence facade for person-profile entities
 */
public interface ProfilePersistenceFacade {
    /**
     * To get person-profile instance by id
     *
     * @param id system-id of the course
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    Optional<PersonProfile> findProfileById(Long id);
}
