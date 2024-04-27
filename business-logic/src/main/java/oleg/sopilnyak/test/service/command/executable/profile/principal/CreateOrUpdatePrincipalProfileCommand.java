package oleg.sopilnyak.test.service.command.executable.profile.principal;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.type.PrincipalProfileCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;


/**
 * Command-Implementation: command to update principal profile instance
 *
 * @see PrincipalProfileCommand
 * @see PrincipalProfile
 * @see ProfilePersistenceFacade
 * @see oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity
 * @see CreateOrUpdateProfileCommand
 */
@Slf4j
@Component
public class CreateOrUpdatePrincipalProfileCommand
        extends CreateOrUpdateProfileCommand<Optional<PrincipalProfile>, PrincipalProfile>
        implements PrincipalProfileCommand<Optional<PrincipalProfile>> {

    /**
     * Constructor
     *
     * @param persistence facade of persistence layer
     */
    public CreateOrUpdatePrincipalProfileCommand(ProfilePersistenceFacade persistence) {
        super(PrincipalProfile.class, persistence);
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CREATE_OR_UPDATE;
    }

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    @Override
    protected LongFunction<Optional<PrincipalProfile>> functionFindById() {
        return persistence::findPrincipalProfileById;
    }

    /**
     * to get function to copy the entity
     *
     * @return function implementation
     */
    @Override
    protected UnaryOperator<PrincipalProfile> functionCopyEntity() {
        return entity -> (PrincipalProfile) persistence.toEntity(entity);
    }

    /**
     * to get function to persist the entity
     *
     * @return function implementation
     */
    @Override
    protected Function<PrincipalProfile, Optional<PrincipalProfile>> functionSave() {
        return persistence::save;
    }
}
