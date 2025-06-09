package oleg.sopilnyak.test.endpoint.aspect;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestControllerAspectTest {
    @Mock
    AspectDelegate aspectDelegate;
    @Mock
    JoinPoint jp;

    RestControllerAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = spy(new RestControllerAspect(Set.of(aspectDelegate)));
    }

    @Test
    void shouldCallControllerBeforeAdvise() {
        aspect.controllerBeforeAdvise(jp);

        verify(aspectDelegate).beforeCall(jp);
    }

    @Test
    void shouldCallControllerAfterAdvise() {
        aspect.controllerAfterAdvise(jp);

        verify(aspectDelegate).afterCall(jp);
    }
}