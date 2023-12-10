package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import java.util.Optional;

/**
 * Service-Facade: Service for manage person profiles in the school
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
     * To get the student's profile by ID
     *
     * @param id system-id of the student profile
     * @return profile instance or empty() if not exists
     * @see StudentProfile
     * @see StudentProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<StudentProfile> findStudentProfileById(Long id) {
        return findById(id).map(p -> p instanceof StudentProfile profile ? profile : null);
    }

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
}
