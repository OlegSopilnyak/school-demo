package oleg.sopilnyak.test.endpoint.rest.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.endpoint.rest.education.StudentsRestController;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionContextAdviseDelegateTest {
    @Mock
    JoinPoint jp;

    @InjectMocks
    ActionContextAdviseDelegate delegate;

    @Test
    void shouldDoActionBeforeCall() {
        String controllerMethodName = "controller-method-name";
        Signature signature = mock(Signature.class);
        doReturn(controllerMethodName).when(signature).getName();
        doReturn(signature).when(jp).getSignature();
        StudentsFacade facade = mock(StudentsFacade.class);
        doCallRealMethod().when(facade).getName();
        StudentsRestController controller = spy(new StudentsRestController(facade));
        doReturn(controller).when(jp).getTarget();
        assertThat(ActionContext.current()).isNull();

        delegate.beforeCall(jp);

        ActionContext context = ActionContext.current();
        assertThat(context).isNotNull();
        assertThat(context.getActionName()).isEqualTo(controllerMethodName);
        assertThat(context.getFacadeName()).isEqualTo(facade.getName());
        assertThat(context.getStartedAt()).isNotNull();
        assertThat(context.getLasts()).isNull();
    }

    @Test
    void shouldDoActionAfterCall() {
        assertThat(ActionContext.current()).isNull();
        String facadeName = "facade-name";
        String controllerMethodName = "controller-method-name";
        ActionContext context = spy(ActionContext.setup(facadeName, controllerMethodName));
        assertThat(ActionContext.current()).isNotNull();
        assertThat(context.getActionName()).isEqualTo(controllerMethodName);
        assertThat(context.getFacadeName()).isEqualTo(facadeName);
        ActionContext.install(context , true);

        delegate.afterCall(jp);

        verify(context).finish();
        assertThat(ActionContext.current()).isNull();
    }
}