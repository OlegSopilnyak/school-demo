package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import java.util.Optional;

import static java.util.Objects.isNull;

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

    /**
     * To create student-profile
     *
     * @param input instance to create
     * @return created instance or Optional#empty()
     * @see StudentProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<StudentProfile> createOrUpdateProfile(StudentProfile input){
        return createOrUpdatePersonProfile(input).map(p -> p instanceof StudentProfile profile ? profile : null);
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
    default Optional<PrincipalProfile> createOrUpdateProfile(PrincipalProfile input){
        return createOrUpdatePersonProfile(input).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }

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

    private static boolean isInvalid(PersonProfile instance) {
        return isNull(instance) || isNull(instance.getId()) || instance.getId() < 0L;
    }
}
