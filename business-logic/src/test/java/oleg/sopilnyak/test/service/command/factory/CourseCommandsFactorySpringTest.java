package oleg.sopilnyak.test.service.command.factory;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CourseCommandsFactorySpringTest.FactoryConfiguration.class)
class CourseCommandsFactorySpringTest {
    // command ids
    private static final String COURSE_FIND_BY_ID = "school::education::courses:find.By.Id";
    private static final String COURSE_FIND_REGISTERED_FOR = "school::education::courses:find.Registered.To.The.Student";
    private static final String COURSE_FIND_WITHOUT_STUDENTS = "school::education::courses:find.Without.Any.Student";
    private static final String COURSE_CREATE_OR_UPDATE = "school::education::courses:create.Or.Update";
    private static final String COURSE_DELETE = "school::education::courses:delete";
    private static final String COURSE_REGISTER = "school::education::courses:register";
    private static final String COURSE_UN_REGISTER = "school::education::courses:unregister";
    // factory meta-names
    private static final String FACTORY_NAME = "Courses";
    private static final String SPRING_NAME = "courseCommandsFactory";
    private Collection<String> commandsId;
    @MockitoBean
    CommandActionExecutor actionExecutor;
    @MockitoBean(name = "parallelCommandNestedCommandsExecutor")
    SchedulingTaskExecutor executor;
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoBean
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<CourseCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
                COURSE_FIND_BY_ID,
                COURSE_FIND_REGISTERED_FOR,
                COURSE_FIND_WITHOUT_STUDENTS,
                COURSE_CREATE_OR_UPDATE,
                COURSE_DELETE,
                COURSE_REGISTER,
                COURSE_UN_REGISTER
        );
    }

    @Test
    void shouldBeValidComponents() {
        assertThat(context).isNotNull();
        assertThat(factory).isNotNull();
    }

    @Test
    void shouldGetTheCommandsFactory() {
        assertThat(factory).isEqualTo(context.getBean(CommandsFactory.class)).isEqualTo(context.getBean(SPRING_NAME));
        assertThat(factory.getName()).isEqualTo(FACTORY_NAME);
    }

    @Test
    void shouldGetTheCommandsByCommandId() {
        assertThat(factory.getSize()).isEqualTo(commandsId.size());
        commandsId.forEach(cmdId->assertThat(factory.command(cmdId)).isNotNull());
    }

    @Configuration
    @ComponentScan("oleg.sopilnyak.test.service.command.executable")
    static class FactoryConfiguration {
        @Bean(name = SPRING_NAME)
        public CommandsFactory<CourseCommand<?>> commandsFactory(final Collection<CourseCommand<?>> commands) {
            return new CourseCommandsFactory(commands);
        }
    }
}