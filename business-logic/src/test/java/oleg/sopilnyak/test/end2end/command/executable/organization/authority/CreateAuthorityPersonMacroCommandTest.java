package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
public class CreateAuthorityPersonMacroCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    AuthorityPersonRepository authorityPersonRepository;
    @Autowired
    PersonProfileRepository profileRepository;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("profilePrincipalUpdate")
    PrincipalProfileCommand profileCommand;
    @SpyBean
    @Autowired
    @Qualifier("authorityPersonUpdate")
    AuthorityPersonCommand personCommand;
    @SpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @Autowired
    CommandThroughMessageService messagesExchangeService;

    CreateAuthorityPersonMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateAuthorityPersonMacroCommand(personCommand, profileCommand, payloadMapper, actionExecutor) {
            @Override
            public NestedCommand<?> wrap(NestedCommand<?> command) {
                return spy(super.wrap(command));
            }
        });
        ActionContext.setup("test-facade", "test-action");
        messagesExchangeService.initialize();
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        messagesExchangeService.shutdown();
        authorityPersonRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidCommand() {
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }


    @Test
    void shouldCreateMacroCommandContexts() {
        AuthorityPerson newPerson = payloadMapper.toPayload(makeCleanAuthorityPerson(1));
        reset(payloadMapper);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(parameter).isNotNull();
        assertThat(parameter.getRootInput().value()).isSameAs(newPerson);
        Deque<Context<?>> nested = parameter.getNestedContexts();
        assertThat(nested).hasSameSizeAs(command.fromNest());
        Context<?> profileContext = nested.pop();
        Context<?> personContext = nested.pop();

        assertThat(personContext).isNotNull();
        assertThat(personContext.isReady()).isTrue();
        assertThat(personContext.getCommand()).isSameAs(nestedPersonCommand);
        AuthorityPerson person = personContext.<AuthorityPerson>getRedoParameter().value();
        assertThat(person).isNotNull();
        assertThat(person.getId()).isNull();
        assertAuthorityPersonEquals(newPerson, person);
        String emailPrefix = person.getFirstName().toLowerCase() + "." + person.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(profileContext.getCommand()).isSameAs(nestedProfileCommand);
        PrincipalProfile profile = profileContext.<PrincipalProfile>getRedoParameter().value();
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNull();
        assertThat(profile.getEmail()).startsWith(emailPrefix);
        assertThat(profile.getPhone()).isNotEmpty();

        verifyProfileCommandContext(newPerson, nestedProfileCommand);

        verifyPersonCommandContext(newPerson, nestedPersonCommand);
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        Input<?> wrongInputParameter = Input.of(wrongTypeInput);
        Context<Optional<AuthorityPerson>> context = command.createContext(wrongInputParameter);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.CREATE_OR_UPDATE);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, wrongInputParameter);
        verify(command).prepareContext(nestedProfileCommand, wrongInputParameter);
        verify(command, never()).createProfileContext(eq(nestedProfileCommand), any());
        verify(nestedProfileCommand, never()).createContext(any(Input.class));

        verify(nestedPersonCommand).acceptPreparedContext(command, wrongInputParameter);
        verify(command).prepareContext(nestedPersonCommand, wrongInputParameter);
        verify(command, never()).createPersonContext(eq(nestedPersonCommand), any());
        verify(nestedPersonCommand, never()).createContext(any(Input.class));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateProfileContextThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(2);
        Input<AuthorityPerson> newPersonInput = (Input<AuthorityPerson>) Input.of(newPerson);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(nestedProfileCommand).createContext(any(Input.class));
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, newPersonInput);
        verify(command).prepareContext(nestedProfileCommand, newPersonInput);
        verify(command).createProfileContext(nestedProfileCommand, newPersonInput.value());
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedPersonCommand).acceptPreparedContext(command, newPersonInput);
        verify(command).prepareContext(nestedPersonCommand, newPersonInput);
        verify(command).createPersonContext(nestedPersonCommand, newPersonInput.value());
        verify(nestedPersonCommand).createContext(newPersonInput);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        String errorMessage = "Cannot create nested person context";
        RuntimeException exception = new RuntimeException(errorMessage);
        AuthorityPerson newPerson = makeCleanAuthorityPerson(3);
        Input<AuthorityPerson> newPersonInput = (Input<AuthorityPerson>) Input.of(newPerson);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        doThrow(exception).when(nestedPersonCommand).createContext(newPersonInput);
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, newPersonInput);
        verify(command).prepareContext(nestedProfileCommand, newPersonInput);
        verify(command).createProfileContext(nestedProfileCommand, newPersonInput.value());
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedPersonCommand).acceptPreparedContext(command, newPersonInput);
        verify(command).prepareContext(nestedPersonCommand, newPersonInput);
        verify(command).createPersonContext(nestedPersonCommand, newPersonInput.value());
        verify(nestedPersonCommand).createContext(newPersonInput);
    }

    @Test
    void shouldExecuteDoCommand() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(4);
        Input<?> newPersonInput = Input.of(newPerson);
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);

        command.doCommand(context);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(context);
        Optional<AuthorityPerson> savedPerson = context.getResult().orElseThrow();
        Long personId = savedPerson.orElseThrow().getId();
        assertThat(savedPerson.orElseThrow().getProfileId()).isEqualTo(profileId);
        assertAuthorityPersonEquals(savedPerson.orElseThrow(), newPerson, false);

        checkContextAfterDoCommand(personContext);
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        final AuthorityPerson person = personResult.orElseThrow();
        assertAuthorityPersonEquals(person, newPerson, false);
        assertThat(person.getId()).isEqualTo(personId);
        assertThat(person.getProfileId()).isEqualTo(profileId);
        assertThat(personContext.<Long>getUndoParameter().value()).isEqualTo(personId);
        assertThat(savedPerson.orElseThrow()).isSameAs(person);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), personContext);
        verify(command).transferProfileIdToAuthorityPersonInput(profileId, personContext);

        verifyPersonDoCommand(personContext);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) != null);
        assertAuthorityPersonEquals(persistence.findAuthorityPersonById(personId).orElseThrow(), newPerson, false);
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(makeCleanAuthorityPerson(5)));
        RuntimeException exception = new RuntimeException("Cannot process nested commands");
        doThrow(exception).when(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
    }

    @Test
    void shouldNotExecuteDoCommand_getCommandResultThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(15);
        Input<?> newPersonInput = Input.of(newPerson);
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);
        RuntimeException exception = new RuntimeException("Cannot get command result");
        doThrow(exception).when(command).finalCommandResult(any(Deque.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(personContext);
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        final AuthorityPerson person = personResult.orElseThrow();
        assertAuthorityPersonEquals(person, newPerson, false);
        Long studentId = person.getId();
        assertThat(person.getProfileId()).isEqualTo(profileId);
        assertThat(personContext.<Long>getUndoParameter().value()).isEqualTo(studentId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), personContext);
        verify(command).transferProfileIdToAuthorityPersonInput(profileId, personContext);

        verifyPersonDoCommand(personContext);

        assertAuthorityPersonEquals(persistence.findAuthorityPersonById(studentId).orElseThrow(), newPerson, false);
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(6);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Cannot process profile nested command");
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isEqualTo(exception);

        assertThat(personContext.getState()).isEqualTo(CANCEL);

        verify(profileCommand).doCommand(profileContext);
        verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));

        verify(command, never()).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any(Context.class));

        verify(command, never()).executeDoNested(eq(personContext), any(Context.StateChangedListener.class));
        verify(personCommand, never()).doCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreateAuthorityPersonDoNestedCommandsThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(7);
        Input<AuthorityPerson> newPersonInput = (Input<AuthorityPerson>) Input.of(newPerson);
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);

        RuntimeException exception = new RuntimeException("Don't want to process person nested command");
        doThrow(exception).when(persistence).save(newPersonInput.value());

        command.doCommand(context);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() ->
                        context.<MacroCommandParameter>getRedoParameter().value().getNestedContexts().getFirst().isUndone()
        );
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();

        assertThat(personContext.isFailed()).isTrue();
        AuthorityPerson person = personContext.<AuthorityPerson>getRedoParameter().value();
        Long profileId = person.getProfileId();
        assertAuthorityPersonEquals(person, newPerson, false);

        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(eq(profileCommand), any(Optional.class), eq(personContext));
        verify(command).transferProfileIdToAuthorityPersonInput(profileId, personContext);

        verifyPersonDoCommand(personContext);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileContext, profileId);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(PrincipalProfileEntity.class, profileId) == null);
    }

    @Test
    void shouldExecuteUndoCommand() {
        AuthorityPerson person = makeCleanAuthorityPerson(8);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(person));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        command.doCommand(context);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long personId = personResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(personResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) != null);
        assertAuthorityPersonEquals(persistence.findAuthorityPersonById(personId).orElseThrow(), person, false);
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        assertThat(profileContext.isUndone()).isTrue();
        assertThat(personContext.isUndone()).isTrue();

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyPersonUndoCommand(personContext, personId);
        // nested commands order
        checkUndoNestedCommandsOrder(profileContext, personContext, personId, profileId);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(PrincipalProfileEntity.class, profileId) == null);
    }


    @Test
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(11);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        command.doCommand(context);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long personId = personResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(personResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(PrincipalProfileEntity.class, profileId) != null);
        assertAuthorityPersonEquals(persistence.findAuthorityPersonById(personId).orElseThrow(), newPerson, false);
        RuntimeException exception = new RuntimeException("Don't want to process nested undo commands");
        doThrow(exception).when(command).rollbackNested(any(Deque.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(personCommand, never()).undoCommand(any(Context.class));
        verify(profileCommand, never()).undoCommand(any(Context.class));

        assertThat(persistence.findAuthorityPersonById(personId)).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
    }

    @Test
    void shouldNotExecuteUndoCommand_AuthorityPersonUndoThrowsException() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(9);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        command.doCommand(context);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long personId = personResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(personResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(PrincipalProfileEntity.class, profileId) != null);
        assertAuthorityPersonEquals(persistence.findAuthorityPersonById(personId).orElseThrow(), newPerson, false);
        RuntimeException exception = new RuntimeException("Don't want to process person undo command");
        doThrow(exception).when(persistence).deleteAuthorityPerson(personId);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        assertThat(profileContext.isDone()).isTrue();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verifyPersonUndoCommand(personContext, personId);

        assertThat(persistence.findAuthorityPersonById(personId)).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
    }

    @Test
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(10);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        command.doCommand(context);
        reset(persistence, command, personCommand);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> studentResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        final Long personId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) != null);
        assertAuthorityPersonEquals(persistence.findAuthorityPersonById(personId).orElseThrow(), newPerson, false);
        RuntimeException exception = new RuntimeException("Don't want to process profile undo command");
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.undoCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findEntity(AuthorityPersonEntity.class, personId) == null);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isEqualTo(exception);
        assertThat(personContext.isUndone()).isFalse();
        assertThat(personContext.isDone()).isTrue();

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyPersonUndoCommand(personContext, personId);
        verifyPersonDoCommand(personContext);

        assertThat(findEntity(AuthorityPersonEntity.class, personId)).isNull();
        assertThat(findEntity(PrincipalProfileEntity.class, profileId)).isNotNull();
        Long resultPersonId = personContext.getResult().orElseThrow().orElseThrow().getId();
        Long resultProfileId = profileContext.getResult().orElseThrow().orElseThrow().getId();
        assertThat(resultProfileId).isEqualTo(profileId);
        assertThat(findEntity(AuthorityPersonEntity.class, resultPersonId)).isNotNull();
        assertThat(findEntity(PrincipalProfileEntity.class, resultProfileId)).isNotNull();
    }

    // private methods
    private void checkUndoNestedCommandsOrder(Context<Optional<PrincipalProfile>> profileContext,
                                              Context<Optional<AuthorityPerson>> personContext,
                                              Long personId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);
        // creating profile and person (profile is first) avers commands order
        inOrder.verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        inOrder.verify(command).executeDoNested(eq(personContext), any(Context.StateChangedListener.class));
        // undo creating profile and person (person is first) revers commands order
        inOrder.verify(command).executeUndoNested(personContext);
        inOrder.verify(command).executeUndoNested(profileContext);

        // persistence operations order
        inOrder = Mockito.inOrder(persistence);
        // creating profile and person (profile is first) avers operations order
        inOrder.verify(persistence).save(any(PrincipalProfile.class));
        inOrder.verify(persistence).save(any(AuthorityPerson.class));
        // undo creating profile and person (person is first) revers operations order
        inOrder.verify(persistence).deleteAuthorityPerson(personId);
        inOrder.verify(persistence).deleteProfileById(profileId);
    }

    private void verifyProfileUndoCommand(Context<Optional<PrincipalProfile>> nestedContext, Long id) {
        verify(command).executeUndoNested(nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyPersonUndoCommand(Context<Optional<AuthorityPerson>> nestedContext, Long id) {
        verify(command).executeUndoNested(nestedContext);
        verify(personCommand).undoCommand(nestedContext);
        verify(personCommand).executeUndo(nestedContext);
        verify(persistence).deleteAuthorityPerson(id);
    }

    private void verifyProfileDoCommand(Context<Optional<PrincipalProfile>> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(nestedContext);
        verify(profileCommand).executeDo(nestedContext);
        verify(persistence).save(any(PrincipalProfile.class));
    }

    private void verifyPersonDoCommand(Context<Optional<AuthorityPerson>> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any());
        verify(personCommand).doCommand(nestedContext);
        verify(personCommand).executeDo(nestedContext);
        verify(persistence).save(any(AuthorityPerson.class));
    }

    private static <T> void checkContextAfterDoCommand(Context<Optional<T>> context) {
        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().get()).isPresent();
    }

    private void verifyPersonCommandContext(AuthorityPerson newPerson, AuthorityPersonCommand<?> personCommand) {
        Input<?> newPersonInput = Input.of(newPerson);
        verify(personCommand).acceptPreparedContext(command, newPersonInput);
        verify(command).prepareContext(personCommand, newPersonInput);
        verify(command).createPersonContext(personCommand, newPerson);
        verify(personCommand).createContext(any(Input.class));
    }

    private void verifyProfileCommandContext(AuthorityPerson newPerson, PrincipalProfileCommand<?> profileCommand) {
        Input<?> newPersonInput = Input.of(newPerson);
        verify(profileCommand).acceptPreparedContext(command, newPersonInput);
        verify(command).prepareContext(profileCommand, newPersonInput);
        verify(command).createProfileContext(profileCommand, newPerson);
        verify(profileCommand).createContext(any(Input.class));
    }
}
