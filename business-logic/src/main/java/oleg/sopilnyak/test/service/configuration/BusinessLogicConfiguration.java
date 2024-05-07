package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.business.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.StudentsFacade;
import oleg.sopilnyak.test.school.common.business.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.business.profile.StudentProfileFacade;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.impl.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.facade.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.FacultyFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SchoolCommandsConfiguration.class})
public class BusinessLogicConfiguration {
    // --------- Business' facades ---------------
    @Bean
    public StudentsFacade studentsFacade(
            @Qualifier(StudentCommand.FACTORY_BEAN_NAME) CommandsFactory<StudentCommand> factory
    ) {
        return new StudentsFacadeImpl(factory);
    }

    @Bean
    public CoursesFacade coursesFacade(
            @Qualifier(CourseCommand.FACTORY_BEAN_NAME) CommandsFactory<CourseCommand> factory
    ) {
        return new CoursesFacadeImpl(factory);
    }

    @Bean
    public AuthorityPersonFacade authorityPersonFacade(
            @Qualifier(AuthorityPersonCommand.FACTORY_BEAN_NAME) CommandsFactory<AuthorityPersonCommand> factory
    ) {
        return new AuthorityPersonFacadeImpl(factory);
    }

    @Bean
    public FacultyFacade facultyFacade(
            @Qualifier(FacultyCommand.FACTORY_BEAN_NAME) CommandsFactory<FacultyCommand> factory
    ) {
        return new FacultyFacadeImpl(factory);
    }

    @Bean
    public StudentsGroupFacade studentsGroupFacade(
            @Qualifier(StudentsGroupCommand.FACTORY_BEAN_NAME) CommandsFactory<StudentsGroupCommand> factory
    ) {
        return new StudentsGroupFacadeImpl(factory);
    }

    @Bean
    public StudentProfileFacade studentProfileFacade(
            @Qualifier(StudentProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<StudentProfileCommand> factory
    ) {
        return new StudentProfileFacadeImpl(factory);
    }

    @Bean
    public PrincipalProfileFacade principalProfileFacade(
            @Qualifier(PrincipalProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<PrincipalProfileCommand> factory
    ) {
        return new PrincipalProfileFacadeImpl(factory);
    }

}
