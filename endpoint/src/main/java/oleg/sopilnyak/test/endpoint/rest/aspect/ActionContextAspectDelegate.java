package oleg.sopilnyak.test.endpoint.rest.aspect;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.aspect.AspectDelegate;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import org.aspectj.lang.JoinPoint;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.stereotype.Component;


/**
 * Delegate for ActionContext actions
 *
 * @see oleg.sopilnyak.test.school.common.business.facade.ActionContext
 */
@Slf4j
@Component
public class ActionContextAspectDelegate implements AspectDelegate {
    @Override
    public String toString() {
        return "RestControllerAspect::AspectDelegate for ActionContext";
    }

    /**
     * To do action before Pointcut call
     *
     * @param joinPoint call's joint point
     * @see JoinPoint
     */
    @Override
    public void beforeCall(JoinPoint joinPoint) {
        final Object controller;
        if (MockUtil.isSpy(joinPoint.getTarget())) {
            log.debug("Target controller is spy.");
            final var mockCreationSettings = Mockito.mockingDetails(joinPoint.getTarget()).getMockCreationSettings();
            controller = mockCreationSettings.getSpiedInstance();
        } else if (MockUtil.isMock(joinPoint.getTarget())) {
            log.debug("Target controller is mock.");
            return;
        } else {
            controller = joinPoint.getTarget();
        }

        // getting facade from the controller
        try {
            log.debug("before call for {}", joinPoint.getSignature());
            final var facade = retrieveFacadeFrom(controller);
            if (isNull(facade)) {
                log.error("No facade found in {}", controller);
                return;
            }
            final String actionName = joinPoint.getSignature().getName();
            // setup action context for the rest call
            ActionContext.setup(facade.getName(), actionName);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            log.error("Error while trying to get facade from {}", controller, e);
        }
    }

    /**
     * To do action after Pointcut call
     *
     * @param joinPoint call's joint point
     * @see JoinPoint
     */
    @Override
    public void afterCall(JoinPoint joinPoint) {
        final ActionContext context = ActionContext.current();
        if (context == null) {
            throw new AssertionError("ActionContext is null");
        }
        context.finish();
        log.info("after call for {}", context);
        ActionContext.release();
    }

    // private methods
    private static BusinessFacade retrieveFacadeFrom(final Object controller) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        final Class<?> controllerClass = controller.getClass();
        final String facadeFiledName = Arrays.stream(controllerClass.getDeclaredFields())
                .filter(field -> BusinessFacade.class.isAssignableFrom(field.getType()))
                .map(Field::getName).findFirst().orElse(null);
        if (isEmpty(facadeFiledName)) {
            log.warn("No facade field found in the {}", controller);
            return null;
        }
        final Method facadeGetter = Arrays.stream(Introspector.getBeanInfo(controllerClass).getPropertyDescriptors())
                .filter(property -> facadeFiledName.equals(property.getName()))
                .map(PropertyDescriptor::getReadMethod).findFirst().orElse(null);
        if (facadeGetter == null) {
            log.warn("No facade field getter found in the {}", controller);
            return null;
        }
        return toFacade(facadeGetter.invoke(controller));
    }

    private static BusinessFacade toFacade(Object facadeValue) {
        return facadeValue instanceof BusinessFacade facade ? facade : null;
    }
}
