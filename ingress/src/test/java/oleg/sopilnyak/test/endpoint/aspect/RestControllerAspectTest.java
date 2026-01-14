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
    AdviseDelegate adviseDelegate;
    @Mock
    JoinPoint jp;

    RestControllerAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = spy(new RestControllerAspect(Set.of(adviseDelegate)));
    }

    @Test
    void shouldCallControllerBeforeAdvise() {
        aspect.controllerBeforeAdvise(jp);

        verify(adviseDelegate).beforeCall(jp);
    }

    @Test
    void shouldCallControllerAfterAdvise() {
        aspect.controllerAfterAdvise(jp);

        verify(adviseDelegate).afterCall(jp);
    }
}