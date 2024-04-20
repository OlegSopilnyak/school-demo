package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.service.command.configurations.CourseCommandsConfiguration;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CourseCommandsConfiguration.class})
@TestPropertySource(properties = {"school.courses.maximum.rooms=20", "school.students.maximum.courses=15"})
class RegisterCourseCommandConfigurationTest<T> {
    final String REGISTER_COMMAND_ID = "course.register";
    @MockBean
    PersistenceFacade persistenceFacade;

    @Autowired(required = false)
    CommandsFactory<T> factory;

    @Test
    void shouldBuildCourseCommandsFactory() {
        assertThat(factory).isNotNull();
    }

    @Test
    void shouldBuildRegisterStudentToCourseCommand(@Value("${school.courses.maximum.rooms:50}") final int maximumRooms,
                                                   @Value("${school.students.maximum.courses:5}") final int coursesExceed) {
        SchoolCommand<?> command = factory.command(REGISTER_COMMAND_ID);
        assertThat(command).isNotNull();
        if (command instanceof RegisterStudentToCourseCommand registerCommand) {
            assertThat(registerCommand.getMaximumRooms()).isNotZero();
            assertThat(registerCommand.getMaximumRooms()).isEqualTo(maximumRooms);
            assertThat(registerCommand.getCoursesExceed()).isNotZero();
            assertThat(registerCommand.getCoursesExceed()).isEqualTo(coursesExceed);
        } else {
            Assertions.fail("Factory command has wrong type :" + command);
        }
    }
}