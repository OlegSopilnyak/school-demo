package oleg.sopilnyak.test.endpoint.aspect;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdviseDelegateTest {
    @Mock
    AdviseDelegate delegate;

    @Test
    void shouldCallBeforeAdvise() {
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Mockito.doCallRealMethod().when(delegate).beforeCall(joinPoint);

        delegate.beforeCall(joinPoint);

        Mockito.verify(delegate).beforeCall(joinPoint);
    }

    @Test
    void shouldCallAfterAdvise() {
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Mockito.doCallRealMethod().when(delegate).afterCall(joinPoint);

        delegate.afterCall(joinPoint);

        Mockito.verify(delegate).afterCall(joinPoint);
    }
}