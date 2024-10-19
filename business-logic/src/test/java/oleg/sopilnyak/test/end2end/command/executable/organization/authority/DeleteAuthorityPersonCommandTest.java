package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.AuthorityPersonEntity;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonIsNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, DeleteAuthorityPersonCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class DeleteAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    AuthorityPersonPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    DeleteAuthorityPersonCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_EntityExists() {
        AuthorityPerson entity = persist();
        Long id = entity.getId();
        assertThat(persistence.findAuthorityPersonById(id)).isPresent();
        Context<Boolean> context = command.createContext(id);
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).contains(true);
        assertAuthorityPersonEquals(entity, context.getUndoParameter(), false);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPersonEntity.class));
        verify(persistence).deleteAuthorityPerson(id);
        assertThat(persistence.findAuthorityPersonById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_EntityNotExists() {
        long id = 315L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonIsNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper, never()).toPayload(any(AuthorityPerson.class));
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext("id");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findAuthorityPersonById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_DeleteExceptionThrown() throws ProfileIsNotFoundException {
        AuthorityPerson entity = persist();
        Long id = entity.getId();
        assertThat(persistence.findAuthorityPersonById(id)).isPresent();
        reset(persistence);
        doThrow(new UnsupportedOperationException()).when(persistence).deleteAuthorityPerson(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(payloadMapper).toPayload(any(AuthorityPersonEntity.class));
        verify(persistence).deleteAuthorityPerson(id);
        assertThat(persistence.findAuthorityPersonById(id)).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_UndoParameterIsCorrect() {
        AuthorityPerson entity = spy(makeCleanAuthorityPerson(1));
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_UndoParameterWrongType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("person");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a  'AuthorityPerson' value:[person]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_UndoParameterIsNull() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value null");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_ExceptionThrown() {
        AuthorityPerson entity = spy(makeCleanAuthorityPerson(1));
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(entity);
        doThrow(new UnsupportedOperationException()).when(persistence).save(entity);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
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