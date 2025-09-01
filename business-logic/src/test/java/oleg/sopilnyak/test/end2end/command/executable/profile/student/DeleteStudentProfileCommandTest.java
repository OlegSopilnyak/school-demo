package oleg.sopilnyak.test.end2end.command.executable.profile.student;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, DeleteStudentProfileCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class DeleteStudentProfileCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    StudentProfileCommand command;

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
    void shouldDoCommand_ProfileExists() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        StudentProfile exists = persistence.findStudentProfileById(id).orElseThrow();
        reset(persistence);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getException()).isNull();
        assertThat(context.getResult()).contains(true);
        assertProfilesEquals(profile, context.<StudentProfile>getUndoParameter().value(), false);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(exists);
        verify(payloadMapper).toPayload(exists);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NoProfile() {
        long id = 415L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(StudentProfile.class));
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongParameterType() throws ProfileNotFoundException {
        Context<Boolean> context = command.createContext(Input.of("id"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findProfileById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NullParameter() throws ProfileNotFoundException {
        Context<Boolean> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value");
        verify(command).executeDo(context);
        verify(persistence, never()).findProfileById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() throws ProfileNotFoundException {
        StudentProfilePayload profile = persistStudentProfile();
        long id = profile.getId();
        doThrow(new UnsupportedOperationException()).when(persistence).deleteProfileById(id);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile.getOriginal());
        verify(persistence).deleteProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_UndoProfileExists() {
        StudentProfile profile = persistStudentProfile();
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_WrongUndoCommandParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("input"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'StudentProfile' value:[input]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_NullUndoCommandParameter() {
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
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_ExceptionThrown() {
        StudentProfile profile = persistStudentProfile();
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }

        doThrow(new UnsupportedOperationException()).when(persistence).saveProfile(profile);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    // private methods
    private StudentProfilePayload persistStudentProfile() {
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