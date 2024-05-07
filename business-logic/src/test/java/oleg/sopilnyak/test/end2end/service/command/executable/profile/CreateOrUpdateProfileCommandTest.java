package oleg.sopilnyak.test.end2end.service.command.executable.profile;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
//@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateProfileCommand.class})
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateOrUpdateProfileCommandTest extends MysqlTestModelFactory {
    //    @SpyBean
//    @Autowired
//    ProfilePersistenceFacade persistenceFacade;
//    @SpyBean
//    @Autowired
//    PersonProfileRepository<PersonProfileEntity> personProfileRepository;
//    @SpyBean
//    @Autowired
//    CreateOrUpdateProfileCommand command;
//
    @Test
    void allPartsShouldBeInitiated() {
//        assertThat(command).isNotNull();
//        assertThat(persistenceFacade).isNotNull();
//        assertThat(personProfileRepository).isNotNull();
    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDoCommandCommand_Create() {
//        StudentProfile profile = makeStudentProfile(null);
//        Context<Optional<? extends PersonProfile>> createContext = spy(command.createContext(profile));
//
//        command.doCommand(createContext);
//
//        assertThat(createContext.getState()).isEqualTo(DONE);
//        Optional<? extends PersonProfile> result = (Optional<? extends PersonProfile>) createContext.getResult().orElse(null);
//        assertThat(result).isNotNull().isNotEmpty();
//        PersonProfile resultProfile = result.orElse(null);
//        assertThat(resultProfile).isNotNull();
//        assertPersonProfilesEquals(resultProfile, profile, false);
//        assertThat(createContext.getUndoParameter()).isEqualTo(resultProfile.getId());
//
//        verify(command).executeDo(createContext);
//        verify(persistenceFacade).save(profile);
//        verify(persistenceFacade).saveProfile(profile);
//        verify(createContext).setState(WORK);
//        verify(persistenceFacade).toEntity(profile);
//        verify(personProfileRepository).saveAndFlush((PersonProfileEntity) resultProfile);
//        verify(createContext).setUndoParameter(resultProfile.getId());
//        verify(createContext).setResult(Optional.of(resultProfile));
//        verify(createContext).setState(DONE);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDoCommandCommand_Update() {
//        PersonProfile profile = makeStudentProfile(null);
//        PersonProfileEntity entity = (PersonProfileEntity) persistenceFacade.toEntity(profile);
//        personProfileRepository.saveAndFlush(entity);
//        reset(personProfileRepository);
//        assertThat(entity.getId()).isNotNull().isPositive();
//        entity.setEmail(profile.getEmail() + ": AnotherOne");
//        PersonProfileEntity toSave = (PersonProfileEntity) persistenceFacade.toEntity(entity);
//        Context<Optional<? extends PersonProfile>> updateContext = spy(command.createContext(toSave));
//
//        command.doCommand(updateContext);
//
//        assertThat(updateContext.getState()).isEqualTo(DONE);
//        Optional<? extends PersonProfile> result = (Optional<? extends PersonProfile>) updateContext.getResult().orElse(null);
//        assertThat(result).isNotNull().isNotEmpty();
//        PersonProfile resultProfile = result.orElse(null);
//        assertThat(resultProfile).isNotNull();
//        assertPersonProfilesEquals(resultProfile, toSave, true);
//        assertThat(updateContext.getUndoParameter()).isEqualTo(toSave).isNotSameAs(toSave);
//
//        verify(command).executeDo(updateContext);
//        verify(persistenceFacade).save((StudentProfile) toSave);
//        verify(persistenceFacade).saveProfile(toSave);
//        verify(updateContext).setState(WORK);
//        verify(personProfileRepository).saveAndFlush(toSave);
//        verify(updateContext).setUndoParameter(toSave);
//        verify(updateContext).setResult(Optional.of(resultProfile));
//        verify(updateContext).setState(DONE);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDontDoCommandCommand_Update_WrongProfileType() {
//        PersonProfile profile = makeStudentProfile(null);
//        PersonProfileEntity entity = (PersonProfileEntity) persistenceFacade.toEntity(profile);
//        personProfileRepository.saveAndFlush(entity);
//        Long studentId = entity.getId();
//        assertThat(studentId).isNotNull().isPositive();
//        reset(personProfileRepository);
//        PersonProfile principal = makePrincipalProfile(studentId);
//        PersonProfileEntity toSave = (PersonProfileEntity) persistenceFacade.toEntity(principal);
//        toSave.setEmail(toSave.getEmail() + " : AnotherOneEmail");
//        Context<Optional<? extends PersonProfile>> updateContext = spy(command.createContext(toSave));
//
//        command.doCommand(updateContext);
//
//        assertThat((Optional<PersonProfile>) updateContext.getResult().orElse(Optional.empty())).isEmpty();
//        assertThat(updateContext.getState()).isEqualTo(DONE);
//        assertThat(updateContext.getUndoParameter()).isEqualTo(entity).isNotSameAs(entity);
//        assertThat(updateContext.getException()).isNull();
//
//        verify(command).executeDo(updateContext);
//        verify(updateContext).setState(WORK);
//        verify(persistenceFacade).findProfileById(toSave.getId());
//        verify(personProfileRepository).findById(toSave.getId());
//        verify(updateContext).setUndoParameter(entity);
//
//        verify(persistenceFacade).save((PrincipalProfile) toSave);
//        verify(persistenceFacade).saveProfile(toSave);
//        verify(personProfileRepository).saveAndFlush(toSave);
//        verify(personProfileRepository).deleteById(anyLong());
//        verify(personProfileRepository).flush();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDontDoCommandCommand_WrongContextState() {
//        Context<Optional<? extends PersonProfile>> context = spy(command.createContext());
//
//        command.doCommand(context);
//
//        verify(command, never()).executeDo(context);
//        verify(context, times(2)).getState();
//        verify(context).setState(FAIL);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldUndoCommandCommand_Create() {
//        StudentProfile profile = makeStudentProfile(null);
//        Context<Optional<? extends PersonProfile>> createContext = spy(command.createContext(profile));
//        command.doCommand(createContext);
//        assertThat(createContext.isDone()).isTrue();
//        Optional<? extends PersonProfile> result = (Optional<? extends PersonProfile>) createContext.getResult().orElse(null);
//        assertThat(result).isNotNull().isNotEmpty();
//        Long resultId = result.orElse(null).getId();
//        verify(createContext).setUndoParameter(resultId);
//        reset(command, createContext, persistenceFacade, personProfileRepository);
//
//        command.undoCommand(createContext);
//
//        assertThat(createContext.getState()).isEqualTo(UNDONE);
//        verify(command).executeUndo(createContext);
//        verify(createContext).setState(WORK);
//        verify(createContext).getUndoParameter();
//        assertThat(createContext.getUndoParameter()).isEqualTo(resultId);
//        verify(personProfileRepository).deleteById(resultId);
//        verify(personProfileRepository).flush();
//        verify(createContext).setState(UNDONE);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldUndoCommandCommand_Update() {
//        PersonProfile profile = makeStudentProfile(null);
//        PersonProfileEntity entity = (PersonProfileEntity) persistenceFacade.toEntity(profile);
//        personProfileRepository.saveAndFlush(entity);
//        reset(personProfileRepository);
//        assertThat(entity.getId()).isNotNull().isPositive();
//        entity.setEmail(profile.getEmail() + ": AnotherOne");
//        PersonProfileEntity toSave = (PersonProfileEntity) persistenceFacade.toEntity(entity);
//        Context<Optional<? extends PersonProfile>> updateContext = spy(command.createContext(toSave));
//        command.doCommand(updateContext);
//        assertThat(updateContext.getState()).isEqualTo(DONE);
//
//        command.undoCommand(updateContext);
//
//        Optional<? extends PersonProfile> result = (Optional<? extends PersonProfile>) updateContext.getResult().orElse(null);
//        assertThat(result).isNotNull().isNotEmpty();
//        PersonProfile resultProfile = result.orElse(null);
//        assertThat(resultProfile).isNotNull();
//        assertPersonProfilesEquals(resultProfile, toSave, true);
//
//        verify(command).executeUndo(updateContext);
//        verify(persistenceFacade).save((StudentProfile) toSave);
//        verify(persistenceFacade, times(2)).saveProfile(toSave);
//        verify(updateContext, times(2)).setState(WORK);
//        verify(personProfileRepository, times(2)).saveAndFlush(toSave);
//        verify(updateContext).setUndoParameter(toSave);
//        verify(updateContext).setResult(Optional.of(resultProfile));
//        verify(updateContext).setState(DONE);
//
//        verify(updateContext).getUndoParameter();
//        assertThat(updateContext.getUndoParameter()).isEqualTo(toSave).isNotSameAs(toSave);
//        verify(updateContext).setState(UNDONE);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDontUndoCommandCommand_WrongContextState() {
//        Context<Optional<? extends PersonProfile>> context = spy(command.createContext());
//
//        command.undoCommand(context);
//
//        verify(command, never()).executeUndo(context);
//        verify(context, times(2)).getState();
//        verify(context).setState(FAIL);
//    }
}