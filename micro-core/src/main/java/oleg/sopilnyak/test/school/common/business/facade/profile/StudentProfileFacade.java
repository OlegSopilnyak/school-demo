package oleg.sopilnyak.test.school.common.business.facade.profile;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
    //
    // function to adopt person-profile result to principal-profile one, if it's possible
    Function<PersonProfile, StudentProfile> STRICT_CASTING =
            p -> p instanceof StudentProfile profile ? profile : null;

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
     * @param parameters the parameters of action to execute
     * @param <T>              type of action execution result
     * @return action execution result value
     */
    @Override
    default <T> T doActionAndResult(String actionId, Object... parameters) {
        return switch (actionId) {
            case FIND_BY_ID, CREATE_OR_UPDATE -> {
                final Optional<PersonProfile> result = personProfileAction(actionId, parameters);
                yield (T) result.map(STRICT_CASTING);
            }
            case  DELETE_BY_ID -> personProfileAction(actionId, parameters);
            case null, default -> throw new InvalidParameterTypeException(String.join(" or ", ACTION_IDS), actionId);
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
}
