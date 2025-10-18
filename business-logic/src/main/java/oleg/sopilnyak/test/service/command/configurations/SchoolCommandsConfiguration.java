package oleg.sopilnyak.test.service.command.configurations;

import com.fasterxml.jackson.databind.Module;
import java.util.Collection;
import java.util.Deque;
import lombok.RequiredArgsConstructor;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.impl.ActionExecutorImpl;
import oleg.sopilnyak.test.service.facade.impl.CommandThroughMessageServiceLocalImpl;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for courses-subsystem commands
 */
@Configuration
@ComponentScan("oleg.sopilnyak.test.service.command.executable")
@RequiredArgsConstructor
public class SchoolCommandsConfiguration {
    /**
     * Executor for school-commands
     *
     * @return the instance
     */
    @Bean
    public ActionExecutor actionExecutor() {
        return new ActionExecutorImpl(commandThroughMessageService());
    }

    /**
     * Local implementation of command messages deliverer for action-executor
     *
     * @return implementation based on local queues
     * @see ActionExecutorImpl#processActionCommand(BaseCommandMessage)
     */
    @Bean
    @Profile("!AMQP")
    public CommandThroughMessageService commandThroughMessageService() {
        return new CommandThroughMessageServiceLocalImpl();
    }

    /**
     * TaskExecutor for all parallel commands
     *
     * @param maxPoolSize maximum size of threads-pool
     * @return ready to use executor
     * @see SchedulingTaskExecutor
     * @see oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand#executeNested(Deque, Context.StateChangedListener)
     * @see oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand#rollbackNested(Deque)
     */
    @Bean
    public SchedulingTaskExecutor parallelCommandNestedCommandsExecutor(
            @Value("${school.parallel.max.pool.size:100}") final int maxPoolSize
    ) {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        final int operationalPoolSize = Math.max(maxPoolSize, Runtime.getRuntime().availableProcessors());
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(operationalPoolSize);
        executor.setThreadNamePrefix("ParallelCommandThread-");
        executor.initialize();
        return executor;
    }
// ----------------- Commands factories ----------------

    /**
     * Builder for student commands factory instance
     *
     * @param commands injected by Spring list of commands having type StudentCommand
     * @return singleton
     * @see StudentCommand
     */
    @Bean(name = StudentCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<StudentCommand<?>> studentsCommandsFactory(final Collection<StudentCommand<?>> commands) {
        return new StudentCommandsFactory(commands);
    }

    /**
     * Builder for course commands factory instance
     *
     * @param commands injected by Spring list of commands having type CourseCommand
     * @return singleton
     * @see CourseCommand
     */
    @Bean(name = CourseCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<CourseCommand<?>> courseCommandsFactory(final Collection<CourseCommand<?>> commands) {
        return new CourseCommandsFactory(commands);
    }

    /**
     * Builder for authority person commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see AuthorityPersonCommand
     */
    @Bean(name = AuthorityPersonCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<AuthorityPersonCommand<?>> authorityPersonCommandFactory(final Collection<AuthorityPersonCommand<?>> commands) {
        return new AuthorityPersonCommandsFactory(commands);
    }

    /**
     * Builder for faculty commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see FacultyCommand
     */
    @Bean(name = FacultyCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<FacultyCommand<?>> facultyCommandFactory(final Collection<FacultyCommand<?>> commands) {
        return new FacultyCommandsFactory(commands);
    }

    /**
     * Builder for students group commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see StudentsGroupCommand
     */
    @Bean(name = StudentsGroupCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<StudentsGroupCommand<?>> studentsGroupCommandFactory(final Collection<StudentsGroupCommand<?>> commands) {
        return new StudentsGroupCommandsFactory(commands);
    }

    /**
     * Builder for student profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type ProfileCommand
     * @return singleton
     * @see StudentProfileCommand
     */
    @Bean(name = StudentProfileCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<StudentProfileCommand<?>> studentProfileCommandsFactory(final Collection<StudentProfileCommand<?>> commands) {
        return new StudentProfileCommandsFactory(commands);
    }

    /**
     * Builder for principal profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type ProfileCommand
     * @return singleton
     * @see PrincipalProfileCommand
     */
    @Bean(name = PrincipalProfileCommand.FACTORY_BEAN_NAME)
    public CommandsFactory<PrincipalProfileCommand<?>> principalProfileCommandsFactory(final Collection<PrincipalProfileCommand<?>> commands) {
        return new PrincipalProfileCommandsFactory(commands);
    }

// ----------------- Commands Factories Farm ----------------

    /**
     * Builder for commands factories farm instance
     *
     * @param factories collection of commands factories
     * @return singleton
     * @see CommandsFactory
     */
    @Bean(name = CommandsFactoriesFarm.FARM_BEAN_NAME)
    public <T extends RootCommand<?>> CommandsFactoriesFarm<T> commandsFactoriesFarm(final Collection<CommandsFactory<T>> factories) {
        return new CommandsFactoriesFarm<>(factories);
    }

    /**
     * Module for ObjectMapper
     *
     * @param context Spring Application Context
     * @return the module
     * @see com.fasterxml.jackson.databind.ObjectMapper
     * @see Module
     */
    @Bean(name = "jsonContextModule")
    public <T extends RootCommand<?>> Module createConetxtModule(ApplicationContext context, CommandsFactoriesFarm<T> farm) {
        return new JsonContextModule(context, farm);
    }
}
