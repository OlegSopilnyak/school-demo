package oleg.sopilnyak.test.service.facade.aspect;

import org.aspectj.lang.JoinPoint;

/**
 * Delegate for aspect actions
 *
 * @see ActionFacadeAspect
 */
public interface AdviseDelegate {
    /**
     * To do action before Pointcut call
     *
     * @param joinPoint call's joint point
     * @see JoinPoint
     */
    default void beforeCall(JoinPoint joinPoint) {
    }

    /**
     * To do action after Pointcut call
     *
     * @param joinPoint call's joint point
     * @see JoinPoint
     */
    default void afterCall(JoinPoint joinPoint) {
    }
}
