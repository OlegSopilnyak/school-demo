package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.SchoolAccessIsDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LoginAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.PrincipalProfilePayload;
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

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, LoginAuthorityPersonCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class LoginAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    LoginAuthorityPersonCommand command;


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(command, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_EntityExists() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, password});

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(entity));
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_AuthorityPersonNotExists() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, password});
        persistence.deleteAuthorityPerson(entity.getId());

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_PrincipalProfileNotExists() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, password});
        persistence.deleteProfileById(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileIsNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with login:'" + username + "', is not found");
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_FindPrincipalProfileThrows() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        String error = "error finding principal profile";
        RuntimeException runtimeException = new RuntimeException(error);
        doThrow(runtimeException).when(persistence).findPrincipalProfileByLogin(username);
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, password});

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_PrincipalProfileWrongPassword() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, "password"});

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(SchoolAccessIsDeniedException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Login authority person command failed for username:" + username);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence, atLeastOnce()).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_FindAuthorityPersonThrows() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        String error = "error finding authority person";
        RuntimeException runtimeException = new RuntimeException(error);
        doThrow(runtimeException).when(persistence).findAuthorityPersonByProfileId(id);
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, password});

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getException().getMessage()).isEqualTo(error);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence, atLeastOnce()).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_NothingToDo() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        Context<Optional<AuthorityPerson>> context = command.createContext(new String[]{username, password});
        context.setState(DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }

    // private methods
    private AuthorityPersonPayload persist() {
        try {
            PrincipalProfile profile = persistence.save(makePrincipalProfile(null)).orElse(null);
            assertThat(profile).isNotNull();

            AuthorityPerson source = makeCleanAuthorityPerson(0);
            if (source instanceof FakeAuthorityPerson person) {
                person.setProfileId(profile.getId());
            }
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

    private void setPersonPermissions(AuthorityPersonPayload person, String username, String password) {
        try {
            PrincipalProfile profile = persistence.findPrincipalProfileById(person.getProfileId()).orElse(null);
            assertThat(profile).isNotNull();

            PrincipalProfilePayload payload = payloadMapper.toPayload(profile);
            payload.setLogin(username);
            payload.setSignature(payload.makeSignatureFor(password));
            Optional<PrincipalProfile> saved = persistence.save(payload);
            assertThat(saved).isPresent();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            reset(persistence, payloadMapper);
        }
    }
}