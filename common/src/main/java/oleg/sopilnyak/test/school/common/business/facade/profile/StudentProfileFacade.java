package oleg.sopilnyak.test.school.common.business.facade.profile;

import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import java.util.Optional;

/**
 * Service-Facade: Service for manage student profiles in the school
 *
 * @see StudentProfile
 * @see PersonProfileFacade
 */
public interface StudentProfileFacade extends PersonProfileFacade {

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
     * To create student-profile
     *
     * @param input instance to create
     * @return created instance or Optional#empty()
     * @see StudentProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<StudentProfile> createOrUpdateProfile(StudentProfile input) {
        return createOrUpdate(input).map(p -> p instanceof StudentProfile profile ? profile : null);
    }
}
