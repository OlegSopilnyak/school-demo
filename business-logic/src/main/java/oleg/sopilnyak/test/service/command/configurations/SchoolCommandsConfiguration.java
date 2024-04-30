package oleg.sopilnyak.test.service.command.configurations;

import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

/**
 * Configuration for courses-subsystem commands
 */
@Configuration
@ComponentScan("oleg.sopilnyak.test.service.command.executable")
public class SchoolCommandsConfiguration {
// ----------------- Commands factories ----------------

    /**
     * Builder for student commands factory instance
     *
     * @param commands injected by Spring list of commands having type StudentCommand
     * @return singleton
     * @see StudentCommand
     */
    @Bean(name = StudentCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> studentsCommandsFactory(final Collection<StudentCommand<T>> commands) {
        return new StudentCommandsFactory<>(commands);
    }

    /**
     * Builder for course commands factory instance
     *
     * @param commands injected by Spring list of commands having type CourseCommand
     * @return singleton
     * @see CourseCommand
     */
    @Bean(name = CourseCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> courseCommandsFactory(final Collection<CourseCommand<T>> commands) {
        return new CourseCommandsFactory<>(commands);
    }

    /**
     * Builder for authority person commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see AuthorityPersonCommand
     */
    @Bean(name = AuthorityPersonCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> authorityPersonCommandFactory(final Collection<AuthorityPersonCommand<T>> commands) {
        return new AuthorityPersonCommandsFactory<>(commands);
    }

    /**
     * Builder for faculty commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see FacultyCommand
     */
    @Bean(name = FacultyCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> facultyCommandFactory(final Collection<FacultyCommand<T>> commands) {
        return new FacultyCommandsFactory<>(commands);
    }

    /**
     * Builder for students group commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see StudentsGroupCommand
     */
    @Bean(name = StudentsGroupCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> studentsGroupCommandFactory(final Collection<StudentsGroupCommand<T>> commands) {
        return new StudentsGroupCommandsFactory<>(commands);
    }

    /**
     * Builder for student profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type ProfileCommand
     * @return singleton
     * @see StudentProfileCommand
     */
    @Bean(name = StudentProfileCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> studentProfileCommandsFactory(final Collection<StudentProfileCommand<T>> commands) {
        return new StudentProfileCommandsFactory<>(commands);
    }

    /**
     * Builder for principal profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type ProfileCommand
     * @return singleton
     * @see PrincipalProfileCommand
     */
    @Bean(name = PrincipalProfileCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> principalProfileCommandsFactory(final Collection<PrincipalProfileCommand<T>> commands) {
        return new PrincipalProfileCommandsFactory<>(commands);
    }

// ----------------- Commands Factories Farm ----------------
    /**
     * Builder for commands factories farm instance
     *
     * @param factories collection of commands factories
     * @return singleton
     * @see CommandsFactory
     */
    @Bean(name = CommandsFactoriesFarm.FARM_BEAN_NAME)
    public <T> CommandsFactoriesFarm<T> createCommandsFactoriesFarm(final Collection<CommandsFactory<T>> factories) {
        return new CommandsFactoriesFarm<>(factories);
    }
}
