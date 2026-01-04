package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateAuthorityPersonCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
class CreateOrUpdateAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    AuthorityPersonPersistenceFacade persistence;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    AuthorityPersonCommand command;

    @BeforeEach
    void setUp() {
        deleteEntities(AuthorityPersonEntity.class);
    }

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CreateEntity() {
        AuthorityPerson entity = makeCleanAuthorityPerson(1);
        Input<AuthorityPerson> input = (Input<AuthorityPerson>) Input.of(entity);
        Context<Optional<AuthorityPerson>> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        AuthorityPerson result = context.getResult().orElseThrow().orElseThrow();
        assertThat(context.<Long>getUndoParameter().value()).isEqualTo(result.getId());
        assertAuthorityPersonEquals(entity, result, false);
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        AuthorityPerson entity = persist();
        Long id = entity.getId();
        AuthorityPerson entityUpdated = payloadMapper.toPayload(entity);
        if (entityUpdated instanceof AuthorityPersonPayload updated) {
            updated.setFirstName(entity.getFirstName() + "-updated");
        }
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(entityUpdated));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<AuthorityPerson>getUndoParameter().value()).isEqualTo(entity);
        assertAuthorityPersonEquals(entityUpdated, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(entity);
        verify(payloadMapper, times(2)).toPayload(any(AuthorityPersonEntity.class));
        verify(persistence).save(entityUpdated);
    }

    @Test
    void shouldNotDoCommand_EntityNotFound() {
        Long id = 301L;
        AuthorityPerson entity = spy(makeTestAuthorityPerson(id));
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(entity, atLeastOnce()).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 302L;
        AuthorityPerson entity = spy(makeTestAuthorityPerson(id));
        doThrow(RuntimeException.class).when(persistence).findAuthorityPersonById(id);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity, atLeastOnce()).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        AuthorityPerson entity = spy(makeTestAuthorityPerson(null));
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(entity));

        assertThrows(UnexpectedRollbackException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity, atLeastOnce()).getId();
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        AuthorityPerson entity = spy(persist());
        Long id = entity.getId();
        doThrow(RuntimeException.class).when(persistence).save(any(AuthorityPerson.class));
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(entity));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity, atLeastOnce()).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPersonEntity.class));
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<AuthorityPerson>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        AuthorityPerson entity = spy(persist());
        Long id = entity.getId();
        assertThat(findPersonEntity(id)).isNotNull();
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        verify(command).executeUndo(context);
        verify(persistence).deleteAuthorityPerson(id);
        assertThat(persistence.findAuthorityPersonById(id)).isEmpty();
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        AuthorityPerson entity = spy(persist());
        Long id = entity.getId();
        assertAuthorityPersonEquals(entity, findPersonEntity(id));
        AuthorityPerson entityUpdated = payloadMapper.toPayload(entity);
        if (entityUpdated instanceof AuthorityPersonPayload updated) {
            updated.setFirstName(entity.getFirstName() + "-updated");
        }
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entityUpdated));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(entityUpdated);
        assertAuthorityPersonEquals(entityUpdated, findPersonEntity(entity.getId()));
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<AuthorityPerson>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<AuthorityPerson>> context = command.createContext();
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
        Context<Optional<AuthorityPerson>> context = command.createContext();
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
        Long id = 305L;
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteAuthorityPerson(id);

        assertThrows(UnexpectedRollbackException.class, () -> command.undoCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        AuthorityPerson entity = spy(persist());
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException("Don't want to create person. Bad Guy!")).when(persistence).save(entity);

        assertThrows(UnexpectedRollbackException.class, () -> command.undoCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    // private methods
    private AuthorityPersonEntity findPersonEntity(Long id) {
        return findEntity(AuthorityPersonEntity.class, id);
    }

    private AuthorityPerson persist() {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            AuthorityPerson source = makeCleanAuthorityPerson(0);
            AuthorityPersonEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(AuthorityPersonEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
        }
    }
}