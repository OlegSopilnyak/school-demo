package oleg.sopilnyak.test.service.command.executable.sys.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class SchoolCommandCacheTest {
    SchoolCommandCache cache;

    @BeforeEach
    void setUp() {
        cache = spy(new SchoolCommandCache() {
            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger(getClass());
            }
        });
    }

    @Test
    void shouldRetrieveEntity() {
        long inputId = 1L;
        BaseType value = mock(BaseType.class);
        BaseType adopted = mock(BaseType.class);
        EntityNotFoundException exception = mock(EntityNotFoundException.class);
        LongFunction<Optional<BaseType>> findEntityById = spy(new FindById(value));
        UnaryOperator<BaseType> adoptEntity = spy(new AdoptEntity(adopted));
        Supplier<? extends EntityNotFoundException> exceptionSupplier = spy(new ErrorHandler(exception));

        BaseType result = cache.retrieveEntity(inputId, findEntityById, adoptEntity, exceptionSupplier);

        assertThat(result).isNotNull().isEqualTo(adopted);
        verify(findEntityById).apply(inputId);
        verify(adoptEntity).apply(value);
        verify(exceptionSupplier, never()).get();
    }

    @Test
    void shouldNotRetrieveEntity_NotFound() {
        long inputId = 2L;
        BaseType adopted = mock(BaseType.class);
        EntityNotFoundException exception = new EntityNotFoundException("message") {
        };
        LongFunction<Optional<BaseType>> findEntityById = spy(new FindById(null));
        UnaryOperator<BaseType> adoptEntity = spy(new AdoptEntity(adopted));
        Supplier<? extends EntityNotFoundException> exceptionSupplier = spy(new ErrorHandler(exception));

        Exception ex = assertThrows(EntityNotFoundException.class,
                () -> cache.retrieveEntity(inputId, findEntityById, adoptEntity, exceptionSupplier)
        );

        assertThat(ex).isNotNull().isSameAs(exception);
        verify(findEntityById).apply(inputId);
        verify(adoptEntity, never()).apply(any(BaseType.class));
        verify(exceptionSupplier).get();
    }

    @Test
    void shouldRollbackCachedEntityRestore() {
        BaseType value = mock(BaseType.class);
        Context<BaseType> context = mock(Context.class);
        Input<?> input = Input.of(value);
        doReturn(input).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        Optional<BaseType> result = cache.rollbackCachedEntity(context, facadeSave);

        assertThat(result).isNotNull().isPresent().isEqualTo(Optional.of(value));
        verify(cache).rollbackCachedEntity(context, facadeSave, null);
        verify(facadeSave).apply(value);
    }

    @Test
    void shouldNotRollbackCachedEntityRestore_WrongUndoParameterType() {
        BaseType value = mock(BaseType.class);
        Context<BaseType> context = mock(Context.class);
        doReturn(Input.of(-1L)).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        var result = assertThrows(InvalidParameterTypeException.class, () -> cache.rollbackCachedEntity(context, facadeSave));

        assertThat(result).isNotNull().isInstanceOf(InvalidParameterTypeException.class);
        assertThat(result.getMessage()).isEqualTo("Parameter not a 'BaseType' value:[-1]");
        verify(cache).rollbackCachedEntity(context, facadeSave, null);
        verify(facadeSave, never()).apply(any(BaseType.class));
    }

    @Test
    void shouldNotRollbackCachedEntityRestore_SaveEntityThrows() {
        BaseType value = mock(BaseType.class);
        Context<BaseType> context = mock(Context.class);
        doReturn(Input.of(value)).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));
        doThrow(RuntimeException.class).when(facadeSave).apply(value);

        var result = assertThrows(RuntimeException.class, () -> cache.rollbackCachedEntity(context, facadeSave));

        assertThat(result).isNotNull().isInstanceOf(RuntimeException.class);
        verify(cache).rollbackCachedEntity(context, facadeSave, null);
        verify(facadeSave).apply(value);
    }

    @Test
    void shouldRollbackCachedEntityDelete() {
        long entityId = 3L;
        BaseType value = mock(BaseType.class);
        Context<BaseType> context = mock(Context.class);
        doReturn(Input.of(entityId)).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));
        LongConsumer facadeDeleteById = spy(new DeleteEntity());

        var result = cache.rollbackCachedEntity(context, facadeSave, facadeDeleteById);

        assertThat(result).isEmpty();
        verify(facadeDeleteById).accept(entityId);
        verify(context).setResult(null);
    }

    @Test
    void shouldRollbackCachedEntityDelete_ClearRedoParameter() {
        long entityId = 4L;
        BaseType value = mock(BaseType.class);
        BasePayload<?> payload = mock(BasePayload.class);
        Context<BaseType> context = mock(Context.class);
        doReturn(Input.of(entityId)).when(context).getUndoParameter();
        doReturn(Input.of(payload)).when(context).getRedoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));
        LongConsumer facadeDeleteById = spy(new DeleteEntity());

        var result = cache.rollbackCachedEntity(context, facadeSave, facadeDeleteById);

        assertThat(result).isEmpty();
        verify(facadeDeleteById).accept(entityId);
        verify(context).setResult(null);
        verify(payload).setId(null);
    }

    @Test
    void shouldNotRollbackCachedEntityDelete_WrongUndoParameterType() {
        BaseType value = mock(BaseType.class);
        Context<BaseType> context = mock(Context.class);
        doReturn(Input.of("entityId")).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));
        LongConsumer facadeDeleteById = spy(new DeleteEntity());

        var result = assertThrows(InvalidParameterTypeException.class,
                () -> cache.rollbackCachedEntity(context, facadeSave, facadeDeleteById)
        );

        assertThat(result).isNotNull().isInstanceOf(InvalidParameterTypeException.class);
        assertThat(result.getMessage()).isEqualTo("Parameter not a 'Long' value:[entityId]");
        verify(facadeDeleteById, never()).accept(anyLong());
        verify(context, never()).setResult(any());
    }

    @Test
    void shouldNotRollbackCachedEntityDelete_DeleteThrows() {
        long entityId = 5L;
        BaseType value = mock(BaseType.class);
        Context<BaseType> context = mock(Context.class);
        doReturn(Input.of(entityId)).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));
        LongConsumer facadeDeleteById = spy(new DeleteEntity());
        doThrow(RuntimeException.class).when(facadeDeleteById).accept(entityId);

        var result = assertThrows(RuntimeException.class,
                () -> cache.rollbackCachedEntity(context, facadeSave, facadeDeleteById)
        );

        assertThat(result).isNotNull().isInstanceOf(RuntimeException.class);
        verify(facadeDeleteById).accept(entityId);
        verify(context, never()).setResult(any());
    }

    @Test
    void shouldRestoreInitialCommandState() {
        String commandId = "command-id";
        RootCommand command = mock(RootCommand.class);
        doReturn(commandId).when(command).getId();
        Context<BaseType> context = mock(Context.class);
        doReturn(command).when(context).getCommand();
        BaseType value = mock(BaseType.class);
        doReturn(Input.of(value)).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        cache.restoreInitialCommandState(context, facadeSave);

        verify(cache).rollbackCachedEntity(context, facadeSave);
        verify(cache).rollbackCachedEntity(context, facadeSave, null);
    }

    @Test
    void shouldNotRestoreInitialCommandState_UndoParameterIsNull() {
        Context<BaseType> context = mock(Context.class);
        BaseType value = mock(BaseType.class);
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        cache.restoreInitialCommandState(context, facadeSave);

        verify(cache, never()).rollbackCachedEntity(any(Context.class), any(Function.class));
    }

    @Test
    void shouldNotRestoreInitialCommandState_UndoParameterIsEmpty() {
        Context<BaseType> context = mock(Context.class);
        BaseType value = mock(BaseType.class);
        doReturn(Input.empty()).when(context).getUndoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        cache.restoreInitialCommandState(context, facadeSave);

        verify(cache, never()).rollbackCachedEntity(any(Context.class), any(Function.class));
    }

    @Test
    void shouldPersistRedoEntity() {
        Context<BaseType> context = mock(Context.class);
        BaseType value = mock(BaseType.class);
        doReturn(Input.of(value)).when(context).getRedoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        var result = cache.persistRedoEntity(context, facadeSave);

        assertThat(result).isPresent().contains(value);
        verify(facadeSave).apply(value);
    }

    @Test
    void shouldNotPersistRedoEntity_WrongRedoParameterType() {
        Context<BaseType> context = mock(Context.class);
        BaseType value = mock(BaseType.class);
        doReturn(Input.of("value")).when(context).getRedoParameter();
        Function<BaseType, Optional<BaseType>> facadeSave = spy(new SaveEntity(value));

        var result = assertThrows(InvalidParameterTypeException.class, () -> cache.persistRedoEntity(context, facadeSave));

        assertThat(result).isNotNull().isInstanceOf(InvalidParameterTypeException.class);
        assertThat(result.getMessage()).isEqualTo("Parameter not a 'BaseType' value:[value]");
        verify(facadeSave, never()).apply(any(BaseType.class));
    }

    @Test
    void shouldDoAfterEntityPersistenceCheck_ContextFailed() {
        Context<BaseType> context = mock(Context.class);
        doReturn(true).when(context).isFailed();
        BaseType value = mock(BaseType.class);
        Runnable rollbackProcess = mock(Runnable.class);

        cache.afterEntityPersistenceCheck(context, rollbackProcess, value, true);

        verify(rollbackProcess).run();
        verify(context, never()).setResult(any());
    }

    @Test
    void shouldDoAfterEntityPersistenceCheck_ContextNotFailed() {
        Context<Optional<BaseType>> context = mock(Context.class);
        BaseType value = mock(BaseType.class);
        Runnable rollbackProcess = mock(Runnable.class);

        cache.afterEntityPersistenceCheck(context, rollbackProcess, value, true);

        verify(rollbackProcess, never()).run();
        verify(context).setResult(Optional.of(value));
    }

    @Test
    void shouldDoAfterEntityPersistenceCheck_ContextNotFailedSetUndoParameter() {
        long entityId = 6L;
        CommandContext<Optional<BaseType>> context = mock(CommandContext.class);
        BaseType value = mock(BaseType.class);
        doReturn(entityId).when(value).getId();
        Runnable rollbackProcess = mock(Runnable.class);

        cache.afterEntityPersistenceCheck(context, rollbackProcess, value, true);

        verify(rollbackProcess, never()).run();
        verify(context).setResult(Optional.of(value));
        verify(context).setUndoParameter(Input.of(entityId));
    }

    @Test
    void shouldPrepareDeleteEntityUndo() {
        CommandContext<Optional<BaseType>> context = mock(CommandContext.class);
        BasePayload<BaseType> value = mock(BasePayload.class);
        EntityNotFoundException exception = new EntityNotFoundException("message") {
        };
        Supplier<? extends EntityNotFoundException> exceptionSupplier = spy(new ErrorHandler(exception));

        cache.prepareDeleteEntityUndo(context, value, exceptionSupplier);

        verify(exceptionSupplier, never()).get();
        verify(value).setId(null);
        verify(context).setUndoParameter(Input.of(value));
    }

    @Test
    void shouldNotPrepareDeleteEntityUndo_WrongEntityType() {
        CommandContext<Optional<BaseType>> context = mock(CommandContext.class);
        BaseType value = mock(BaseType.class);
        EntityNotFoundException exception = new EntityNotFoundException("message") {
        };
        Supplier<? extends EntityNotFoundException> exceptionSupplier = spy(new ErrorHandler(exception));

        var result = assertThrows(EntityNotFoundException.class, () -> cache.prepareDeleteEntityUndo(context, value, exceptionSupplier));

        assertThat(result).isNotNull().isSameAs(exception);
        verify(exceptionSupplier).get();
    }

    // inner classes
    static class FindById implements LongFunction<Optional<BaseType>> {
        final BaseType value;

        FindById(BaseType value) {
            this.value = value;
        }

        @Override
        public Optional<BaseType> apply(final long inputId) {
            return Optional.ofNullable(value);
        }
    }

    static class AdoptEntity implements UnaryOperator<BaseType> {
        final BaseType result;

        AdoptEntity(BaseType result) {
            this.result = result;
        }

        @Override
        public BaseType apply(BaseType baseType) {
            return result;
        }
    }

    static class ErrorHandler implements Supplier<EntityNotFoundException> {
        final EntityNotFoundException exception;

        ErrorHandler(EntityNotFoundException exception) {
            this.exception = exception;
        }

        @Override
        public EntityNotFoundException get() {
            return exception;
        }
    }

    static class SaveEntity implements Function<BaseType, Optional<BaseType>> {
        final BaseType saved;

        SaveEntity(BaseType saved) {
            this.saved = saved;
        }

        @Override
        public Optional<BaseType> apply(BaseType baseType) {
            return Optional.ofNullable(saved);
        }
    }

    static class DeleteEntity implements LongConsumer {
        @Override
        public void accept(long value) {
            // just to test behavior
        }
    }
}