package oleg.sopilnyak.test.end2end.command.executable.organization.faculty;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindFacultyCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FindFacultyCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    FacultyPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    FacultyCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(FacultyEntity.class);
    }

    @Test
    void shouldDoCommand_EntityExists() {
        Faculty entity = persist();
        Long id = entity.getId();
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
        assertThat(context.getResult().orElseThrow()).contains(payloadMapper.toPayload(findFacultyById(id)));
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        long id = 421L;
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        long id = 422L;
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));
        doThrow(RuntimeException.class).when(persistence).findFacultyById(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        long id = 423L;
        Context<Optional<Faculty>> context = command.createContext(Input.of(id));
        context.setState(DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(persist()));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }

    // private methods
    private Faculty findFacultyById(Long id) {
        return findEntity(FacultyEntity.class, id, entity -> entity.getCourseEntitySet().size());
    }

    private Faculty persist() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            Faculty source = makeCleanFacultyNoDean(0);
            FacultyEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(FacultyEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
            em.close();
        }
    }
}