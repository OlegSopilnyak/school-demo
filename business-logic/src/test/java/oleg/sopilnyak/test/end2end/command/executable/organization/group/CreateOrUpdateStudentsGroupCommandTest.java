package oleg.sopilnyak.test.end2end.command.executable.organization.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateStudentsGroupCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
class CreateOrUpdateStudentsGroupCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    StudentsGroupPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    StudentsGroupCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(CourseEntity.class);
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentsGroupEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CreateEntity() {
        StudentsGroup entity = makeCleanStudentsGroup(1);
        Input<StudentsGroup> input = (Input<StudentsGroup>) Input.of(entity);
        Context<Optional<StudentsGroup>> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        StudentsGroup result = context.getResult().orElseThrow().orElseThrow();
        assertStudentsGroupEquals(entity, result, false);
        assertThat(context.getUndoParameter().value()).isEqualTo(result.getId());
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        StudentsGroup entity = persist();
        Long id = entity.getId();
        StudentsGroup entityUpdated = payloadMapper.toPayload(entity);
        if (entityUpdated instanceof StudentsGroupPayload updated) {
            updated.setName(entity.getName() + "-updated");
        }
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entityUpdated));
        reset(payloadMapper);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        StudentsGroup undo = context.<StudentsGroup>getUndoParameter().value();
        assertStudentsGroupEquals(entity, undo, true);
        StudentsGroup result = context.getResult().orElseThrow().orElseThrow();
        assertStudentsGroupEquals(entityUpdated, result, true);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, times(2)).toPayload(any(StudentsGroupEntity.class));
        verify(persistence).save(entityUpdated);
    }

    @Test
    void shouldNotDoCommand_StudentsGroupNotFound() {
        Long id = 501L;
        StudentsGroup entity = makeTestStudentsGroup(id);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentsGroupNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, never()).toPayload(any(StudentsGroup.class));
        verify(persistence, never()).save(any(StudentsGroup.class));
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        StudentsGroup entity = persist();
        Long id = entity.getId();
        doThrow(RuntimeException.class).when(persistence).findStudentsGroupById(id);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, never()).toPayload(any(StudentsGroup.class));
        verify(persistence, never()).save(any(StudentsGroup.class));
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        StudentsGroup entity = makeTestStudentsGroup(null);
        Input<StudentsGroup> input = (Input<StudentsGroup>) Input.of(entity);
        Context<Optional<StudentsGroup>> context = command.createContext(input);

        doThrow(RuntimeException.class).when(persistence).save(input.value());

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        StudentsGroup entity = persist();
        Long id = entity.getId();
        String errorMessage = "Cannot save students group";
        Exception expectedException = new RuntimeException(errorMessage);
        doThrow(expectedException).when(persistence).save(entity);
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(entity));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(expectedException);
        assertThat(context.getException().getMessage()).isSameAs(errorMessage);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper).toPayload(any(StudentsGroupEntity.class));
        verify(persistence, times(2)).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<StudentsGroup>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<StudentsGroup>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        Long id = 500L;
        StudentsGroup entity = makeTestStudentsGroup(null);
        if (entity instanceof FakeStudentsGroup fake) {
            fake.setStudents(List.of());
            fake.setLeader(null);
            id = persist(fake).getId();
        }
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        StudentsGroup entity = persist();
        Context<Optional<StudentsGroup>> context = command.createContext();
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
        Context<Optional<StudentsGroup>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<StudentsGroup>> context = command.createContext();
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
        Context<Optional<StudentsGroup>> context = command.createContext();
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
        StudentsGroup entity = persist();
        Long id = entity.getId();
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteStudentsGroup(id);

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        StudentsGroup entity = persist();
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).save(entity);

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    // private methods
    private StudentsGroup persist() {
        return persist(makeCleanStudentsGroup(0));
    }

    private StudentsGroup persist(StudentsGroup source) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            StudentsGroupEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentsGroupEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
        }
    }
}