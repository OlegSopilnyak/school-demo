package oleg.sopilnyak.test.school.common.business.facade.profile.base;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;

import java.util.Optional;

/**
 * Service-BaseFacade: Service for manage person profiles in the school
 *
 * @see BusinessFacade
 */
public interface PersonProfileFacade extends BusinessFacade {
    /**
     * Action ID of find person by id
     *
     * @return action-id value
     */
    String findByIdActionId();

    /**
     * Action ID of create or update person by person instance
     *
     * @return created or updated person instance value
     */
    String createOrUpdateActionId();

    /**
     * Action ID of delete person by id
     *
     * @return action-id value
     */
    String deleteByIdActionId();

    /**
     * Facade depended, action's execution
     *
     * @param actionId         the id of the action
     * @param actionParameters the parameters of action to execute
     * @param <T>              type of action execution result
     * @return action execution result value
     */
    @SuppressWarnings("unchecked")
    default <T> T concreteAction(String actionId, Object... actionParameters) {
        if (actionId.equals(findByIdActionId())) {
            final Long id = (Long) actionParameters[0];
            return (T) findById(id);
        } else if (actionId.equals(createOrUpdateActionId())) {
            final PersonProfile profile = (PersonProfile) actionParameters[0];
            return (T) createOrUpdate(profile);
        } else if (actionId.equals(deleteByIdActionId())) {
            final Long id = (Long) actionParameters[0];
            deleteById(id);
            return null;
        }
        throw new IllegalArgumentException("Unknown actionId: " + actionId);
    }

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
     * To create person-profile
     *
     * @param profile instance to create or update
     * @return created instance or Optional#empty()
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    Optional<PersonProfile> createOrUpdate(PersonProfile profile);

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws ProfileNotFoundException throws if profile with id does not exist
     */
    void deleteById(Long id) throws ProfileNotFoundException;

    /**
     * To delete the profile
     *
     * @param profile instance to delete
     * @throws ProfileNotFoundException throws if profile with id does not exist
     * @see PersonProfile
     */
    default void delete(PersonProfile profile) throws ProfileNotFoundException {
        if (PersistenceFacadeUtilities.isInvalid(profile)) {
            throw new ProfileNotFoundException("Wrong " + profile + " to delete");
        }
        deleteById(profile.getId());
    }
}
