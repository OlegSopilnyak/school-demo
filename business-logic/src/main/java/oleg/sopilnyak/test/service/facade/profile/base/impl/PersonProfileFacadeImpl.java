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

    protected PersonProfileFacadeImpl(CommandsFactory<P> factory, CommandActionExecutor actionExecutor) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
    }

    /**
     * PersonProfile action processing facade method
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of action to execute
     * @return action execution result value
     * @see PersonProfileFacade#doActionAndResult(String, Object...)
     */
    @Override
    public <T> T personProfileAction(final String actionId, final Object... parameters) {
        final Object argument = parameters.length > 0 ? parameters[0] : null;
        getLogger().debug("Trying to execute action '{}' with argument {}", actionId, argument);
        return profileAction(actionId, argument);
    }

    /**
     * Concrete profile action processing facade method
     *
     * @param actionId   the id of the action
     * @param argument the parameters of action to execute
     * @return action execution result value
     * @param <T> execution result type
     */
    protected abstract <T> T profileAction(final String actionId, final Object argument);

    /**
     * To get the operator(function) to convert entity to payload
     *
     * @return unary operator
     * @see UnaryOperator#apply(Object)
     */
    protected abstract UnaryOperator<PersonProfile> toPayload();

    /**
     * To throw exception if action-id is invalid
     * @param actionId   the id of the action
     * @return does no matter
     */
    protected Object unknownActionId(final String actionId) {
        this.throwInvalidActionId(actionId);
        return "wrong-action-id";
    }

    /**
     * To get the person's profile by ID (for entry-point)
     *
     * @param actionId the action-id for feature
     * @param argument person instance id
     * @return found profile instance or empty
     * @see Optional
     */
    protected Optional<PersonProfile> internalFindById(final String actionId, final Object argument) {
        final Long id = toLong(argument);
        getLogger().debug("Getting profile by ID:{}", id);
        // running command execution
        final Input<Long> input = Input.of(id);
        final Optional<Optional<PersonProfile>> result = executeCommand(actionId, factory, input);
        return result.flatMap(profile -> {
            getLogger().debug("Found profile {}", profile);
            return profile.map(toPayload());
        });
    }

    /**
     * To create or update person-profile instance (for entry-point)
     *
     * @param actionId the action-id for feature
     * @param argument person instance to create or update
     * @return updated profile instance or empty
     * @see Optional
     */
    protected Optional<PersonProfile> internalCreateOrUpdate(final String actionId, final Object argument) {
        final PersonProfile instance = toPersonProfile(argument);
        getLogger().debug("Creating or Updating profile {}", instance);
        // running command execution
        final var input = Input.of(toPayload().apply(instance));
        final Optional<Optional<PersonProfile>> result = executeCommand(actionId, factory, input);
        return result.flatMap(profile -> {
            getLogger().debug("Changed profile {}", profile);
            return profile.map(toPayload());
        });
    }

    /**
     * To delete person-profile by id or profile (for entry-point)
     *
     * @param argument profile instance or profile-id to delete
     * @return nothing
     * @throws ProfileNotFoundException if profile isn't exists
     */
    protected Void internalDelete(final String actionId, final Object argument) throws ProfileNotFoundException {
        return switch (argument) {
            // delete by id
            case Long id -> internalDeleteById(actionId, id);
            // delete by profile (invalid profile)
            case PersonProfile profile when PersistenceFacadeUtilities.isInvalid(profile) ->
                    throw new ProfileNotFoundException("Wrong " + profile + " to delete");
            // delete by profile (valid profile)
            case PersonProfile profile -> internalDeleteById(actionId, profile.getId());
            // invalid argument type
            case null, default -> throw new InvalidParameterTypeException("Long or PersonProfile", argument);
        };
    }

    // private methods
    // To delete person-profile by profile-id (for internal usage)
    @Nullable
    private Void internalDeleteById(final String commandId, final Long id) {
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
}
