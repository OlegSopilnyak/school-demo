package oleg.sopilnyak.test.endpoint.configuration;

import java.util.Collection;
import oleg.sopilnyak.test.endpoint.aspect.AspectDelegate;
import oleg.sopilnyak.test.endpoint.aspect.RestControllerAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * SpringConfiguration: Configuration for aspects
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
//@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"oleg.sopilnyak.test.endpoint.rest.aspect"})
public class AspectForRestConfiguration {
    @Bean
    public RestControllerAspect makeControllerAspect(final Collection<AspectDelegate> delegates) {
        return new RestControllerAspect(delegates);
    }
}
