package oleg.sopilnyak.test.service.command.executable.profile.principal;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
import oleg.sopilnyak.test.service.command.type.PrincipalProfileCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to delete principal profile instance by id
 *
 * @see PrincipalProfileCommand
 * @see PrincipalProfile
 * @see ProfilePersistenceFacade
 * @see oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity
 */
@Slf4j
@Component
public class DeletePrincipalProfileCommand
        extends DeleteProfileCommand<Boolean, PrincipalProfile>
        implements PrincipalProfileCommand<Boolean> {
    /**
     * Constructor
     *
     * @param persistence persistence facade instance
     */
    public DeletePrincipalProfileCommand(ProfilePersistenceFacade persistence) {
        super(PrincipalProfile.class, persistence);
    }

    @Override
    protected LongFunction<Optional<PrincipalProfile>> functionFindById() {
        return persistence::findPrincipalProfileById;
    }

    @Override
    protected UnaryOperator<PrincipalProfile> functionCopyEntity() {
        return entity -> (PrincipalProfile) persistence.toEntity(entity);
    }

    @Override
    protected Function<PrincipalProfile, Optional<PrincipalProfile>> functionSave() {
        return persistence::save;
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
        return DELETE_BY_ID_COMMAND_ID;
    }
}
