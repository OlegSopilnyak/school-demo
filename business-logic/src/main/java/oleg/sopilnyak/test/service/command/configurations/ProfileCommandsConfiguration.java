package oleg.sopilnyak.test.service.command.configurations;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

/**
 * Configuration for profiles-subsystem commands
 */
@Configuration
public class ProfileCommandsConfiguration {
    public static final String COMMANDS_FACTORY = "profileCommandsFactory";
    private final PersistenceFacade persistenceFacade;

    public ProfileCommandsConfiguration(final PersistenceFacade persistenceFacade) {
        this.persistenceFacade = persistenceFacade;
    }

    /**
     * Builder for profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type ProfileCommand
     * @return singleton
     * @see ProfileCommand
     */
    @Bean(name = COMMANDS_FACTORY)
    public <T> CommandsFactory<T> profileCommandsFactory(final Collection<ProfileCommand<T>> commands) {
        return new ProfileCommandsFactory<>(commands);
    }
}
