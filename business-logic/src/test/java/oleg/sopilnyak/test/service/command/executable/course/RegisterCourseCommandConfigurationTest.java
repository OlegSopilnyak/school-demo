package oleg.sopilnyak.test.service.command.executable.course;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.education.course.RegisterStudentToCourseCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class})
@TestPropertySource(properties = {"school.courses.maximum.rooms=20", "school.students.maximum.courses=15"})
@DirtiesContext
class RegisterCourseCommandConfigurationTest {
    static final String REGISTER_COMMAND_ID = "school::education::courses:register";
    @MockitoBean
    PersistenceFacade persistence;
    @MockitoBean
    BusinessMessagePayloadMapper payloadMapper;

    @Autowired(required = false)
    CommandsFactory<CourseCommand<?>> factory;

    @Test
    void shouldBuildCourseCommandsFactory() {
        assertThat(factory).isNotNull();
        assertThat(factory.getSize()).isGreaterThan(1);
    }

    @Test
    void shouldBuildRegisterStudentToCourseCommand(
            @Value("${school.courses.maximum.rooms:50}") final int maximumRooms,
            @Value("${school.students.maximum.courses:5}") final int coursesExceed
    ) {
        RootCommand<?> command = factory.command(REGISTER_COMMAND_ID);
        assertThat(command).isNotNull();
        if (command instanceof RegisterStudentToCourseCommand registerCommand) {
            assertThat(registerCommand.getMaximumRooms()).isEqualTo(maximumRooms).isEqualTo(20);
            assertThat(registerCommand.getCoursesExceed()).isEqualTo(coursesExceed).isEqualTo(15);
        } else {
            Assertions.fail("Factory command has wrong type :" + command);
        }
    }
}