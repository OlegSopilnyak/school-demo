package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.business.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.StudentsFacade;
import oleg.sopilnyak.test.school.common.business.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.business.profile.StudentProfileFacade;
import oleg.sopilnyak.test.service.command.factory.*;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.*;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.impl.*;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.FacultyFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
public class BusinessLogicConfiguration {
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

// --------- Business' facades ---------------
    @Bean
    public StudentsFacade studentsFacade(
            @Qualifier(StudentCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new StudentsFacadeImpl<>(factory);
    }

    @Bean
    public CoursesFacade coursesFacade(
            @Qualifier(CourseCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new CoursesFacadeImpl<>(factory);
    }

    @Bean
    public AuthorityPersonFacade authorityPersonFacade(
            @Qualifier(AuthorityPersonCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new AuthorityPersonFacadeImpl(factory);
    }

    @Bean
    public FacultyFacade facultyFacade(
            @Qualifier(FacultyCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new FacultyFacadeImpl(factory);
    }

    @Bean
    public StudentsGroupFacade studentsGroupFacade(
            @Qualifier(StudentsGroupCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new StudentsGroupFacadeImpl(factory);
    }

    @Bean
    public StudentProfileFacade studentProfileFacade(
            @Qualifier(StudentProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new StudentProfileFacadeImpl<>(factory);
    }

    @Bean
    public PrincipalProfileFacade principalProfileFacade(
            @Qualifier(PrincipalProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new PrincipalProfileFacadeImpl<>(factory);
    }

}
