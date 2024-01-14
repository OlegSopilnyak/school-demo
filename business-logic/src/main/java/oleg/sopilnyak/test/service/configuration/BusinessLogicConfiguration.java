package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.facade.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.facade.StudentsFacade;
import oleg.sopilnyak.test.service.command.configurations.CourseCommandsConfiguration;
import oleg.sopilnyak.test.service.command.configurations.OrganizationCommandsConfiguration;
import oleg.sopilnyak.test.service.command.configurations.ProfileCommandsConfiguration;
import oleg.sopilnyak.test.service.command.configurations.StudentsCommandConfiguration;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.facade.course.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.PersonProfileFacadeImpl;
import oleg.sopilnyak.test.service.facade.student.StudentsFacadeImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(
        {
                CourseCommandsConfiguration.class,
                StudentsCommandConfiguration.class,
                OrganizationCommandsConfiguration.class,
                ProfileCommandsConfiguration.class
        })
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

    @Bean
    public OrganizationFacade organizationFacade(
            @Qualifier(OrganizationCommand.FACTORY_BEAN_NAME) CommandsFactory<?> factory
    ) {
        return new OrganizationFacadeImpl(factory);
    }

}
