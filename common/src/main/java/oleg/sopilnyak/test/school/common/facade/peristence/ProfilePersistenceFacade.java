package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

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
    default Optional<StudentProfile> findStudentProfileById(Long id){
        final Optional<PersonProfile> profile = findProfileById(id);
        return profile.isPresent() && profile.get() instanceof StudentProfile entity ?
                Optional.of(entity) : Optional.empty();
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
    default Optional<PrincipalProfile> findPrincipalProfileById(Long id){
        final Optional<PersonProfile> profile = findProfileById(id);
        return profile.isPresent() && profile.get() instanceof PrincipalProfile entity ?
                Optional.of(entity) : Optional.empty();
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
        final Optional<PersonProfile> profile = saveProfile(input);
        return profile.isPresent() && profile.get() instanceof PrincipalProfile entity ?
                Optional.of(entity) : Optional.empty();
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
        final Optional<PersonProfile> profile = saveProfile(input);
        return profile.isPresent() && profile.get() instanceof StudentProfile entity ?
                Optional.of(entity) : Optional.empty();

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
}
