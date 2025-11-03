package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
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
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, DeleteAuthorityPersonCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class DeleteAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @Autowired
    EntityMapper entityMapper;
    @SpyBean
    @Autowired
    AuthorityPersonPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
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
    void shouldDoCommand_EntityExists() {
        AuthorityPerson entity = persist();
        Long id = entity.getId();
        assertThat(findPersonEntity(id)).isNotNull();
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).contains(true);
        assertAuthorityPersonEquals(entity, context.<AuthorityPerson>getUndoParameter().value(), false);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPersonEntity.class));
        verify(persistence).deleteAuthorityPerson(id);
        assertThat(persistence.findAuthorityPersonById(id)).isEmpty();
    }

    @Test
    void shouldNotDoCommand_EntityNotExists() {
        long id = 315L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext(Input.of("id"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findAuthorityPersonById(anyLong());
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Boolean> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value null");
        verify(command).executeDo(context);
        verify(persistence, never()).findAuthorityPersonById(anyLong());
    }

    @Test
    void shouldNotDoCommand_DeleteExceptionThrown() throws ProfileNotFoundException {
        Long id = persist().getId();
        assertThat(findPersonEntity(id)).isNotNull();
        doThrow(new UnsupportedOperationException()).when(persistence).deleteAuthorityPerson(id);
        Context<Boolean> context = command.createContext(Input.of(id));

        assertThrows(UnexpectedRollbackException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPersonEntity.class));
        verify(persistence).deleteAuthorityPerson(id);
        assertThat(findPersonEntity(id)).isNotNull();
    }

    @Test
    void shouldUndoCommand_UndoParameterIsCorrect() {
        AuthorityPerson entity = spy(makeCleanAuthorityPerson(1));
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldUndoCommand_UndoParameterWrongType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of("person"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'AuthorityPerson' value:[person]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    void shouldUndoCommand_UndoParameterIsNull() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.empty());
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value null");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        AuthorityPerson entity = spy(makeCleanAuthorityPerson(1));
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        doThrow(new UnsupportedOperationException()).when(persistence).save(entity);

        assertThrows(UnexpectedRollbackException.class, () -> command.undoCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    // private methods
    private AuthorityPersonEntity findPersonEntity(Long id) {
        return findEntity(AuthorityPersonEntity.class, id);
    }

    private AuthorityPerson persist() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
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
            em.close();
        }
    }
}