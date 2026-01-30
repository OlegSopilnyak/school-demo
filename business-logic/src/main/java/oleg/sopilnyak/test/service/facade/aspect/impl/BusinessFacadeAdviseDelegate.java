package oleg.sopilnyak.test.service.facade.aspect.impl;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.facade.aspect.AdviseDelegate;

import org.aspectj.lang.JoinPoint;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Delegate for ActionContext actions
 *
 * @see oleg.sopilnyak.test.school.common.business.facade.ActionContext
 */
@Slf4j
@Component
public class BusinessFacadeAdviseDelegate implements AdviseDelegate {
    private static final String DO_ACTION_METHOD_NAME = "doActionAndResult";
    /**
     * To do action before Pointcut call
     *
     * @param joinPoint call's joint point
     * @see JoinPoint
     */
    @Override
    public void beforeCall(JoinPoint joinPoint) {
        final BusinessFacade facade = getFacadeReference(joinPoint);
        final String methodName = joinPoint.getSignature().getName();
        if (DO_ACTION_METHOD_NAME.equals(methodName)) {
            applyActionContext(joinPoint, facade);
        } else {
            log.warn("Unknown method name {}", methodName);
        }
    }

    // private method
    private void applyActionContext(JoinPoint joinPoint, BusinessFacade facade) {
        final Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.warn("Empty args array");
        } else {
            final String actionId = args[0].toString();
            facade.validActions().stream().filter(id -> id.equals(actionId)).findFirst()
                    .ifPresentOrElse(id -> log.debug("Valid action-id = '{}'", id),
                            () -> {
                                log.error("Invalid action-id '{}'", actionId);
                                throw new InvalidParameterTypeException("Valid BusinessFacade Action-ID", actionId);
                            }
                    );
            // store action-id to thread's action-context
            ActionContext.current().setActionId(actionId);
        }
    }

    private BusinessFacade getFacadeReference(final JoinPoint joinPoint) {
        final Object target = joinPoint.getTarget();
        if (MockUtil.isSpy(target)) {
            log.warn("Target facade is spy.");
            // if target is spy, we can retrieve genuine instance from it
            // this is useful for Mockito spy, which is not a mock, but a real object
            return (BusinessFacade) Mockito.mockingDetails(target).getMockCreationSettings().getSpiedInstance();
        } else if (MockUtil.isMock(target)) {
            log.warn("Target facade is mock.");
            // if target is mock, we cannot retrieve genuine instance from it
            return null;
        } else if (target instanceof BusinessFacade facade) {
            log.info("Detected BusinessFacade name = '{}'.", facade.getName());
            return facade;
        } else {
            return null;
        }
    }
}
