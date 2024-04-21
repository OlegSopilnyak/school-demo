package oleg.sopilnyak.test.end2end.service.command.executable.profile;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class DeleteProfileCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistenceFacade;
    @SpyBean
    @Autowired
    PersonProfileRepository<PersonProfileEntity> personProfileRepository;
    @SpyBean
    @Autowired
    DeleteProfileCommand command;

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
    void shouldExecuteCommand() throws ProfileNotExistsException {
        long id = persistStudentProfile().getId();

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade, atLeastOnce()).findProfileById(id);
        verify(personProfileRepository, atLeastOnce()).findById(id);
        verify(persistenceFacade).deleteProfileById(id);
        verify(personProfileRepository).deleteById(id);

        assertThat(personProfileRepository.findById(id)).isEmpty();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommand_ProfileNotExists() throws ProfileNotExistsException {
        long id = 405L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(personProfileRepository).findById(id);
        verify(persistenceFacade, never()).deleteProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(ProfileNotExistsException.class);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommand_WrongIdType() {

        CommandResult<Boolean> result = command.execute("id");

        verify(persistenceFacade, never()).findProfileById(anyLong());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(ClassCastException.class);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommand_NullId() throws ProfileNotExistsException {

        CommandResult<Boolean> result = command.execute(null);

        verify(persistenceFacade).findProfileById(null);
        verify(personProfileRepository).findById(null);
        verify(persistenceFacade, never()).deleteProfileById(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommandDoCommand() throws ProfileNotExistsException {
        StudentProfile student = persistStudentProfile();
        long id = student.getId();
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.getResult()).contains(true);
        assertThat(context.getUndoParameter()).isEqualTo(student).isNotSameAs(student);
        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getException()).isNull();

        verify(command).executeDo(context);
        verify(persistenceFacade, atLeastOnce()).findProfileById(id);
        verify(personProfileRepository, atLeastOnce()).findById(id);
        verify(persistenceFacade).deleteProfileById(id);
        verify(personProfileRepository).deleteById(id);

        assertThat(persistenceFacade.findProfileById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommandDoCommand_NoProfile() throws ProfileNotExistsException {
        long id = 415L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.getResult()).contains(false);
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(ProfileNotExistsException.class);

        verify(command).executeDo(context);
        verify(persistenceFacade).findProfileById(id);
        verify(personProfileRepository).findById(id);
        verify(persistenceFacade, never()).deleteProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommandUndoCommand() {
        StudentProfile student = persistStudentProfile();
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(student);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
        verify(persistenceFacade).saveProfile(student);
        verify(personProfileRepository).saveAndFlush((PersonProfileEntity) student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteCommandUndo_WrongUndoCommandParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("input");

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);

        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).saveProfile(any(PersonProfile.class));
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