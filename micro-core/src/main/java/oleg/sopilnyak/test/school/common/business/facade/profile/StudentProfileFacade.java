package oleg.sopilnyak.test.school.common.business.facade.profile;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import java.util.List;
import java.util.Optional;

/**
 * Service-Facade: Service for manage student profiles in the school
 *
 * @see StudentProfile
 * @see PersonProfileFacade
 */
public interface StudentProfileFacade extends PersonProfileFacade, BusinessFacade {
    //
    // action-ids
    String PREFIX = "profile.student";
    String FIND_BY_ID = PREFIX + ".findById";
    String CREATE_OR_UPDATE = PREFIX + ".createOrUpdate";
    String DELETE_BY_ID = PREFIX + ".deleteById";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(FIND_BY_ID, CREATE_OR_UPDATE, DELETE_BY_ID);

    /**
     * To get the list of valid action-ids
     *
     * @return valid action-ids for concrete descendant-facade
     */
    @Override
    default List<String> validActions() {
        return ACTION_IDS;
    }

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
        final String validActionId = ACTION_IDS.stream().filter(id -> id.equals(actionId)).findFirst()
                .orElseThrow(
                        () -> new InvalidParameterTypeException(String.join(" or ", ACTION_IDS), actionId)
                );
        return concreteAction(validActionId, actionParameters);
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
     * @deprecated
     */
    @Deprecated
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
     * @deprecated
     */
    @Deprecated
    default Optional<StudentProfile> createOrUpdateProfile(StudentProfile input) {
        return createOrUpdate(input).map(p -> p instanceof StudentProfile profile ? profile : null);
    }
}
