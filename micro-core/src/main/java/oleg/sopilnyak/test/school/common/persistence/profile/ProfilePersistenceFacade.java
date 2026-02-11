package oleg.sopilnyak.test.school.common.persistence.profile;

import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;

import java.util.Optional;

/**
 * Persistence facade for person-profile entities
 */
public interface ProfilePersistenceFacade {
    /**
     * To get principal-profile instance by login
     *
     * @param login the value of profile login to get
     * @return profile instance or empty() if not exists
     * @see PrincipalProfile
     * @see Optional
     * @see Optional#empty()
     */
    default Optional<PrincipalProfile> findPrincipalProfileByLogin(String login){
        return findPersonProfileByLogin(login).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }

    /**
     * To get person-profile instance by login value
     *
     * @param login system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    Optional<PersonProfile> findPersonProfileByLogin(String login);

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
     * @param id system-id of the profile
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
    <T extends PersonProfile> T toEntity(T profile);
}
