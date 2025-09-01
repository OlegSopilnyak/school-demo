package oleg.sopilnyak.test.end2end.command.executable.organization.faculty;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
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
import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindAllFacultiesCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FindAllFacultiesCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    FacultyPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    FacultyCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_EntityExists() {
        Faculty entity = persist();
        Long id = entity.getId();
        Context<Set<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow())
                .contains(persistence.findFacultyById(id).orElseThrow());
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findAllFaculties();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_EntityNotExists() {
        Context<Set<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of());
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findAllFaculties();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_FindThrowsException() {
        doThrow(RuntimeException.class).when(persistence).findAllFaculties();
        Context<Set<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findAllFaculties();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_NothingToDo() {
        Context<Set<Faculty>> context = command.createContext(null);
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
    private Faculty persist() {
        try {
            Faculty source = makeCleanFacultyNoDean(0);
            Faculty entity = persistence.save(source).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<Faculty> person = persistence.findFacultyById(id);
            assertFacultyEquals(person.orElseThrow(), source, false);
            assertThat(person).contains(entity);
            return payloadMapper.toPayload(entity);
        } finally {
            reset(persistence, payloadMapper);
        }
    }
}