package oleg.sopilnyak.test.end2end.service.command.executable.profile;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
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

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateOrUpdateProfileCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistenceFacade;
    @SpyBean
    @Autowired
    PersonProfileRepository<PersonProfileEntity> personProfileRepository;
    @SpyBean
    @Autowired
    CreateOrUpdateProfileCommand command;

    @Test
    void allPartsShouldBeInitiated() {
        assertThat(command).isNotNull();
        assertThat(persistenceFacade).isNotNull();
        assertThat(personProfileRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRedoCommand_Create() {
        PersonProfile profile = makeStudentProfile(null);
        Context<Optional<? extends PersonProfile>> createContext = spy(command.createContext(profile));

        command.redo(createContext);

        assertThat(createContext.getState()).isEqualTo(DONE);
        Optional<? extends PersonProfile> result = createContext.getResult().orElse(null);
        assertThat(result).isNotNull().isNotEmpty();
        PersonProfile resultProfile = result.orElse(null);
        assertThat(resultProfile).isNotNull();
        assertPersonProfilesEquals(resultProfile, profile, false);
        assertThat(createContext.getUndoParameter()).isEqualTo(resultProfile.getId());

        verify(persistenceFacade).save((StudentProfile) profile);
        verify(persistenceFacade).saveProfile(profile);
        verify(createContext).setState(WORK);
        verify(persistenceFacade).toEntity(profile);
        verify(personProfileRepository).saveAndFlush((PersonProfileEntity) resultProfile);
        verify(createContext).setUndoParameter(resultProfile.getId());
        verify(createContext).setResult(Optional.of(resultProfile));
        verify(createContext).setState(DONE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldRedoCommand_Update() {
        PersonProfile profile = makeStudentProfile(null);
        PersonProfileEntity entity = (PersonProfileEntity) persistenceFacade.toEntity(profile);
        personProfileRepository.saveAndFlush(entity);
        reset(personProfileRepository);
        assertThat(entity.getId()).isNotNull().isPositive();
        entity.setEmail(profile.getEmail() + ": AnotherOne");
        PersonProfileEntity toSave = (PersonProfileEntity) persistenceFacade.toEntity(entity);
        Context<Optional<? extends PersonProfile>> updateContext = spy(command.createContext(toSave));

        command.redo(updateContext);

        assertThat(updateContext.getState()).isEqualTo(DONE);
        Optional<? extends PersonProfile> result = updateContext.getResult().orElse(null);
        assertThat(result).isNotNull().isNotEmpty();
        PersonProfile resultProfile = result.orElse(null);
        assertThat(resultProfile).isNotNull();
        assertPersonProfilesEquals(resultProfile, toSave, true);
        assertThat(updateContext.getUndoParameter()).isEqualTo(toSave).isNotSameAs(toSave);

        verify(persistenceFacade).save((StudentProfile) toSave);
        verify(persistenceFacade).saveProfile(toSave);
        verify(updateContext).setState(WORK);
        verify(personProfileRepository).saveAndFlush(toSave);
        verify(updateContext).setUndoParameter(toSave);
        verify(updateContext).setResult(Optional.of(resultProfile));
        verify(updateContext).setState(DONE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontRedoCommand_Update_WrongProfileType() {
        PersonProfile profile = makeStudentProfile(null);
        PersonProfileEntity entity = (PersonProfileEntity) persistenceFacade.toEntity(profile);
        personProfileRepository.saveAndFlush(entity);
        Long studentId = entity.getId();
        assertThat(studentId).isNotNull().isPositive();
        reset(personProfileRepository);
        PersonProfile principal = makePrincipalProfile(studentId);
        PersonProfileEntity toSave = (PersonProfileEntity) persistenceFacade.toEntity(principal);
        toSave.setEmail(toSave.getEmail() + " : AnotherOneEmail");
        Context<Optional<? extends PersonProfile>> updateContext = spy(command.createContext(toSave));

        command.redo(updateContext);

        Optional<? extends PersonProfile> result = updateContext.getResult().orElse(null);
        assertThat(result).isNull();
        assertThat(updateContext.getState()).isEqualTo(FAIL);
        assertThat(updateContext.getUndoParameter()).isEqualTo(entity).isNotSameAs(entity);
        assertThat(updateContext.getException()).isInstanceOf(ProfileNotExistsException.class);

        verify(updateContext).setState(WORK);
        verify(persistenceFacade).findProfileById(toSave.getId());
        verify(personProfileRepository).findById(toSave.getId());
        verify(updateContext).setUndoParameter(any(PersonProfileEntity.class));

        verify(persistenceFacade).save((PrincipalProfile) toSave);
        verify(persistenceFacade).saveProfile(toSave);
        verify(personProfileRepository).saveAndFlush(toSave);
        verify(personProfileRepository).deleteById(anyLong());
        verify(personProfileRepository).flush();

        verify(updateContext).setState(FAIL);
        verify(updateContext).setException(any(ProfileNotExistsException.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontRedoCommand_WrongContextState() {
        Context<Optional<? extends PersonProfile>> context = spy(command.createContext());

        command.redo(context);

        verify(context, times(2)).getState();
        verify(context).setState(FAIL);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_Create() {
        PersonProfile profile = makeStudentProfile(null);
        Context<Optional<? extends PersonProfile>> createContext = spy(command.createContext(profile));
        command.redo(createContext);
        assertThat(createContext.getState()).isEqualTo(DONE);

        command.undo(createContext);

        assertThat(createContext.getState()).isEqualTo(UNDONE);

        Optional<? extends PersonProfile> result = createContext.getResult().orElse(null);
        assertThat(result).isNotNull().isNotEmpty();
        PersonProfile resultProfile = result.orElse(null);
        assertThat(resultProfile).isNotNull();
        assertPersonProfilesEquals(resultProfile, profile, false);
        Long resultId = resultProfile.getId();

        verify(persistenceFacade).save((StudentProfile) profile);
        verify(persistenceFacade).saveProfile(profile);
        verify(createContext, times(2)).setState(WORK);
        verify(persistenceFacade).toEntity(profile);
        verify(personProfileRepository).saveAndFlush((PersonProfileEntity) resultProfile);
        verify(createContext).setUndoParameter(resultId);
        verify(createContext).setResult(Optional.of(resultProfile));
        verify(createContext).setState(DONE);

        verify(createContext).getUndoParameter();
        assertThat(createContext.getUndoParameter()).isEqualTo(resultId);
        verify(personProfileRepository).deleteById(resultId);
        verify(personProfileRepository).flush();
        verify(createContext).setState(UNDONE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_Update() {
        PersonProfile profile = makeStudentProfile(null);
        PersonProfileEntity entity = (PersonProfileEntity) persistenceFacade.toEntity(profile);
        personProfileRepository.saveAndFlush(entity);
        reset(personProfileRepository);
        assertThat(entity.getId()).isNotNull().isPositive();
        entity.setEmail(profile.getEmail() + ": AnotherOne");
        PersonProfileEntity toSave = (PersonProfileEntity) persistenceFacade.toEntity(entity);
        Context<Optional<? extends PersonProfile>> updateContext = spy(command.createContext(toSave));
        command.redo(updateContext);
        assertThat(updateContext.getState()).isEqualTo(DONE);

        command.undo(updateContext);

        Optional<? extends PersonProfile> result = updateContext.getResult().orElse(null);
        assertThat(result).isNotNull().isNotEmpty();
        PersonProfile resultProfile = result.orElse(null);
        assertThat(resultProfile).isNotNull();
        assertPersonProfilesEquals(resultProfile, toSave, true);

        verify(persistenceFacade).save((StudentProfile) toSave);
        verify(persistenceFacade, times(2)).saveProfile(toSave);
        verify(updateContext, times(2)).setState(WORK);
        verify(personProfileRepository, times(2)).saveAndFlush(toSave);
        verify(updateContext).setUndoParameter(toSave);
        verify(updateContext).setResult(Optional.of(resultProfile));
        verify(updateContext).setState(DONE);

        verify(updateContext).getUndoParameter();
        assertThat(updateContext.getUndoParameter()).isEqualTo(toSave).isNotSameAs(toSave);
        verify(updateContext).setState(UNDONE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontUndoCommand_WrongContextState() {
        Context<Optional<? extends PersonProfile>> context = spy(command.createContext());

        command.undo(context);

        verify(context, times(2)).getState();
        verify(context).setState(FAIL);
    }
}