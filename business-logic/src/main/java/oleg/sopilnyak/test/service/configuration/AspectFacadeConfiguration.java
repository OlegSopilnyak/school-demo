package oleg.sopilnyak.test.service.configuration;

import oleg.sopilnyak.test.service.facade.aspect.ActionFacadeAspect;
import oleg.sopilnyak.test.service.facade.aspect.AdviseDelegate;

import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * SpringConfiguration: Configuration for aspects
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = {"oleg.sopilnyak.test.service.facade"})
public class AspectFacadeConfiguration {
    @Bean
    public ActionFacadeAspect makeFacadeAspect(final Collection<AdviseDelegate> delegates) {
        return new ActionFacadeAspect(delegates);
    }
}
