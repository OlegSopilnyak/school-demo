package oleg.sopilnyak.test.service.command.configurations;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.organization.*;
import oleg.sopilnyak.test.service.command.factory.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
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
     * Builder for profile commands factory instance
     *
     * @param commands injected by Spring list of commands having type OrganizationCommand
     * @return singleton
     * @see OrganizationCommand
     */
    @Bean(name = OrganizationCommand.FACTORY_BEAN_NAME)
    public <T> CommandsFactory<T> organizationCommandsFactory(final Collection<OrganizationCommand<T>> commands) {
        return new OrganizationCommandsFactory<>(commands);
    }
}
