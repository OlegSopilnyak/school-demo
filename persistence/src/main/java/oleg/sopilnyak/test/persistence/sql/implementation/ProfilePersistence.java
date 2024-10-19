package oleg.sopilnyak.test.persistence.sql.implementation;

import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_UNCOMMITTED)
    default Optional<PersonProfile> findProfileById(Long id) {
        getLog().debug("Looking for PersonProfile with ID:{}", id);
        return findPersonProfileById(id).map(PersonProfile.class::cast);
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
    default Optional<PersonProfile> saveProfile(final PersonProfile profile) {
        getLog().debug("Saving PersonProfile '{}'", profile);
        final PersonProfileEntity entity = profile instanceof PersonProfileEntity e ? e : toEntity(profile);

        getLog().debug("Checking PersonProfileEntity transformation...");
        if (isNull(entity)) {
            getLog().warn("Cannot transform to PersonProfileEntity {}", profile);
            return Optional.empty();
        }

        final Long profileId = entity.getId();

        if (nonNull(profileId)) {
            final Optional<PersonProfileEntity> existsEntity = findPersonProfileById(profileId);
            getLog().debug("Checking PersonProfileEntity entity state in the database...");
            if (existsEntity.isPresent()) {
                if (profile instanceof PrincipalProfile && !(existsEntity.get() instanceof PrincipalProfile)) {
                    getLog().warn("Found entity with ID:{} but with not PrincipalProfile type", profileId);
                    return Optional.empty();
                } else if (profile instanceof StudentProfile && !(existsEntity.get() instanceof StudentProfile)) {
                    getLog().warn("Found entity with ID:{} but with not StudentProfile type", profileId);
                    return Optional.empty();
                }
            } else {
                getLog().warn("Not found PersonProfileEntity with ID:{}", profileId);
                return Optional.empty();
            }
        }

        getLog().debug("Saving entity to the database. Entity to save:{}", entity);
        final PersonProfileEntity saved = getPersonProfileRepository().saveAndFlush(entity);
        getLog().debug("Saved PersonProfile '{}'", saved);
        return Optional.of(saved);
    }

    /**
     * To delete the profile by profile-id
     *
     * @param id the system-id of the profile
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default void deleteProfileById(Long id) {
        getLog().debug("Deleting PersonProfile with ID:{}", id);
        if (findPersonProfileById(id).isPresent()) {
            getPersonProfileRepository().deleteById(id);
            getPersonProfileRepository().flush();
            getLog().debug("Deleted PersonProfile with ID:{}", id);
        } else {
            getLog().warn("PersonProfile with ID:{} is not exists.", id);
            throw new ProfileIsNotFoundException("PersonProfile with ID:" + id + " is not exists.");
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

    // private methods
    private Optional<PersonProfileEntity> findPersonProfileById(final Long id) {
        return getPersonProfileRepository().findById(id);
    }

    private StudentProfileEntity toEntity(StudentProfile profile) {
        return getMapper().toEntity(profile);
    }

    private PrincipalProfileEntity toEntity(PrincipalProfile profile) {
        return getMapper().toEntity(profile);
    }
}
