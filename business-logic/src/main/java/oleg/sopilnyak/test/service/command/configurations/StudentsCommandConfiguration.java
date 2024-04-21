package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.student.*;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration for students-subsystem commands
 */
@Configuration
@AllArgsConstructor
public class StudentsCommandConfiguration {
    private final StudentCourseLinkPersistenceFacade persistenceFacade;

    @Bean
    public StudentCommand<Optional<Student>> findStudentCommand() {
        return new FindStudentCommand(persistenceFacade);
    }

    @Bean
    public StudentCommand<Optional<Student>> createOrUpdateStudentCommand() {
        return new CreateOrUpdateStudentCommand(persistenceFacade);
    }

    @Bean
    public StudentCommand<Boolean> deleteStudentCommand() {
        return new DeleteStudentCommand(persistenceFacade);
    }

    @Bean
    public StudentCommand<Set<Student>> findEnrolledStudentsCommand() {
        return new FindEnrolledStudentsCommand(persistenceFacade);
    }

    @Bean
    public StudentCommand<Set<Student>> findNotEnrolledStudentsCommand() {
        return new FindNotEnrolledStudentsCommand(persistenceFacade);
    }

    /**
     * Builder for profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type StudentCommand
     * @return singleton
     * @see StudentCommand
     */
    @Bean(name = StudentCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> studentsCommandsFactory(final Collection<StudentCommand<T>> commands) {
        return new StudentCommandsFactory<>(commands);
    }
}
