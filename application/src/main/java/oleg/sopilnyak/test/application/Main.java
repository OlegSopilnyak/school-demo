package oleg.sopilnyak.test.application;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.configuration.EndpointConfiguration;
import oleg.sopilnyak.test.persistence.configuration.MySqlDataSourceConfiguration;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Slf4j
@SpringBootApplication
@Import({EndpointConfiguration.class, BusinessLogicConfiguration.class, PersistenceConfiguration.class, MySqlDataSourceConfiguration.class})
public class Main {
    public static void main(String[] parameters) {
        log.info("Running school-application...");
        SpringApplication application = new SpringApplication(Main.class);
        application.run(parameters);
    }

    @Bean
    public CommandLineRunner commandLineRunner(PersistenceFacade persistenceFacade) {
        return args -> {
            log.info("Creating default database for application...");
            persistenceFacade.initDefaultDataset();
        };
    }
}
