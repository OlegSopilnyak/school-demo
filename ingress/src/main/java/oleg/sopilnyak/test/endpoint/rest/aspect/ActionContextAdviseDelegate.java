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
import oleg.sopilnyak.test.endpoint.aspect.AdviseDelegate;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
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
public class ActionContextAdviseDelegate implements AdviseDelegate {
    @Override
    public String toString() {
        return "RestControllerAspect::AdviseDelegate for ActionContext Entity";
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
        // getting controller instance from the joint point
        if ((controller = getRestControllerInstance(joinPoint)) == null) return;

        final Signature signature = joinPoint.getSignature();
        log.debug("before call for {}", signature);
        try {
            // getting facade from the controller
            final BusinessFacade facade = retrieveFacadeFrom(controller);
            if (isNull(facade)) {
                log.error("No business facade found in controller {}", controller);
            } else {
                log.debug("BusinessFacade found: {}", facade);
                // setting up current ActionContext with facade name and action name
                ActionContext.setup(facade.getName(), signature.getName());
            }
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
        final ActionContext context;
        if ((context = ActionContext.current()) == null) {
            throw new AssertionError("current ActionContext entity is null");
        } else {
            // finishing context's work
            context.finish();
        }
        log.debug("after call for {}", context);
        ActionContext.release();
    }

    // private methods
    private static Object getRestControllerInstance(final JoinPoint joinPoint) {
        final Object target = joinPoint.getTarget();
        if (MockUtil.isSpy(target)) {
            log.warn("Target controller is spy.");
            // if target is spy, we can retrieve genuine instance from it
            // this is useful for Mockito spy, which is not a mock, but a real object
            return Mockito.mockingDetails(target).getMockCreationSettings().getSpiedInstance();
        } else if (MockUtil.isMock(target)) {
            log.warn("Target controller is mock.");
            // if target is mock, we cannot retrieve genuine instance from it
            return null;
        } else {
            log.debug("Returning genuine target controller instance: {}", target);
            // this is real controller instance, not mock or spy
            return target;
        }
    }

    private static BusinessFacade retrieveFacadeFrom(final Object controller) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        final Class<?> controllerClass = controller.getClass();
        // searching for field with BusinessFacade type
        log.debug("Searching for BusinessFacade field in the {}", controllerClass);
        final String facadeFiledName = Arrays.stream(controllerClass.getDeclaredFields())
                .filter(field -> BusinessFacade.class.isAssignableFrom(field.getType()))
                .map(Field::getName).findFirst().orElse(null);
        if (isEmpty(facadeFiledName)) {
            log.warn("No facade field found in the {}", controller);
            return null;
        }
        // searching for getter method for the facade field
        log.debug("Searching for getter method for the facade field '{}' in the {}", facadeFiledName, controllerClass);
        final Method facadeGetter = Arrays.stream(Introspector.getBeanInfo(controllerClass).getPropertyDescriptors())
                .filter(property -> facadeFiledName.equals(property.getName()))
                .map(PropertyDescriptor::getReadMethod).findFirst().orElse(null);
        if (facadeGetter == null) {
            log.warn("No facade field getter found in the {}", controller);
            return null;
        }
        // invoking getter method to retrieve facade instance
        return toFacade(facadeGetter.invoke(controller));
    }

    private static BusinessFacade toFacade(final Object facadeValue) {
        return facadeValue instanceof BusinessFacade facade ? facade : null;
    }
}
