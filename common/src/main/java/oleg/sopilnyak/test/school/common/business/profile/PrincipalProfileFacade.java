package oleg.sopilnyak.test.school.common.business.profile;

import oleg.sopilnyak.test.school.common.business.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import java.util.Optional;

/**
 * Service-Facade: Service for manage principal profiles in the school
 *
 * @see PrincipalProfile
 * @see PersonProfileFacade
 */
public interface PrincipalProfileFacade extends PersonProfileFacade {
    /**
     * To get the principal's profile by ID
     *
     * @param id system-id of the principal profile
     * @return profile instance or empty() if not exists
     * @see PrincipalProfile
     * @see PrincipalProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<PrincipalProfile> findPrincipalProfileById(Long id) {
        return findById(id).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }

    /**
     * To create principal-profile
     *
     * @param input instance to create
     * @return created instance or Optional#empty()
     * @see PrincipalProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<PrincipalProfile> createOrUpdateProfile(PrincipalProfile input) {
        return createOrUpdate(input).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }
}
