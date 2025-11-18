package oleg.sopilnyak.test.end2end.command.executable.organization.faculty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateFacultyCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class CreateOrUpdateFacultyCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    FacultyPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    FacultyCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(FacultyEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CreateEntity() {
        Faculty entity = makeCleanFacultyNoDean(1);
        Input<Faculty> input = (Input<Faculty>) Input.of(entity);
        Context<Optional<Faculty>> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Faculty result = context.getResult().orElseThrow().orElseThrow();
        assertFacultyEquals(entity, result, false);
        assertThat(context.getUndoParameter().value()).isEqualTo(result.getId());
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        Faculty entity = persist();
        Long id = entity.getId();
        Faculty entityUpdated = payloadMapper.toPayload(entity);
        if (entityUpdated instanceof FacultyPayload updated) {
            updated.setName(entity.getName() + "-updated");
        }
        Context<Optional<Faculty>> context = command.createContext(Input.of(entityUpdated));
        reset(payloadMapper);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Faculty undo = context.<Faculty>getUndoParameter().value();
        assertFacultyEquals(entity, undo, true);
        Faculty result = context.getResult().orElseThrow().orElseThrow();
        assertFacultyEquals(entityUpdated, result, true);
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, times(2)).toPayload(any(FacultyEntity.class));
        verify(persistence).save(entityUpdated);
    }

    @Test
    void shouldNotDoCommand_EntityNotFound() {
        Long id = 401L;
        Faculty entity = spy(makeTestFaculty(id));
        Context<Optional<Faculty>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(FacultyNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Faculty with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, never()).toPayload(any(Faculty.class));
        verify(persistence, never()).save(any(Faculty.class));
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Faculty entity = persist();
        Long id = entity.getId();
        Context<Optional<Faculty>> context = command.createContext(Input.of(entity));

        doThrow(RuntimeException.class).when(persistence).findFacultyById(id);
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, never()).toPayload(any(Faculty.class));
        verify(persistence, never()).save(any(Faculty.class));
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        Faculty entity = spy(makeCleanFacultyNoDean(1));
        Context<Optional<Faculty>> context = command.createContext(Input.of(entity));
        doThrow(RuntimeException.class).when(persistence).save(entity);

        Exception e = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(e).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        Faculty entity = persist();
        Long id = entity.getId();
        Context<Optional<Faculty>> context = command.createContext(Input.of(entity));

        doThrow(RuntimeException.class).when(persistence).save(entity);
        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
        verify(payloadMapper).toPayload(any(FacultyEntity.class));
        verify(persistence, times(2)).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Faculty>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<Faculty>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        Faculty entity = persist();
        Long id = entity.getId();
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteFaculty(id);
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        Faculty entity = persist();
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<Faculty>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage())
                .isEqualTo("Wrong input parameter value (cannot be null or empty).");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("param"));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Long' value:[param]");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteEntityExceptionThrown() throws ProfileNotFoundException {
        Faculty entity = persist();
        Long id = entity.getId();
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteFaculty(id);

        Exception e = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(e).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteFaculty(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        Faculty entity = persist();
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).save(entity);

        Exception e = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(e).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    // private methods
    private Faculty persist() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            Faculty source = makeCleanFacultyNoDean(0);
            FacultyEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(FacultyEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
            em.close();
        }
    }
}