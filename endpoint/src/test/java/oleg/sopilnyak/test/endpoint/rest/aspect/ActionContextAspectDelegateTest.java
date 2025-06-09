package oleg.sopilnyak.test.endpoint.rest.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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
class ActionContextAspectDelegateTest {
    @Mock
    JoinPoint jp;

    @InjectMocks
    ActionContextAspectDelegate delegate;

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

        delegate.beforeCall(jp);

        ActionContext context = ActionContext.current();
        assertThat(context).isNotNull();
        assertThat(context.getActionName()).isEqualTo(controllerMethodName);
        assertThat(context.getFacadeName()).isEqualTo(facade.getName());
    }

    @Test
    void shouldDoActionAfterCall() {
        assertThat(ActionContext.current()).isNull();
        String facadeName = "facade-name";
        String controllerMethodName = "controller-method-name";
        ActionContext.setup(facadeName, controllerMethodName);
        ActionContext context = ActionContext.current();
        assertThat(context).isNotNull();
        assertThat(context.getActionName()).isEqualTo(controllerMethodName);
        assertThat(context.getFacadeName()).isEqualTo(facadeName);

        delegate.afterCall(jp);

        assertThat(ActionContext.current()).isNull();
    }
}