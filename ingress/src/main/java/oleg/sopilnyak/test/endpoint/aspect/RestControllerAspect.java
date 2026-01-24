package oleg.sopilnyak.test.endpoint.aspect;

import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Aspect: the aspect to intercept rest calls for do something before and after http rest call
 */
@Slf4j
@Aspect
public class RestControllerAspect {
    private final Collection<AdviseDelegate> delegates;

    public RestControllerAspect(Collection<AdviseDelegate> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Pointcut("within (@org.springframework.web.bind.annotation.RestController *)")
    private void controllerMethodCalls() {
    }

    @Pointcut("within (oleg.sopilnyak.test.endpoint.rest..*)")
    private void moduleRestCalls() {
    }

    /**
     * Before controller call advice to process delegates
     *
     * @param jp joint point before call
     * @see JoinPoint#getSignature()
     * @see JoinPoint#getArgs()
     * @see AdviseDelegate#beforeCall(JoinPoint)
     */
    @Before("controllerMethodCalls() && moduleRestCalls()")
    public void controllerBeforeAdvise(final JoinPoint jp) {
        for(final AdviseDelegate delegate : delegates) {
            before(delegate, jp);
        }
    }

    /**
     * After controller call advice to process delegates
     *
     * @param jp joint point before call
     * @see JoinPoint#getSignature()
     * @see JoinPoint#getArgs()
     * @see AdviseDelegate#afterCall(JoinPoint) (JoinPoint)
     */
    @After("controllerMethodCalls() && moduleRestCalls()")
    public void controllerAfterAdvise(final JoinPoint jp) {
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
