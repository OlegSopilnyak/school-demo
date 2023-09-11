package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.StudentsFacade;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.course.*;
import oleg.sopilnyak.test.service.command.student.*;
import oleg.sopilnyak.test.service.facade.course.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.facade.student.StudentsFacadeImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class BusinessLogicConfiguration {
    @Autowired
    private PersistenceFacade persistenceFacade;
    @Value("${school.courses.maximum.rooms:50}")
    private int maximumRooms;
    @Value("${school.students.maximum.courses:5}")
    private int coursesExceed;

    @Bean
    public SchoolCommandsFactory studentsCommandFactory() {
        return new SchoolCommandsFactory(
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
    public SchoolCommandsFactory coursesCommandFactory() {
        return new SchoolCommandsFactory(
                Set.of(
                        new CreateOrUpdateCourseCommand(persistenceFacade),
                        new DeleteCourseCommand(persistenceFacade),
                        new FindCourseCommand(persistenceFacade),
                        new FindCoursesWithoutStudentsCommand(persistenceFacade),
                        new FindRegisteredCoursesCommand(persistenceFacade),
                        new RegisterStudentToCourseCommand(persistenceFacade, maximumRooms, coursesExceed),
                        new UnRegisterStudentFromCourseCommand(persistenceFacade)
                )
        );
    }

    @Bean
    public CoursesFacade coursesFacade() {
        return new CoursesFacadeImpl(coursesCommandFactory());
    }
}
