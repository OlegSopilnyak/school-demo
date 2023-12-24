package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.facade.*;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.course.CourseCommandsConfiguration;
import oleg.sopilnyak.test.service.command.organization.*;
import oleg.sopilnyak.test.service.command.student.*;
import oleg.sopilnyak.test.service.facade.course.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.facade.organization.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.facade.profile.PersonProfileFacadeImpl;
import oleg.sopilnyak.test.service.facade.student.StudentsFacadeImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Set;

@Configuration
@Import({CourseCommandsConfiguration.class})
public class BusinessLogicConfiguration {
    private final PersistenceFacade persistenceFacade;

    public BusinessLogicConfiguration(final PersistenceFacade persistenceFacade) {
        this.persistenceFacade = persistenceFacade;
    }

    @Bean
    public SchoolCommandsFactory studentsCommandFactory() {
        return new SchoolCommandsFactory("students",
                Set.of(
                        new CreateOrUpdateStudentCommand(persistenceFacade),
                        new DeleteStudentCommand(persistenceFacade),
                        new FindStudentCommand(persistenceFacade),
                        new FindEnrolledStudentsCommand(persistenceFacade),
                        new FindNotEnrolledStudentsCommand(persistenceFacade),
                        new FindStudentCommand(persistenceFacade)
                )
        );
    }

    @Bean
    public StudentsFacade studentsFacade() {
        return new StudentsFacadeImpl(studentsCommandFactory());
    }


    @Bean
    public CoursesFacade coursesFacade(@Qualifier(CourseCommandsConfiguration.COMMANDS_FACTORY) CommandsFactory factory) {
        return new CoursesFacadeImpl(factory);
    }


    @Bean
    public SchoolCommandsFactory profilesCommandFactory() {
        return new SchoolCommandsFactory("person-profile",
                Set.of(
                )
        );
    }

    @Bean
    public PersonProfileFacade personProfileFacade() {
        return new PersonProfileFacadeImpl(profilesCommandFactory());
    }

    @Bean
    public SchoolCommandsFactory organizationCommandFactory() {
        return new SchoolCommandsFactory("organization",
                Set.of(
                        new CreateOrUpdateAuthorityPersonCommand(persistenceFacade),
                        new CreateOrUpdateFacultyCommand(persistenceFacade),
                        new CreateOrUpdateStudentsGroupCommand(persistenceFacade),
                        new DeleteAuthorityPersonCommand(persistenceFacade),
                        new DeleteFacultyCommand(persistenceFacade),
                        new DeleteStudentsGroupCommand(persistenceFacade),
                        new FindAllAuthorityPersonsCommand(persistenceFacade),
                        new FindAllFacultiesCommand(persistenceFacade),
                        new FindAllStudentsGroupsCommand(persistenceFacade),
                        new FindAuthorityPersonCommand(persistenceFacade),
                        new FindFacultyCommand(persistenceFacade),
                        new FindStudentsGroupCommand(persistenceFacade)
                )
        );
    }

    @Bean
    public OrganizationFacade organizationFacade() {
        return new OrganizationFacadeImpl(organizationCommandFactory());
    }

}
