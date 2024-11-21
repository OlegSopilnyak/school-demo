package oleg.sopilnyak.test.service.command.configurations;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchoolCommandsConfiguration.class)
class SchoolCommandsConfigurationTest {
    @MockBean
    PersistenceFacade persistenceFacade;
    @MockBean
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    @Qualifier("studentCommandsFactory")
    CommandsFactory<StudentCommand<?>> studentsCommandsFactory;
    @Autowired
    @Qualifier("courseCommandsFactory")
    CommandsFactory<CourseCommand<?>> courseCommandsFactory;
    @Autowired
    @Qualifier("authorityCommandsFactory")
    CommandsFactory<AuthorityPersonCommand<?>> authorityPersonCommandFactory;
    @Autowired
    @Qualifier("facultyCommandsFactory")
    CommandsFactory<FacultyCommand<?>> facultyCommandFactory;
    @Autowired
    @Qualifier("groupCommandsFactory")
    CommandsFactory<StudentsGroupCommand<?>> studentsGroupCommandFactory;
    @Autowired
    @Qualifier("studentProfileCommandsFactory")
    CommandsFactory<StudentProfileCommand<?>> studentProfileCommandsFactory;
    @Autowired
    @Qualifier("principalProfileCommandsFactory")
    CommandsFactory<PrincipalProfileCommand<?>> principalProfileCommandsFactory;
    @Autowired
    @Qualifier("commandFactoriesFarm")
    CommandsFactory<RootCommand<?>> farm;
    @Autowired
    ApplicationContext context;

    @Test
    void shouldBeValidComponents() {
        assertThat(context).isNotNull();
        assertThat(studentsCommandsFactory).isNotNull();
        assertThat(courseCommandsFactory).isNotNull();
        assertThat(authorityPersonCommandFactory).isNotNull();
        assertThat(facultyCommandFactory).isNotNull();
        assertThat(studentsGroupCommandFactory).isNotNull();
        assertThat(studentProfileCommandsFactory).isNotNull();
        assertThat(principalProfileCommandsFactory).isNotNull();
        assertThat(farm).isNotNull();
    }

    @Test
    void shouldHaveCommandsStudentsCommandsFactory() {
        assertThat(studentsCommandsFactory.getSize()).isGreaterThan(1);
        var factory = studentsCommandsFactory;
        factory.commandIds().forEach(cmdId -> {
            StudentCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveCommandsCourseCommandsFactory() {
        assertThat(courseCommandsFactory.getSize()).isGreaterThan(1);
        var factory = courseCommandsFactory;
        factory.commandIds().forEach(cmdId -> {
            CourseCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveCommandsAuthorityPersonCommandFactory() {
        assertThat(authorityPersonCommandFactory.getSize()).isGreaterThan(1);
        var factory = authorityPersonCommandFactory;
        factory.commandIds().forEach(cmdId -> {
            AuthorityPersonCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveCommandsFacultyCommandFactory() {
        assertThat(facultyCommandFactory.getSize()).isGreaterThan(1);
        var factory = facultyCommandFactory;
        factory.commandIds().forEach(cmdId -> {
            FacultyCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveCommandsStudentsGroupCommandFactory() {
        assertThat(studentsGroupCommandFactory.getSize()).isGreaterThan(1);
        var factory = studentsGroupCommandFactory;
        factory.commandIds().forEach(cmdId -> {
            StudentsGroupCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveCommandsStudentProfileCommandsFactory() {
        assertThat(studentProfileCommandsFactory.getSize()).isGreaterThan(1);
        var factory = studentProfileCommandsFactory;
        factory.commandIds().forEach(cmdId -> {
            StudentProfileCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveCommandsPrincipalProfileCommandsFactory() {
        assertThat(principalProfileCommandsFactory.getSize()).isGreaterThan(1);
        var factory = principalProfileCommandsFactory;
        factory.commandIds().forEach(cmdId -> {
            PrincipalProfileCommand<?> command = factory.command(cmdId);
            assertThat(command).isEqualTo(farm.command(cmdId));
        });
    }

    @Test
    void shouldHaveFactoriesCommandsInTheFarm() {
        CommandsFactoriesFarm<RootCommand<?>> factoriesFarm = (CommandsFactoriesFarm<RootCommand<?>>) farm;
        assertThat(factoriesFarm.findCommandFactory(studentsCommandsFactory.getName()).orElseThrow()).isEqualTo(studentsCommandsFactory);
        assertThat(factoriesFarm.findCommandFactory(courseCommandsFactory.getName()).orElseThrow()).isEqualTo(courseCommandsFactory);
        assertThat(factoriesFarm.findCommandFactory(authorityPersonCommandFactory.getName()).orElseThrow()).isEqualTo(authorityPersonCommandFactory);
        assertThat(factoriesFarm.findCommandFactory(facultyCommandFactory.getName()).orElseThrow()).isEqualTo(facultyCommandFactory);
        assertThat(factoriesFarm.findCommandFactory(studentsGroupCommandFactory.getName()).orElseThrow()).isEqualTo(studentsGroupCommandFactory);
        assertThat(factoriesFarm.findCommandFactory(studentProfileCommandsFactory.getName()).orElseThrow()).isEqualTo(studentProfileCommandsFactory);
        assertThat(factoriesFarm.findCommandFactory(principalProfileCommandsFactory.getName()).orElseThrow()).isEqualTo(principalProfileCommandsFactory);
    }

    @Test
    void shouldHaveCommandsInTheFarm() {
        List<CommandsFactory<? extends RootCommand<?>>> factories = List.of(
                studentsCommandsFactory,
                courseCommandsFactory,
                authorityPersonCommandFactory,
                facultyCommandFactory,
                studentsGroupCommandFactory,
                studentProfileCommandsFactory,
                principalProfileCommandsFactory
        );

        int commandsCount = factories.stream().mapToInt(CommandsFactory::getSize).sum();
        assertThat(commandsCount).isEqualTo(farm.getSize());

        factories.forEach(factory -> factory.commandIds().forEach(cmdId -> {
                    RootCommand<?> command = factory.command(cmdId);
                    assertThat(command).isEqualTo(farm.command(cmdId));
                })
        );
    }
}