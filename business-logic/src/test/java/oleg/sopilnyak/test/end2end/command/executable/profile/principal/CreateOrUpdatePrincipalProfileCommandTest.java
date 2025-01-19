package oleg.sopilnyak.test.end2end.command.executable.profile.principal;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdatePrincipalProfileCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateOrUpdatePrincipalProfileCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    CreateOrUpdatePrincipalProfileCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_UpdateProfile() {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<PrincipalProfile>getUndoParameter().value()).isEqualTo(profile);
        assertProfilesEquals(profile, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(payloadMapper).toPayload(any(PrincipalProfileEntity.class));
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_CreateProfile() {
        PrincipalProfile profile = makePrincipalProfile(null);
        if (profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setLogin(fakeProfile.getLogin() + "-> 1");
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Input<PrincipalProfile> input = (Input<PrincipalProfile>) Input.of(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Optional<PrincipalProfile> doResult = context.getResult().orElseThrow();
        assertProfilesEquals(doResult.orElseThrow(), profile, false);
        assertThat(context.getUndoParameter().value()).isEqualTo(doResult.get().getId());
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
        verify(persistence).saveProfile(input.value());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongState() {
        Context<Optional<PrincipalProfile>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_IncompatibleProfileType() {
        StudentProfile profile = persistStudentProfile();
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");

        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(profile.getId());
        verify(persistence).findProfileById(profile.getId());
        verify(persistence, never()).save(any(PrincipalProfile.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ProfileNotFound() {
        Long id = 701L;
        PrincipalProfile profile = makePrincipalProfile(id);
        if (profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setLogin(fakeProfile.getLogin() + "-> 1");
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(PrincipalProfile.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(PrincipalProfile.class));
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        PrincipalProfile profile = makePrincipalProfile(null);
        if (profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setLogin(fakeProfile.getLogin() + "-> 1");
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Input<PrincipalProfile> input = (Input<PrincipalProfile>) Input.of(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(input);

        doThrow(RuntimeException.class).when(persistence).saveProfile(input.value());
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
        verify(persistence).saveProfile(input.value());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        PrincipalProfile exists = persistence.findPrincipalProfileById(id).orElseThrow();
        reset(persistence);
        doThrow(RuntimeException.class).when(persistence).saveProfile(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(exists);
        verify(payloadMapper).toPayload(exists);
        verify(persistence, times(2)).save(profile);
        verify(persistence, times(2)).saveProfile(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        assertThat(context.getException().getMessage()).contains("cannot be cast to class oleg.sopilnyak.test.school.common.model.PersonProfile");
        verify(command).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<PrincipalProfile>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).contains("Wrong input parameter value");
        verify(command).executeDo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_DeleteCreated() {
        Long id = persistPrincipalProfile().getId();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(persistence.findPrincipalProfileById(id)).isEmpty();
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_RestoreUpdated() {
        PrincipalProfile profile = persistPrincipalProfile();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<PrincipalProfile>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
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
        Context<Optional<PrincipalProfile>> context = command.createContext();
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
    void shouldNotUndoCommand_DeleteByIdExceptionThrown() throws ProfileNotFoundException {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteProfileById(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(persistence.findPrincipalProfileById(id)).isPresent();
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_SaveProfileExceptionThrown() {
        PrincipalProfile profile = persistPrincipalProfile();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }
        context.setState(Context.State.DONE);

        doThrow(new RuntimeException()).when(persistence).saveProfile(profile);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    // private methods
    private PrincipalProfile persistPrincipalProfile() {
        int order = 0;
        try {
            PrincipalProfile profile = makePrincipalProfile(null);
            if (profile instanceof FakePrincipalProfile fakeProfile) {
                fakeProfile.setLogin(fakeProfile.getLogin() + "->" + order);
                fakeProfile.setEmail(fakeProfile.getEmail() + ".site" + order);
            }
            PrincipalProfile entity = persistence.save(profile).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<PrincipalProfile> dbProfile = persistence.findPrincipalProfileById(id);
            assertProfilesEquals(dbProfile.orElseThrow(), profile, false);
            assertThat(dbProfile).contains(entity);
            return payloadMapper.toPayload(persistence.toEntity(entity));
        } finally {
            reset(persistence, payloadMapper);
        }
    }

    private StudentProfile persistStudentProfile() {
        int order = 0;
        try {
            StudentProfile profile = makeStudentProfile(null);
            if (profile instanceof FakeStudentsProfile fakeProfile) {
                fakeProfile.setEmail(fakeProfile.getEmail() + ".site" + order);
            }
            StudentProfile entity = persistence.save(profile).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<StudentProfile> dbProfile = persistence.findStudentProfileById(id);
            assertProfilesEquals(dbProfile.orElseThrow(), profile, false);
            assertThat(dbProfile).contains(entity);
            return payloadMapper.toPayload(persistence.toEntity(entity));
        } finally {
            reset(persistence, payloadMapper);
        }
    }
}