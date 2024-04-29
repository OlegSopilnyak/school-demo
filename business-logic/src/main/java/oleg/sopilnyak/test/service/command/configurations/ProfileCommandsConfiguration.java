package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;

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
