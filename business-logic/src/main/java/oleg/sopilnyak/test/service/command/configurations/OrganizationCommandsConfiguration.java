package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.base.OrganizationCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration for infrastructure-subsystem commands
 */
@Configuration
@AllArgsConstructor
public class OrganizationCommandsConfiguration {
    private final OrganizationPersistenceFacade persistenceFacade;

    @Bean
    public OrganizationCommand<Optional<AuthorityPerson>> createOrUpdateAuthorityPersonCommand() {
        return new CreateOrUpdateAuthorityPersonCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Optional<Faculty>> createOrUpdateFacultyCommand() {
        return new CreateOrUpdateFacultyCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Optional<StudentsGroup>> createOrUpdateStudentsGroupCommand() {
        return new CreateOrUpdateStudentsGroupCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Boolean> deleteAuthorityPersonCommand() {
        return new DeleteAuthorityPersonCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Boolean> deleteFacultyCommand() {
        return new DeleteFacultyCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Boolean> deleteStudentsGroupCommand() {
        return new DeleteStudentsGroupCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Set<AuthorityPerson>> findAllAuthorityPersonsCommand() {
        return new FindAllAuthorityPersonsCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Set<Faculty>> findAllFacultiesCommand() {
        return new FindAllFacultiesCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Set<StudentsGroup>> findAllStudentsGroupsCommand() {
        return new FindAllStudentsGroupsCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Optional<AuthorityPerson>> findAuthorityPersonCommand() {
        return new FindAuthorityPersonCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Optional<Faculty>> findFacultyCommand() {
        return new FindFacultyCommand(persistenceFacade);
    }

    @Bean
    public OrganizationCommand<Optional<StudentsGroup>> findStudentsGroupCommand() {
        return new FindStudentsGroupCommand(persistenceFacade);
    }
    /*
                        + new CreateOrUpdateAuthorityPersonCommand(persistenceFacade),
                        +new CreateOrUpdateFacultyCommand(persistenceFacade),
                        +new CreateOrUpdateStudentsGroupCommand(persistenceFacade),
                        +new DeleteAuthorityPersonCommand(persistenceFacade),
                        +new DeleteFacultyCommand(persistenceFacade),
                        +new DeleteStudentsGroupCommand(persistenceFacade),
                        +new FindAllAuthorityPersonsCommand(persistenceFacade),
                        +new FindAllFacultiesCommand(persistenceFacade),
                        +new FindAllStudentsGroupsCommand(persistenceFacade),
                        +new FindAuthorityPersonCommand(persistenceFacade),
                        +new FindFacultyCommand(persistenceFacade),
                        +new FindStudentsGroupCommand(persistenceFacade)
     */

    /**
     * Builder for authority person commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see AuthorityPersonCommand
     */
    @Bean(name = AuthorityPersonCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> authorityPersonCommandFactory(final Collection<AuthorityPersonCommand<T>> commands) {
        return new AuthorityPersonCommandsFactory<>(commands);
    }

    /**
     * Builder for faculty commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see FacultyCommand
     */
    @Bean(name = FacultyCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> facultyCommandFactory(final Collection<FacultyCommand<T>> commands) {
        return new FacultyCommandsFactory<>(commands);
    }

    /**
     * Builder for students group commands factory
     *
     * @param commands injected by Spring list of commands having type AuthorityPersonCommand
     * @return singleton
     * @see StudentsGroupCommand
     */
    @Bean(name = StudentsGroupCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> studentsGroupCommandFactory(final Collection<StudentsGroupCommand<T>> commands) {
        return new StudentsGroupCommandsFactory<>(commands);
    }
}
