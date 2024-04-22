package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Optional;

/**
 * Configuration for profiles-subsystem commands
 */
@Configuration
@AllArgsConstructor
public class ProfileCommandsConfiguration {
//    private final ProfilePersistenceFacade persistenceFacade;
//
//    @Bean(ProfileCommand.FIND_BY_ID_COMMAND_ID)
//    public ProfileCommand<Optional<PersonProfile>> findProfileCommand() {
//        return new FindProfileCommand(persistenceFacade);
//    }
//
//    @Bean(ProfileCommand.CREATE_OR_UPDATE_COMMAND_ID)
//    public ProfileCommand<Optional<? extends PersonProfile>> createProfileCommand() {
//        return new CreateOrUpdateProfileCommand(persistenceFacade);
//    }
//
//    @Bean(ProfileCommand.DELETE_BY_ID_COMMAND_ID)
//    public ProfileCommand<Boolean> deleteProfileCommand() {
//        return new DeleteProfileCommand(persistenceFacade);
//    }
//
//    /**
//     * Builder for profile commands factory instance
//     *
//     * @param commands injected by Spring list of commands having type ProfileCommand
//     * @return singleton
//     * @see ProfileCommand
//     */
//    @Bean(name = ProfileCommand.FACTORY_BEAN_NAME)
//    public <T> CommandsFactory<T> profileCommandsFactory(final Collection<ProfileCommand<T>> commands) {
//        return new ProfileCommandsFactory<>(commands);
//    }
}
