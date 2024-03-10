package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.profile.FindPrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.FindStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
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
    private final ProfilePersistenceFacade persistenceFacade;

    @Bean
    public ProfileCommand<Optional<PersonProfile>> findProfileCommand() {
        return new FindProfileCommand(persistenceFacade);
    }

    @Bean
    public ProfileCommand<Optional<StudentProfile>> findStudentProfileCommand() {
        return new FindStudentProfileCommand(persistenceFacade);
    }

    @Bean
    public ProfileCommand<Optional<PrincipalProfile>> findPrincipalProfileCommand() {
        return new FindPrincipalProfileCommand(persistenceFacade);
    }

    /**
     * Builder for profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type ProfileCommand
     * @return singleton
     * @see ProfileCommand
     */
    @Bean(name = ProfileCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> profileCommandsFactory(final Collection<ProfileCommand<T>> commands) {
        return new ProfileCommandsFactory<>(commands);
    }
}
