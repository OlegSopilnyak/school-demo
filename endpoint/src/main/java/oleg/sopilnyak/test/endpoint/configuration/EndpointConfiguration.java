package oleg.sopilnyak.test.endpoint.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"oleg.sopilnyak.test.endpoint.rest"})
public class EndpointConfiguration  implements WebMvcConfigurer {
}
