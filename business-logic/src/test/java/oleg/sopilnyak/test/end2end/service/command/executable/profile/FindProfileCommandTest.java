package oleg.sopilnyak.test.end2end.service.command.executable.profile;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FindProfileCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistenceFacade;
    @SpyBean
    @Autowired
    PersonProfileRepository<PersonProfileEntity> personProfileRepository;
    @SpyBean
    @Autowired
    FindProfileCommand command;

    @AfterEach
    void tearDown() {
        reset(command);
        reset(persistenceFacade);
        reset(personProfileRepository);
    }

    @Test
    void allPartsShouldBeInitiated() {
        assertThat(command).isNotNull();
        assertThat(persistenceFacade).isNotNull();
        assertThat(personProfileRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommand() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();

        CommandResult<Optional<PersonProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(personProfileRepository).findById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent().contains(Optional.of(profile));
        assertThat(result.getException()).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommand_NoProfile() {
        Long id = 413L;

        CommandResult<Optional<PersonProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(personProfileRepository).findById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommandDoCommand() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        Context<Optional<PersonProfile>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.getResult()).isNotEmpty().contains(Optional.of(profile));
        assertThat(context.getState()).isEqualTo(DONE);
        assertThat(context.getException()).isNull();

        verify(command).executeDo(context);
        verify(persistenceFacade).findProfileById(id);
        verify(personProfileRepository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommandDoCommand_NotFound() {
        Long id = 405L;
        Context<Optional<PersonProfile>> context = command.createContext(id);

        command.doCommand(context);

        assertThat((Optional<PersonProfile>)context.getResult().orElse(Optional.empty())).isEmpty();
        assertThat(context.getState()).isEqualTo(DONE);
        assertThat(context.getException()).isNull();

        verify(command).executeDo(context);
        verify(persistenceFacade).findProfileById(id);
        verify(personProfileRepository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommandDoCommand_WrongParameterType() {
        Long id = 406L;
        Context<Optional<PersonProfile>> context = command.createContext("" + id);

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.getState()).isEqualTo(FAIL);
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);

        verify(command).executeDo(context);
        verify(persistenceFacade, never()).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommandUndoCommand() {
        Long id = 414L;
        Context<Optional<PersonProfile>> context = command.createContext(id);
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }

    // private methods
    private StudentProfile persistStudentProfile() {
        StudentProfile student = makeStudentProfile(null);
        StudentProfile entity = persistenceFacade.save(student).orElse(null);
        assertThat(entity).isNotNull();
        long id = entity.getId();
        assertThat(personProfileRepository.findById(id)).isNotEmpty();
        reset(persistenceFacade, personProfileRepository);
        return entity;
    }
}