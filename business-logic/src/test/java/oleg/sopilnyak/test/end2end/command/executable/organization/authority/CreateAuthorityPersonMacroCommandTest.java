package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
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
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
public class CreateAuthorityPersonMacroCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("profilePrincipalUpdate")
    PrincipalProfileCommand profileCommand;
    @MockitoSpyBean
    @Autowired
    @Qualifier("authorityPersonUpdate")
    AuthorityPersonCommand personCommand;
    @MockitoSpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @Autowired
    CommandThroughMessageService messagesExchangeService;

    CreateAuthorityPersonMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateAuthorityPersonMacroCommand(personCommand, profileCommand, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-processing");
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        deleteEntities(AuthorityPersonEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
    }

    @Test
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
        assertEquals(personContext.getCommand(), nestedPersonCommand);
        nestedPersonCommand = (AuthorityPersonCommand<?>) personContext.getCommand();
        AuthorityPerson person = personContext.<AuthorityPerson>getRedoParameter().value();
        assertThat(person).isNotNull();
        assertThat(person.getId()).isNull();
        assertAuthorityPersonEquals(newPerson, person);
        String emailPrefix = person.getFirstName().toLowerCase() + "." + person.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertEquals(profileContext.getCommand(), nestedProfileCommand);
        nestedProfileCommand = (PrincipalProfileCommand<?>) profileContext.getCommand();
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
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.CommandId.CREATE_OR_UPDATE);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, wrongInputParameter);
        ArgumentCaptor<PrincipalProfileCommand> profileCmdCaptor = ArgumentCaptor.forClass(PrincipalProfileCommand.class);
        ArgumentCaptor<Input> profileInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(profileCmdCaptor.capture(), profileInputCaptor.capture());
        // check profile command-context
        assertEquals(profileCmdCaptor.getValue(), nestedProfileCommand);
        nestedProfileCommand = profileCmdCaptor.getValue();
        assertThat(profileInputCaptor.getValue()).isSameAs(wrongInputParameter);
        verify(command, never()).createProfileContext(any(PrincipalProfileCommand.class), any());
        verify(nestedProfileCommand, never()).createContext(any(Input.class));

        verify(nestedPersonCommand).acceptPreparedContext(command, wrongInputParameter);
        ArgumentCaptor<AuthorityPersonCommand> personCmdCaptor = ArgumentCaptor.forClass(AuthorityPersonCommand.class);
        ArgumentCaptor<Input> personInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(personCmdCaptor.capture(), personInputCaptor.capture());
        // check person command-context
        assertEquals(personCmdCaptor.getValue(), nestedPersonCommand);
        nestedPersonCommand = personCmdCaptor.getValue();
        verify(command, never()).createPersonContext(any(AuthorityPersonCommand.class), any());
        verify(nestedPersonCommand, never()).createContext(any(Input.class));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateProfileContextThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(2);
        Input<AuthorityPerson> newPersonInput = (Input<AuthorityPerson>) Input.of(newPerson);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        String errorMessage = "Don't want to create nested profile context. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(nestedProfileCommand).createContext(any(Input.class));
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        //
        // person part of building
        verify(nestedPersonCommand).acceptPreparedContext(command, newPersonInput);
        ArgumentCaptor<AuthorityPersonCommand> personCmdCaptor = ArgumentCaptor.forClass(AuthorityPersonCommand.class);
        verify(command).prepareContext(personCmdCaptor.capture(), any(Input.class));
        // check person command
        assertEquals(personCmdCaptor.getValue(), nestedPersonCommand);
        nestedPersonCommand = personCmdCaptor.getValue();
        // check person command input
        ArgumentCaptor<Input> personInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(nestedPersonCommand).createContext(personInputCaptor.capture());
        Input<AuthorityPerson> personInput = personInputCaptor.getValue();
        AuthorityPerson inputParameter = personInput.value();
        assertAuthorityPersonEquals(newPerson, inputParameter);
        // create particular context
        verify(command).createPersonContext(nestedPersonCommand, inputParameter);
        //
        // profile part of building
        verify(nestedProfileCommand).acceptPreparedContext(command, newPersonInput);
        ArgumentCaptor<PrincipalProfileCommand> profileCmdCaptor = ArgumentCaptor.forClass(PrincipalProfileCommand.class);
        verify(command).prepareContext(profileCmdCaptor.capture(), any(Input.class));
        // check profile command
        assertEquals(profileCmdCaptor.getValue(), nestedProfileCommand);
        nestedProfileCommand = profileCmdCaptor.getValue();
        // check profile command input
        ArgumentCaptor<Input> profileInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(nestedProfileCommand).createContext(profileInputCaptor.capture());
        assertThat(profileInputCaptor.getValue().value()).isInstanceOf(PrincipalProfilePayload.class);
        // create particular context
        verify(command).createProfileContext(nestedProfileCommand, inputParameter);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        String errorMessage = "Don't want to create nested person context. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        AuthorityPerson newPerson = makeCleanAuthorityPerson(3);
        Input<AuthorityPerson> newPersonInput = (Input<AuthorityPerson>) Input.of(newPerson);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        doThrow(exception).when(nestedPersonCommand).createContext(newPersonInput);
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        //
        // person part of building
        verify(nestedPersonCommand).acceptPreparedContext(command, newPersonInput);
        ArgumentCaptor<AuthorityPersonCommand> personCmdCaptor = ArgumentCaptor.forClass(AuthorityPersonCommand.class);
        verify(command).prepareContext(personCmdCaptor.capture(), any(Input.class));
        // check person command
        assertEquals(personCmdCaptor.getValue(), nestedPersonCommand);
        nestedPersonCommand = personCmdCaptor.getValue();
        // check person command input
        ArgumentCaptor<Input> personInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(nestedPersonCommand).createContext(personInputCaptor.capture());
        assertThat(personInputCaptor.getValue().value()).isInstanceOf(AuthorityPersonPayload.class);
        Input<AuthorityPerson> personInput = personInputCaptor.getValue();
        AuthorityPerson inputParameter = personInput.value();
        assertAuthorityPersonEquals(newPerson, inputParameter);
        // create particular context instance
        verify(command).createPersonContext(nestedPersonCommand, inputParameter);
        //
        // profile part of building
        verify(nestedProfileCommand).acceptPreparedContext(command, newPersonInput);
        ArgumentCaptor<PrincipalProfileCommand> profileCmdCaptor = ArgumentCaptor.forClass(PrincipalProfileCommand.class);
        verify(command).prepareContext(profileCmdCaptor.capture(), any(Input.class));
        // check profile command
        assertEquals(profileCmdCaptor.getValue(), nestedProfileCommand);
        nestedProfileCommand = profileCmdCaptor.getValue();
        // check profile command input
        ArgumentCaptor<Input> profileInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(nestedProfileCommand).createContext(profileInputCaptor.capture());
        assertThat(profileInputCaptor.getValue().value()).isInstanceOf(PrincipalProfilePayload.class);
        // create particular context instance
        verify(command).createProfileContext(nestedProfileCommand, inputParameter);
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
        verify(command).transferResult(any(RootCommand.class), any(), any(Context.class));
        verify(command, times(command.fromNest().size())).executeDoNested(any(Context.class), any(Context.StateChangedListener.class));

        // check nested profile create
        verifyProfileDoCommand();

        // check transfer profile-id to person creation command-context
        ArgumentCaptor<Context<Optional<AuthorityPerson>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Optional<PrincipalProfile>> profileResultCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(command).transferResult(any(RootCommand.class), profileResultCaptor.capture(), personContextCaptor.capture());
        assertThat(profileResultCaptor.getValue().orElseThrow().getId()).isEqualTo(profileId);
        verify(command).transferProfileIdToAuthorityPersonUpdateInput(profileId, personContextCaptor.getValue());

        // check nested person create
        verifyPersonDoCommand();

        // check database state
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        assertAuthorityPersonEquals(findPersonEntity(personId), newPerson, false);
        assertThat(findProfileEntity(profileId)).isNotNull();
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(makeCleanAuthorityPerson(5)));
        RuntimeException exception = new RuntimeException("Don't want to execute nested commands. Bad guy!");
        doThrow(exception).when(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isSameAs(exception);
    }

    @Test
    void shouldNotExecuteDoCommand_getCommandResultThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(15);
        Input<?> newPersonInput = Input.of(newPerson);
        Context<Optional<AuthorityPerson>> context = command.createContext(newPersonInput);
        RuntimeException exception = new RuntimeException("Don't want to get command result. Bad guy!");
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
        AuthorityPerson person = personContext.getResult().orElseThrow().orElseThrow();
        assertAuthorityPersonEquals(person, newPerson, false);
        Long personId = person.getId();
        assertThat(person.getProfileId()).isEqualTo(profileId);
        assertThat(personContext.<Long>getUndoParameter().value()).isEqualTo(personId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        // check nested profile create
        verifyProfileDoCommand();

        // check transfer profile-id to person creation command-context
        ArgumentCaptor<Context<Optional<AuthorityPerson>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Optional<PrincipalProfile>> profileResultCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(command).transferResult(any(RootCommand.class), profileResultCaptor.capture(), personContextCaptor.capture());
        assertThat(profileResultCaptor.getValue().orElseThrow().getId()).isEqualTo(personId);
        verify(command).transferProfileIdToAuthorityPersonUpdateInput(profileId, personContextCaptor.getValue());

        // check nested person create
        verifyPersonDoCommand();

        // check database state
        assertAuthorityPersonEquals(findPersonEntity(personId), newPerson, false);
        assertThat(findProfileEntity(profileId)).isNotNull();
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(6);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));

        RuntimeException exception = new RuntimeException("Don't want to execute create profile command. Bad guy!");
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();

        // checking profile execution
        verify(command).executeDoNested(any(Context.class), any(Context.StateChangedListener.class));
        ArgumentCaptor<Context<Optional<PrincipalProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).doCommand(contextCaptor.capture());
        Context<Optional<PrincipalProfile>> profileContext = contextCaptor.getValue();
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));
        // checking person execution
        verify(command, never()).executeDoNested(eq(personContext), any(Context.StateChangedListener.class));
        // checking the contexts updates
        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();
        // profile context state
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo(exception.getMessage());

        // person context state
        assertThat(personContext.getState()).isEqualTo(CANCEL);

        verify(command, never()).transferResult(any(RootCommand.class), any(Optional.class), any(Context.class));
        verify(command, never()).transferProfileIdToAuthorityPersonUpdateInput(anyLong(), any(Context.class));

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
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();

        assertThat(personContext.isFailed()).isTrue();
        AuthorityPerson person = personContext.<AuthorityPerson>getRedoParameter().value();
        Long profileId = person.getProfileId();
        assertAuthorityPersonEquals(person, newPerson, false);

        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        // check nested profile create
        verifyProfileDoCommand();

        // check transfer profile-id to person creation command-context
        ArgumentCaptor<Context<Optional<AuthorityPerson>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Optional<PrincipalProfile>> profileResultCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(command).transferResult(any(RootCommand.class), profileResultCaptor.capture(), personContextCaptor.capture());
        assertThat(profileResultCaptor.getValue().orElseThrow().getId()).isEqualTo(profileId);
        verify(command).transferProfileIdToAuthorityPersonUpdateInput(profileId, personContextCaptor.getValue());

        // check nested person create
        verifyPersonDoCommand(false);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileId);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }

    @Test
    void shouldExecuteUndoCommand() {
        AuthorityPerson person = makeCleanAuthorityPerson(8);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(person));
        command.doCommand(context);
        // check context after execution
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long personId = personResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(personResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertAuthorityPersonEquals(findPersonEntity(personId), person, false);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        // prepare nested contexts to check
        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(personContext.isUndone()).isTrue();

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(command, times(command.fromNest().size())).executeUndoNested(any(Context.class));
        verifyProfileUndoCommand(profileId);
        verifyPersonUndoCommand(personId);
        // nested commands order
        checkUndoNestedCommandsOrder(personId, profileId);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }


    @Test
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(11);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        command.doCommand(context);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long personId = personResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(personResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertAuthorityPersonEquals(findPersonEntity(personId), newPerson, false);
        RuntimeException exception = new RuntimeException("Don't want to process nested undo commands. Bad Guy!");
        doThrow(exception).when(command).rollbackNested(any(Deque.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(personCommand, never()).undoCommand(any(Context.class));
        verify(profileCommand, never()).undoCommand(any(Context.class));

        assertThat(findPersonEntity(personId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
    }

    @Test
    void shouldNotExecuteUndoCommand_AuthorityPersonUndoThrowsException() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(9);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        command.doCommand(context);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long personId = personResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(personResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertAuthorityPersonEquals(findPersonEntity(personId), newPerson, false);
        RuntimeException exception = new RuntimeException("Don't want to process person undo command");
        doThrow(exception).when(persistence).deleteAuthorityPerson(personId);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());

        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(exception.getClass());
        assertThat(personContext.getException().getMessage()).isEqualTo(exception.getMessage());

        verify(command).executeUndo(context);
        verifyPersonUndoCommand(personId);

        assertThat(findPersonEntity(personId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
    }

    @Test
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(10);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        command.doCommand(context);
        reset(persistence, command, personCommand);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<AuthorityPerson> studentResult = personContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        final Long personId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        assertAuthorityPersonEquals(findPersonEntity(personId), newPerson, false);
        RuntimeException exception = new RuntimeException("Don't want to process profile undo command, Bad Guy!");
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.undoCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());

        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(exception.getClass());
        assertThat(profileContext.getException().getMessage()).isEqualTo(exception.getMessage());
        assertThat(personContext.isUndone()).isFalse();
        assertThat(personContext.isDone()).isTrue();

        verify(command).executeUndo(context);
        verify(command, times(command.fromNest().size())).executeUndoNested(any(Context.class));
        verifyProfileUndoCommand(profileId);
        verifyPersonUndoCommand(personId);
        verifyPersonDoCommand();

        assertThat(findPersonEntity(personId)).isNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
        Long resultPersonId = personContext.getResult().orElseThrow().orElseThrow().getId();
        assertThat(findPersonEntity(resultPersonId)).isNotNull();
    }

    // private methods
    private <N extends RootCommand<?>>void assertEquals(N actual, N expected) {
        assertThat(actual.commandFamily()).isSameAs(expected.commandFamily());
        assertThat(actual.getId()).isEqualTo(expected.getId());
    }

    private AuthorityPersonEntity findPersonEntity(Long id) {
        return findEntity(AuthorityPersonEntity.class, id);
    }

    private PrincipalProfileEntity findProfileEntity(Long id) {
        return findEntity(PrincipalProfileEntity.class, id);
    }

    private void checkUndoNestedCommandsOrder(Long personId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);
        // creating profile and person (profile is first) avers commands order
        ArgumentCaptor<Context<Optional<PrincipalProfile>>> profileContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Context<Optional<AuthorityPerson>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        inOrder.verify(command).executeDoNested(profileContextCaptor.capture(), any(Context.StateChangedListener.class));
        assertContextOf(profileContextCaptor.getValue(), profileCommand);
        inOrder.verify(command).executeDoNested(personContextCaptor.capture(), any(Context.StateChangedListener.class));
        assertContextOf(personContextCaptor.getValue(), personCommand);

        // undo creating profile and person (person is first) revers commands order
        personContextCaptor = ArgumentCaptor.forClass(Context.class);
        profileContextCaptor = ArgumentCaptor.forClass(Context.class);
        inOrder.verify(command).executeUndoNested(personContextCaptor.capture());
        assertContextOf(personContextCaptor.getValue(), personCommand);
        inOrder.verify(command).executeUndoNested(profileContextCaptor.capture());
        assertContextOf(profileContextCaptor.getValue(), profileCommand);

        // persistence operations order
        inOrder = Mockito.inOrder(persistence);
        // creating profile and person (profile is first) avers operations order
        inOrder.verify(persistence).save(any(PrincipalProfile.class));
        inOrder.verify(persistence).save(any(AuthorityPerson.class));
        // undo creating profile and person (person is first) revers operations order
        inOrder.verify(persistence).deleteAuthorityPerson(personId);
        inOrder.verify(persistence).deleteProfileById(profileId);
    }

    private void assertContextOf(Context<?> context, RootCommand<?> command) {
        assertThat(context.getCommand().getId()).isEqualTo(command.getId());
    }

    private void verifyProfileUndoCommand(Long id) {
        String contextCommandId = profileCommand.getId();
        ArgumentCaptor<Context<Optional<PrincipalProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isProfile = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isProfile).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), profileCommand);
        verify(profileCommand).executeUndo(contextCaptor.getValue());
        verify(persistence).deleteProfileById(id);
    }

    private void verifyPersonUndoCommand(Long id) {
        String contextCommandId = personCommand.getId();
        ArgumentCaptor<Context<Optional<AuthorityPerson>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isPerson = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isPerson).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), personCommand);
        verify(personCommand).executeUndo(contextCaptor.getValue());
        verify(persistence).deleteAuthorityPerson(id);
    }

    private void verifyProfileDoCommand() {
        ArgumentCaptor<Context<Optional<PrincipalProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        Optional<PrincipalProfile> profile = nestedContext.getResult().orElseThrow();
        assertThat(profile).isNotEmpty();
        assertThat(nestedContext.getCommand().getId()).isEqualTo(profileCommand.getId());
        verify(profileCommand).executeDo(nestedContext);
        verify(persistence).save(any(PrincipalProfile.class));
    }

    private void verifyPersonDoCommand() {
        verifyPersonDoCommand(true);
    }

    private void verifyPersonDoCommand(boolean checkResult) {
        ArgumentCaptor<Context<Optional<AuthorityPerson>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        if (checkResult) {
            assertThat(nestedContext.getResult().orElseThrow()).isNotEmpty();
        }
        assertThat(nestedContext.getCommand().getId()).isEqualTo(personCommand.getId());
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
