package oleg.sopilnyak.test.service.command.factory.farm;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CommandsFactoriesFarmSpringTest.FactoryConfiguration.class)
class CommandsFactoriesFarmSpringTest {
    private static final String FACTORY_NAME = "CommandFactories-Farm";
    @MockBean
    PersistenceFacade persistenceFacade;
    @MockBean
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @Autowired
    CommandsFactory<CourseCommand<?>> factory1;
    @Spy
    @Autowired
    CommandsFactory<StudentCommand<?>> factory2;
    @Spy
    @Autowired
    CommandsFactory<FacultyCommand<?>> factory3;

    @Autowired
    CommandsFactoriesFarm<RootCommand<?>> farm;
    @Autowired
    ApplicationContext context;


    @Test
    void shouldBeValidComponents() {
        assertThat(context).isNotNull();
        assertThat(factory1).isNotNull();
        assertThat(factory2).isNotNull();
        assertThat(factory3).isNotNull();
        assertThat(farm).isNotNull();
        assertThat(farm.getName()).isEqualTo(FACTORY_NAME);
    }

    @Test
    void shouldGetTheCommandsFactories() {
        assertThat(factory1).isEqualTo(context.getBean(factory1.getClass()));
        assertThat(factory2).isEqualTo(context.getBean(factory2.getClass()));
        assertThat(factory3).isEqualTo(context.getBean(factory3.getClass()));
        assertThat(factory1).isEqualTo(farm.findCommandFactory(factory1.getName()).orElseThrow());
        assertThat(factory2).isEqualTo(farm.findCommandFactory(factory2.getName()).orElseThrow());
        assertThat(factory3).isEqualTo(farm.findCommandFactory(factory3.getName()).orElseThrow());
    }

    @Test
    void shouldGetTheCommandsById() {
        Collection<CommandsFactory<?>> factories = Set.of(factory1, factory2, factory3);
        Collection<String> commandsIds = factories.stream().flatMap(factory -> factory.commandIds().stream()).toList();

        assertThat(farm.getSize()).isEqualTo(commandsIds.size());

        commandsIds.forEach(commandId -> {
            RootCommand<?> command = farm.command(commandId);
            assertThat(command).isNotNull();
            CommandsFactory<?> factory = findCommandFactory(factories, commandId);
            assertThat(command).isEqualTo(factory.command(commandId));
        });
    }

    private CommandsFactory<?> findCommandFactory(Collection<CommandsFactory<?>> factories, String commandId) {
        return factories.stream().filter(factory -> factory.commandIds().contains(commandId)).findFirst().orElseThrow();
    }


    @Configuration
    @ComponentScan("oleg.sopilnyak.test.service.command.executable")
    static class FactoryConfiguration {
        @Bean
        public CommandsFactory<StudentCommand<?>> studentsCommandsFactory(final Collection<StudentCommand<?>> commands) {
            return new StudentCommandsFactory(commands);
        }

        @Bean
        public CommandsFactory<CourseCommand<?>> courseCommandsFactory(final Collection<CourseCommand<?>> commands) {
            return new CourseCommandsFactory(commands);
        }

        @Bean
        public CommandsFactory<FacultyCommand<?>> facultyCommandFactory(final Collection<FacultyCommand<?>> commands) {
            return new FacultyCommandsFactory(commands);
        }

        @Bean
        public <T extends RootCommand<?>> CommandsFactoriesFarm<T> commandsFactoriesFarm(final Collection<CommandsFactory<T>> factories) {
            return new CommandsFactoriesFarm<>(factories);
        }
    }
}