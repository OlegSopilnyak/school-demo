package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.course.*;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration for courses-subsystem commands
 */
@Configuration
@AllArgsConstructor
public class CourseCommandsConfiguration {
    private final StudentCourseLinkPersistenceFacade persistenceFacade;

    @Bean
    public CourseCommand<Optional<Course>> createOrUpdateCourseCommand() {
        return new CreateOrUpdateCourseCommand(persistenceFacade);
    }

    @Bean
    public CourseCommand<Boolean> deleteCourseCommand() {
        return new DeleteCourseCommand(persistenceFacade);
    }

    @Bean
    public CourseCommand<Optional<Course>> findCourseCommand() {
        return new FindCourseCommand(persistenceFacade);
    }

    @Bean
    public CourseCommand<Set<Course>> findCoursesWithoutStudentsCommand() {
        return new FindCoursesWithoutStudentsCommand(persistenceFacade);
    }

    @Bean
    public CourseCommand<Set<Course>> findRegisteredCoursesCommand() {
        return new FindRegisteredCoursesCommand(persistenceFacade);
    }

    @Bean
    public CourseCommand<Boolean> registerStudentToCourseCommand(@Value("${school.courses.maximum.rooms:50}") final int maximumRooms,
                                                                 @Value("${school.students.maximum.courses:5}") final int coursesExceed) {
        return new RegisterStudentToCourseCommand(persistenceFacade, maximumRooms, coursesExceed);
    }

    @Bean
    public CourseCommand<Boolean> unRegisterStudentFromCourseCommand() {
        return new UnRegisterStudentFromCourseCommand(persistenceFacade);
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
}
