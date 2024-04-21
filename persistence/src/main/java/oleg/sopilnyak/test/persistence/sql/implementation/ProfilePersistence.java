package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;
import static oleg.sopilnyak.test.school.common.business.PersonProfileFacade.isInvalidId;

/**
 * Persistence facade implementation for person-profile entities
 */
public interface ProfilePersistence extends ProfilePersistenceFacade {
    Logger getLog();

    SchoolEntityMapper getMapper();

    PersonProfileRepository<PersonProfileEntity> getPersonProfileRepository();

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
        return getPersonProfileRepository().findById(id).map(PersonProfile.class::cast);
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
        final PersonProfileEntity entity = profile instanceof PersonProfileEntity e ? e : toEntity(profile);
        if (isNull(entity)) {
            getLog().warn("Cannot transform to entity {}", profile);
            return Optional.empty();
        }
        final Long profileId = entity.getId();
        final PersonProfileEntity saved = getPersonProfileRepository().saveAndFlush(entity);
        final Long savedId = saved.getId();
        if (isInvalidId(profileId) || Objects.equals(profileId, savedId)) {
            getLog().debug("Saved PersonProfile '{}'", saved);
            return Optional.of(saved);
        } else {
            getPersonProfileRepository().deleteById(savedId);
            getPersonProfileRepository().flush();
            getLog().debug("Deleted wrong PersonProfile '{}'", saved);
            return Optional.empty();
        }
    }

    /**
     * To delete the profile by profile-id
     *
     * @param id the system-id of the profile
     * @throws ProfileNotExistsException if profile with id is not exists
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default void deleteProfileById(Long id) throws ProfileNotExistsException {
        getLog().debug("Deleting PersonProfile with ID:{}", id);
        if (this.findProfileById(id).isPresent()) {
            getPersonProfileRepository().deleteById(id);
            getPersonProfileRepository().flush();
            getLog().debug("Deleted PersonProfile with ID:{}", id);
        } else {
            getLog().warn("PersonProfile with ID:{} is not exists.", id);
            throw new ProfileNotExistsException("PersonProfile with ID:" + id + " is not exists.");
        }
    }

    /**
     * Convert profile to entity bean
     *
     * @param profile instance to convert
     * @return instance ready to use in the repository
     */
    @Override
    default PersonProfileEntity toEntity(PersonProfile profile) {
        if (profile instanceof StudentProfile student) {
            return toEntity(student);
        } else if (profile instanceof PrincipalProfile principal) {
            return toEntity(principal);
        } else return null;
    }

    private StudentProfileEntity toEntity(StudentProfile profile) {
        return getMapper().toEntity(profile);
    }

    private PrincipalProfileEntity toEntity(PrincipalProfile profile) {
        return getMapper().toEntity(profile);
    }
}
