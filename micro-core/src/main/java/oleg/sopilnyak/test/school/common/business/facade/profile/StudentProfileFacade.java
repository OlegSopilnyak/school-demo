package oleg.sopilnyak.test.school.common.business.facade.profile;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import java.util.Optional;

/**
 * Service-Facade: Service for manage student profiles in the school
 *
 * @see StudentProfile
 * @see PersonProfileFacade
 */
public interface StudentProfileFacade extends PersonProfileFacade, BusinessFacade {
    String PREFIX = "profile.student";
    String FIND_BY_ID = PREFIX + ".findById";
    String CREATE_OR_UPDATE = PREFIX + ".createOrUpdate";
    String DELETE_BY_ID = PREFIX + ".deleteById";

    /**
     * Action ID of find person by id
     *
     * @return action-id value
     */
    @Override
    default String findByIdActionId() {
        return FIND_BY_ID;
    }

    /**
     * Action ID of create or update person by person instance
     *
     * @return created or updated person instance value
     */
    @Override
    default String createOrUpdateActionId() {
        return CREATE_OR_UPDATE;
    }

    /**
     * Action ID of delete person by id
     *
     * @return action-id value
     */
    @Override
    default String deleteByIdActionId() {
        return DELETE_BY_ID;
    }

    /**
     * To do action and return the result
     *
     * @param actionId         the id of the action
     * @param actionParameters the parameters of action to execute
     * @param <T>              type of action execution result
     * @return action execution result value
     */
    @Override
    default <T> T doActionAndResult(String actionId, Object... actionParameters) {
        return switch (actionId) {
            case FIND_BY_ID,
                 CREATE_OR_UPDATE,
                 DELETE_BY_ID -> concreteAction(actionId, actionParameters);
            case null, default -> throw new IllegalArgumentException("Unknown actionId: " + actionId);
        };
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "StudentProfileFacade";
    }

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
