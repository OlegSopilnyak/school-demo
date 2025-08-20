package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.education.impl.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.facade.education.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.facade.impl.ActionExecutorImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.FacultyFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SchoolCommandsConfiguration.class})
public class BusinessLogicConfiguration {
    @Bean
    public BusinessMessagePayloadMapper messagePayloadMapper() {
        return Mappers.getMapper(BusinessMessagePayloadMapper.class);
    }

    @Bean
    public ActionExecutor actionExecutor() {
        return new ActionExecutorImpl();
    }
    // --------- Business' facades ---------------
    @Bean
    public StudentsFacade studentsFacade(
            @Qualifier(StudentCommand.FACTORY_BEAN_NAME) CommandsFactory<StudentCommand<?>> factory
    ) {
        return new StudentsFacadeImpl(factory, messagePayloadMapper());
    }

    @Bean
    public CoursesFacade coursesFacade(
            @Qualifier(CourseCommand.FACTORY_BEAN_NAME) CommandsFactory<CourseCommand<?>> factory
    ) {
        return new CoursesFacadeImpl(factory, messagePayloadMapper(), actionExecutor());
    }

    @Bean
    public AuthorityPersonFacade authorityPersonFacade(
            @Qualifier(AuthorityPersonCommand.FACTORY_BEAN_NAME) CommandsFactory<AuthorityPersonCommand<?>> factory
    ) {
        return new AuthorityPersonFacadeImpl(factory, messagePayloadMapper());
    }

    @Bean
    public FacultyFacade facultyFacade(
            @Qualifier(FacultyCommand.FACTORY_BEAN_NAME) CommandsFactory<FacultyCommand<?>> factory
    ) {
        return new FacultyFacadeImpl(factory, messagePayloadMapper());
    }

    @Bean
    public StudentsGroupFacade studentsGroupFacade(
            @Qualifier(StudentsGroupCommand.FACTORY_BEAN_NAME) CommandsFactory<StudentsGroupCommand<?>> factory
    ) {
        return new StudentsGroupFacadeImpl(factory, messagePayloadMapper());
    }

    @Bean
    public StudentProfileFacade studentProfileFacade(
            @Qualifier(StudentProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<StudentProfileCommand<?>> factory
    ) {
        return new StudentProfileFacadeImpl(factory, messagePayloadMapper());
    }

    @Bean
    public PrincipalProfileFacade principalProfileFacade(
            @Qualifier(PrincipalProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<PrincipalProfileCommand<?>> factory
    ) {
        return new PrincipalProfileFacadeImpl(factory, messagePayloadMapper());
    }

}
