package oleg.sopilnyak.test.service.command.configurations;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.executable.course.*;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
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
public class CourseCommandsConfiguration {
    public static final String COMMANDS_FACTORY = "courseCommandsFactory";
    private final PersistenceFacade persistenceFacade;

    public CourseCommandsConfiguration(final PersistenceFacade persistenceFacade) {
        this.persistenceFacade = persistenceFacade;
    }

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
    @Bean(name = COMMANDS_FACTORY)
    public <T> CommandsFactory<T> courseCommandsFactory(final Collection<CourseCommand<T>> commands) {
        return new CourseCommandsFactory<>(commands);
    }
}
