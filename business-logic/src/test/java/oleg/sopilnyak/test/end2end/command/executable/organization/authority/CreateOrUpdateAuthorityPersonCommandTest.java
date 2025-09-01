package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateAuthorityPersonCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateOrUpdateAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    AuthorityPersonPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    AuthorityPersonCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        AuthorityPerson entity = spy(makeTestAuthorityPerson(null));
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(entity));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity, atLeastOnce()).getId();
        verify(persistence).save(entity);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongState() {
        Context<Optional<AuthorityPerson>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_CreateEntity() {
        AuthorityPerson entity = spy(persist());
        Long id = entity.getId();
        assertThat(persistence.findAuthorityPersonById(id)).isPresent();
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteAuthorityPerson(id);
        assertThat(persistence.findAuthorityPersonById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_UpdateEntity() {
        AuthorityPerson entity = spy(persist());
        Long id = entity.getId();
        assertAuthorityPersonEquals(entity, persistence.findAuthorityPersonById(id).orElseThrow());
        AuthorityPerson entityUpdated = payloadMapper.toPayload(entity);
        if (entityUpdated instanceof AuthorityPersonPayload updated) {
            updated.setFirstName(entity.getFirstName() + "-updated");
        }
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entityUpdated));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(entityUpdated);
        assertAuthorityPersonEquals(entityUpdated, persistence.findAuthorityPersonById(entity.getId()).orElseThrow());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<AuthorityPerson>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Wrong input parameter value null");
        verify(command).executeUndo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_DeleteEntityExceptionThrown() throws ProfileNotFoundException {
        Long id = 305L;
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        doThrow(new RuntimeException()).when(persistence).deleteAuthorityPerson(id);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        AuthorityPerson entity = spy(persist());
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).save(entity);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    // private methods
    private AuthorityPerson persist() {
        try {
            AuthorityPerson source = makeCleanAuthorityPerson(0);
            AuthorityPerson entity = persistence.save(source).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<AuthorityPerson> person = persistence.findAuthorityPersonById(id);
            assertAuthorityPersonEquals(person.orElseThrow(), source, false);
            assertThat(person).contains(entity);
            return payloadMapper.toPayload(entity);
        } finally {
            reset(persistence, payloadMapper);
        }
    }
}