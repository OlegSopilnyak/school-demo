package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.StudentsFacade;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.course.RegisterStudentToCourseCommand;
import oleg.sopilnyak.test.service.command.course.*;
import oleg.sopilnyak.test.service.command.student.*;
import oleg.sopilnyak.test.service.facade.course.CourseCommandsFacade;
import oleg.sopilnyak.test.service.facade.course.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.facade.student.StudentCommandFacade;
import oleg.sopilnyak.test.service.facade.student.StudentsFacadeImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

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
                Map.of(
                        StudentCommandFacade.FIND_BY_ID, new FindStudentCommand(persistenceFacade),
                        StudentCommandFacade.FIND_ENROLLED, new FindEnrolledStudentsCommand(persistenceFacade),
                        StudentCommandFacade.FIND_NOT_ENROLLED, new FindNotEnrolledStudentsCommand(persistenceFacade),
                        StudentCommandFacade.CREATE_OR_UPDATE, new CreateOrUpdateStudentCommand(persistenceFacade),
                        StudentCommandFacade.DELETE, new DeleteStudentCommand(persistenceFacade)
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
                Map.of(
                        CourseCommandsFacade.FIND_BY_ID, new FindCourseCommand(persistenceFacade),
                        CourseCommandsFacade.FIND_REGISTERED, new FindRegisteredCoursesCommand(persistenceFacade),
                        CourseCommandsFacade.FIND_NOT_REGISTERED, new FindCoursesWithoutStudentsCommand(persistenceFacade),
                        CourseCommandsFacade.CREATE_OR_UPDATE, new CreateOrUpdateCourseCommand(persistenceFacade),
                        CourseCommandsFacade.DELETE, new DeleteCourseCommand(persistenceFacade),
                        CourseCommandsFacade.REGISTER, new RegisterStudentToCourseCommand(persistenceFacade, maximumRooms, coursesExceed),
                        CourseCommandsFacade.UN_REGISTER, new UnRegisterStudentFromCourseCommand(persistenceFacade)
                )
        );
    }

    @Bean
    public CoursesFacade coursesFacade() {
        return new CoursesFacadeImpl(coursesCommandFactory());
    }
}
