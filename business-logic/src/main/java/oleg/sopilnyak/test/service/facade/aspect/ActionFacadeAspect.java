package oleg.sopilnyak.test.service.facade.aspect;

import java.util.Collection;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect: the aspect to intercept facade's methods call
 */
@Slf4j
@Aspect
public class ActionFacadeAspect {
    private final Collection<AdviseDelegate> delegates;

    public ActionFacadeAspect(Collection<AdviseDelegate> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Pointcut("execution (* oleg.sopilnyak.test.school.common.business.facade.BusinessFacade.doActionAndResult(..))")
    private void facadeDoActionCall() {
    }

    /**
     * Before facade do-action call advice to process delegates
     *
     * @param jp joint point before call
     * @see ActionFacadeAspect#before(AdviseDelegate, JoinPoint)
     * @see JoinPoint#getSignature()
     * @see JoinPoint#getArgs()
     * @see AdviseDelegate#beforeCall(JoinPoint)
     */
    @Before("facadeDoActionCall()")
    public void facadeBeforeAdvise(final JoinPoint jp) {
        for(final AdviseDelegate delegate : delegates) {
            before(delegate, jp);
        }
    }

    /**
     * After facade do-action call advice to process delegates
     *
     * @param jp joint point before call
     * @see ActionFacadeAspect#after(AdviseDelegate, JoinPoint)
     * @see JoinPoint#getSignature()
     * @see JoinPoint#getArgs()
     * @see AdviseDelegate#afterCall(JoinPoint)
     */
    @After("facadeDoActionCall()")
    public void facadeAfterAdvise(final JoinPoint jp) {
        for(final AdviseDelegate delegate : delegates) {
            after(delegate, jp);
        }
    }

    // private methods
    private static void before(final AdviseDelegate delegate, final JoinPoint jp) {
        final Signature signature = jp.getSignature();
        log.debug("Calling before {} for '{}'", delegate, signature);
        try {
            delegate.beforeCall(jp);
            log.info("before for delegate '{}' called...", delegate);
        } catch (Throwable e) {
            log.error("Cannot execute before for '{}'", signature, e);
        }
    }

    private static void after(final AdviseDelegate delegate, final JoinPoint jp) {
        log.debug("Calling after {} for '{}'", delegate, jp.getSignature());
        try {
            delegate.afterCall(jp);
            log.info("after for delegate '{}' called...", delegate);
        } catch (Throwable e) {
            log.error("Cannot execute after for '{}'", jp.getSignature(), e);
        }
    }
}
