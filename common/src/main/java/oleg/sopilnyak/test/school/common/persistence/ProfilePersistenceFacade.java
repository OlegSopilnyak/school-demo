package oleg.sopilnyak.test.school.common.persistence;

import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;

import java.util.Optional;

/**
 * Persistence facade for person-profile entities
 */
public interface ProfilePersistenceFacade {
    /**
     * To get student-profile instance by id
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see StudentProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<StudentProfile> findStudentProfileById(Long id) {
        return findProfileById(id).map(p -> p instanceof StudentProfile profile ? profile : null);
    }

    /**
     * To get principal-profile instance by id
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PrincipalProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<PrincipalProfile> findPrincipalProfileById(Long id) {
        return findProfileById(id).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }

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

    /**
     * To save principal-profile instance
     *
     * @param input instance to save
     * @return saved instance of empty() if cannot
     * @see PrincipalProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<PrincipalProfile> save(PrincipalProfile input) {
        return saveProfile(input).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }

    /**
     * To save student-profile instance
     *
     * @param input instance to save
     * @return saved instance of empty() if cannot
     * @see StudentProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<StudentProfile> save(StudentProfile input) {
        return saveProfile(input).map(p -> p instanceof StudentProfile profile ? profile : null);
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
    Optional<PersonProfile> saveProfile(PersonProfile profile);

    /**
     * To delete the profile by profile-id
     *
     * @param id the system-id of the profile
     */
    void deleteProfileById(Long id);

    /**
     * Convert profile to entity bean
     *
     * @param profile instance to convert
     * @return instance ready to use in the repository
     */
    PersonProfile toEntity(PersonProfile profile);
}
