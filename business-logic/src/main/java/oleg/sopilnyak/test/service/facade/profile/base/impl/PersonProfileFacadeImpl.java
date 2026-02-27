package oleg.sopilnyak.test.service.facade.profile.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.springframework.lang.Nullable;
import lombok.Getter;

/**
 * Service: To process commands for school's person profiles facades
 *
 * @see oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl
 * @see oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl
 */
public abstract class PersonProfileFacadeImpl<P extends ProfileCommand<?>> implements PersonProfileFacade, ActionFacade {

    private final CommandsFactory<P> factory;
    @Getter
    private final CommandActionExecutor actionExecutor;

    protected PersonProfileFacadeImpl(CommandsFactory<P> factory, BusinessMessagePayloadMapper mapper,
                                      CommandActionExecutor actionExecutor
    ) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
        prepareToPayloadFunction(mapper);
    }

    /**
     * To prepare the operator to convert entity to payload
     *
     * @param mapper layer's data mapper
     * @see PersonProfileFacadeImpl#toPayload()
     * @see BusinessMessagePayloadMapper
     */
    protected void prepareToPayloadFunction(final BusinessMessagePayloadMapper mapper) {
        throw new UnsupportedOperationException("Please implement method in PersonProfileFacadeImpl's descendant.");
    }

    /**
     * To get the operator to convert entity to payload
     *
     * @return unary operator
     * @see UnaryOperator#apply(Object)
     */
    protected UnaryOperator<PersonProfile> toPayload() {
        throw new UnsupportedOperationException("Please implement method in PersonProfileFacadeImpl's descendant.");
    }

    /**
     * PersonProfile action processing facade method
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of action to execute
     * @return action execution result value
     * @see PersonProfileFacade#doActionAndResult(String, Object...)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T personProfileAction(final String actionId, final Object... parameters) {
        final Object argument = parameters.length > 0 ? parameters[0] : null;
        getLogger().debug("Trying to execute action '{}' with argument {}", actionId, argument);
        return (T) switch (actionId) {
            case String id when findByIdActionId().equals(id) -> this.internalFindById(argument);
            case String id when createOrUpdateActionId().equals(id) -> this.internalCreateOrUpdate(argument);
            case String id when deleteByIdActionId().equals(id) -> this.internalDelete(argument);
            default -> throwsUnknownActionId(actionId).apply(null);
        };
    }

    // private methods
    // throws exception if action-id is invalid
    // @see throwInvalidActionId(actionId)
    private UnaryOperator<Object> throwsUnknownActionId(final String actionId) {
        this.throwInvalidActionId(actionId);
        return null;
    }

    // To get the person's profile by ID (for entry-point)
    private Optional<PersonProfile> internalFindById(final Object argument) {
        final Long id = toLong(argument);
        getLogger().debug("Getting profile by ID:{}", id);
        // running command execution
        final Input<Long> input = Input.of(id);
        final Optional<Optional<PersonProfile>> result = executeCommand(findByIdActionId(), factory, input);
        return result.flatMap(profile -> {
            getLogger().debug("Found profile {}", profile);
            return profile.map(toPayload());
        });
    }

    // To create or update person-profile (for entry-point)
    private Optional<PersonProfile> internalCreateOrUpdate(final Object argument) {
        final PersonProfile instance = toPersonProfile(argument);
        getLogger().debug("Creating or Updating profile {}", instance);
        // running command execution
        final var input = Input.of(toPayload().apply(instance));
        final Optional<Optional<PersonProfile>> result = executeCommand(createOrUpdateActionId(), factory, input);
        return result.flatMap(profile -> {
            getLogger().debug("Changed profile {}", profile);
            return profile.map(toPayload());
        });
    }

    // To delete person-profile by id or profile (for entry-point)
    public Void internalDelete(final Object argument) throws ProfileNotFoundException {
        return switch (argument) {
            // delete by id
            case Long id -> internalDeleteById(id);
            // delete by profile (invalid profile)
            case PersonProfile profile when PersistenceFacadeUtilities.isInvalid(profile) ->
                    throw new ProfileNotFoundException("Wrong " + profile + " to delete");
            // delete by profile (valid profile)
            case PersonProfile profile -> internalDeleteById(profile.getId());
            // invalid argument type
            case null, default -> throw new InvalidParameterTypeException("Long or PersonProfile", argument);
        };
    }

    // To delete person-profile by profile-id (for internal usage)
    @Nullable
    private Void internalDeleteById(Long id) {
        final String commandId = deleteByIdActionId();
        // prepare execution custom error-handler
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case ProfileNotFoundException profileNotFoundException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw profileNotFoundException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };
        // running command execution
        getLogger().debug("Deleting profile with ID:{}", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doOnError);
        result.ifPresent(success ->
                getLogger().debug("Deleted profile with ID:{} successfully:{} .", id, success)
        );
        // returns nothing
        return null;
    }

    // convert argument to particular type
    private static Long toLong(final Object argument) {
        if (argument instanceof Long id) {
            return id;
        } else {
            throw new InvalidParameterTypeException("Long", argument);
        }
    }

    private static PersonProfile toPersonProfile(final Object argument) {
        if (argument instanceof PersonProfile profile) {
            return profile;
        } else {
            throw new InvalidParameterTypeException("PersonProfile", argument);
        }
    }
}
