package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.business.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.business.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.business.StudentsFacade;
import oleg.sopilnyak.test.school.common.business.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.service.command.configurations.CourseCommandsConfiguration;
import oleg.sopilnyak.test.service.command.configurations.OrganizationCommandsConfiguration;
import oleg.sopilnyak.test.service.command.configurations.ProfileCommandsConfiguration;
import oleg.sopilnyak.test.service.command.configurations.StudentsCommandConfiguration;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.*;
import oleg.sopilnyak.test.service.command.type.base.OrganizationCommand;
import oleg.sopilnyak.test.service.facade.impl.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({
        CourseCommandsConfiguration.class,
        StudentsCommandConfiguration.class,
        OrganizationCommandsConfiguration.class,
        ProfileCommandsConfiguration.class
})
@Configuration
public class BusinessLogicConfiguration {
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
    public PersonProfileFacade personProfileFacade(
            @Qualifier(ProfileCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new PersonProfileFacadeImpl<>(factory);
    }

//    @Bean
//    public OrganizationFacade organizationFacade(
//            @Qualifier(OrganizationCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
//    ) {
//        return new OrganizationFacadeImpl(factory);
//    }

    @Bean
    public AuthorityPersonFacade authorityPersonCommand(
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

}
