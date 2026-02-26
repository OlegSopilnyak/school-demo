package oleg.sopilnyak.test.service.command.factory;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
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
@ContextConfiguration(classes = StudentCommandsFactorySpringTest.FactoryConfiguration.class)
class StudentCommandsFactorySpringTest {
    private static final String STUDENT_FIND_BY_ID = "school::education::students:find.By.Id";
    private static final String STUDENT_FIND_ENROLLED_TO = "school::education::students:find.Enrolled.To.The.Course";
    private static final String STUDENT_FIND_NOT_ENROLLED = "school::education::students:find.Not.Enrolled.To.Any.Course";
    private static final String STUDENT_CREATE_OR_UPDATE = "school::education::students:create.Or.Update";
    private static final String STUDENT_CREATE_NEW = "school::education::students:create.Macro";
    private static final String STUDENT_DELETE = "school::education::students:delete";
    private static final String STUDENT_DELETE_ALL = "school::education::students:delete.Macro";

    private static final String FACTORY_NAME = "Students";
    private static final String SPRING_NAME = "studentCommandsFactory";
    private Collection<String> commandsId;
    @MockitoBean
    CommandActionExecutor actionExecutor;
    @MockitoBean(name = "parallelCommandNestedCommandsExecutor")
    SchedulingTaskExecutor executor;
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoBean
    AuthenticationFacade authenticationFacade;
    @MockitoBean
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<StudentCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
                STUDENT_FIND_BY_ID,
                STUDENT_FIND_ENROLLED_TO,
                STUDENT_FIND_NOT_ENROLLED,
                STUDENT_CREATE_OR_UPDATE,
                STUDENT_CREATE_NEW,
                STUDENT_DELETE,
                STUDENT_DELETE_ALL
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
        commandsId.forEach(cmdId -> assertThat(factory.command(cmdId)).isNotNull());
    }

    @Configuration
    @ComponentScan("oleg.sopilnyak.test.service.command.executable")
    static class FactoryConfiguration {
        @Bean(name = SPRING_NAME)
        public CommandsFactory<StudentCommand<?>> commandsFactory(final Collection<StudentCommand<?>> commands) {
            return new StudentCommandsFactory(commands);
        }
    }
}