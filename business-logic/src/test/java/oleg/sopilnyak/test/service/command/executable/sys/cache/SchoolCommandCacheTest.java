package oleg.sopilnyak.test.service.command.executable.sys.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.model.BaseType;

import java.util.Optional;
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
    void rollbackCachedEntity() {
    }

    @Test
    void testRollbackCachedEntity() {
    }

    @Test
    void restoreInitialCommandState() {
    }

    @Test
    void persistRedoEntity() {
    }

    @Test
    void afterEntityPersistenceCheck() {
    }

    @Test
    void prepareDeleteEntityUndo() {
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
}